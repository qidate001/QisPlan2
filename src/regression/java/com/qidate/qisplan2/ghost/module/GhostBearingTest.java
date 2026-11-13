package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.bearing.GhostBearing;
import com.qidate.qisplan2.ghost.bearing.GhostModuleForm;
import com.qidate.qisplan2.ghost.bearing.HostComposition;
import com.qidate.qisplan2.ghost.bearing.SpiritualPart;
import com.qidate.qisplan2.ghost.corrosion.CorrosionMatrix;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleContainer;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.SupernaturalAttackEvent;
import com.qidate.qisplan2.ghost.module.implementation.GhostShroudModule;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** 覆盖真实容器、部位分配与执行资格，避免只检查类名或配置值。 */
public final class GhostBearingTest {
    /** 测试入口无需实例。 */
    private GhostBearingTest() {}

    /** 校验失败时输出对应承载约定。 */
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    /** 注册具有不同形式的测试厉鬼，替换式之间使用不同模块 ID。 */
    private static ResourceLocation module(String path, GhostModuleForm form) {
        var id = ResourceLocation.fromNamespaceAndPath("bearing_test", path);
        GhostModuleRegistry.register(new GhostModule() {
            /** 返回测试身份。 */
            public ResourceLocation getId() { return id; }
            /** 返回声明形式，不引用宿主类型。 */
            public GhostModuleForm form() { return form; }
        });
        return id;
    }

    /** 测试宿主只提供组成及模块列表，不模拟实体、物品或玩家。 */
    private record Host(HostComposition composition, List<GhostModuleData.Entry> getModules)
            implements GhostModuleHost {}

    /** 验证所有支持的安装、存档、扩展与失败路径。 */
    public static void run() {
        var skin = GhostShroudModule.ID;
        var otherSkin = module("other_skin", GhostModuleForm.replacing(SpiritualPart.SKIN));
        var heart = module("heart", GhostModuleForm.replacing(SpiritualPart.HEART));
        var free = module("independent", GhostModuleForm.independent());
        var engine = new SpiritualPart(ResourceLocation.fromNamespaceAndPath("bearing_test", "engine"));
        var engineGhost = module("engine", GhostModuleForm.replacing(engine));
        check(HostComposition.HUMANOID.capacities().size() == 21
                && !HostComposition.HUMANOID.capacities().containsKey(SpiritualPart.GLOBAL), "人形必须只有21部位");
        check(new SpiritualPart(SpiritualPart.SKIN.id()).equals(SpiritualPart.SKIN), "动态重建的部位必须按标识相等");

        var humanoid = new GhostModuleContainer(() -> HostComposition.HUMANOID);
        check(humanoid.addModule(skin, 30), "人形可安装皮肤替换");
        check(humanoid.addModule(heart, 10), "不同部位可同时替换");
        check(humanoid.addModule(free, 10), "独立式不占位");
        check(!humanoid.addModule(otherSkin, 10), "同一部位不能安装第二个鬼");
        check(!humanoid.addModule(engineGhost, 10), "没有对应组成不能安装");
        humanoid.setDeadlocked(skin, true);
        check(!humanoid.addModule(otherSkin, 10), "死机不能释放承载部位");
        humanoid.setSleeping(skin, true);
        check(!humanoid.addModule(otherSkin, 10), "沉睡不能释放承载部位");
        check(humanoid.addModule(skin, 40) && humanoid.getModules().size() == 3, "更新强度不能重复占位");
        var before = humanoid.getModules();
        check(!humanoid.replaceEntries(List.of(new GhostModuleData.Entry(skin, 30),
                new GhostModuleData.Entry(otherSkin, 30))) && humanoid.getModules().equals(before),
                "批量导入冲突必须原子失败并保留旧数据");
        humanoid.removeModule(skin);
        check(humanoid.addModule(otherSkin, 30), "移除后必须释放部位");

        var item = new GhostModuleContainer();
        check(item.addModule(skin, 30) && item.addModule(free, 1), "万能部位兼容皮肤且独立式不占位");
        check(!item.addModule(heart, 10), "不同替换目标在万能部位上仍然互斥");
        var allocation = GhostBearing.allocate(HostComposition.UNIVERSAL, item.getModules());
        check(allocation.occupants().get(SpiritualPart.UNIVERSAL).equals(List.of(skin)), "万能部位占位应明确可查询");
        item.removeModule(skin);
        check(item.addModule(engineGhost, 5), "万能部位应兼容自定义目标");

        var machine = new HostComposition(Map.of(engine, 1));
        var custom = new GhostModuleContainer(() -> machine);
        check(custom.addModule(engineGhost, 1) && !custom.addModule(skin, 1), "自定义宿主只接受自身声明的部位");
        var expandable = new HostComposition(Map.of(SpiritualPart.SKIN, 2));
        var future = new GhostModuleContainer(() -> expandable);
        check(future.addModule(skin, 1) && future.addModule(otherSkin, 1), "容量字段必须支持未来多占位扩展");
        var noParts = new GhostModuleContainer(() -> new HostComposition(Map.of()));
        check(noParts.addModule(free, 1) && !noParts.addModule(skin, 1), "独立式必须能存在于没有部位的宿主");

        // 旧数据冲突不丢弃实例，但只有先分配到部位的模块可以运行。
        var saved = new CompoundTag();
        saved.putInt("erosionTicks", 123);
        var legacy = new GhostModuleContainer(() -> HostComposition.HUMANOID);
        legacy.loadEntries(List.of(new GhostModuleData.Entry(otherSkin, 1),
                new GhostModuleData.Entry(skin, 30, 0, false, false, saved)));
        check(legacy.getModules().size() == 2 && legacy.getCustomData(skin).getInt("erosionTicks") == 123,
                "读档冲突不能丢弃模块独有数据");
        legacy.setIntensity(otherSkin, 2);
        var host = new Host(HostComposition.HUMANOID, legacy.getModules());
        check(host.carriesModule(otherSkin) && !host.carriesModule(skin), "普通更新不能改变占位顺序");
        var context = new GhostAbilityContext(host, skin, GhostShroudModule.PROTECT,
                GhostAbilityContext.Source.POSSESSION, new SupernaturalAttackEvent(UUID.randomUUID(), 1));
        check(!context.isModuleAvailable(), "主动调用也不能绕过承载冲突");
        legacy.removeModule(otherSkin);
        check(new Host(HostComposition.HUMANOID, legacy.getModules()).carriesModule(skin),
                "解除冲突后保留实例应重新获得承载");

        var roundTrip = new GhostModuleContainer(() -> HostComposition.HUMANOID);
        roundTrip.loadEntries(legacy.getModules());
        check(GhostBearing.allocate(HostComposition.HUMANOID, roundTrip.getModules())
                .occupants().get(SpiritualPart.SKIN).equals(List.of(skin)), "读档后应重建相同占位");

        var corrosion = GhostCorrosion.builder().add(engine, 60).build();
        var matrix = new CorrosionMatrix(machine);
        matrix.add(engine, engineGhost, engine, corrosion.get(engine));
        check(matrix.total(engine) == 60 && matrix.total(SpiritualPart.HEART) == 0, "侵蚀必须支持非枚举自定义部位");
        boolean rejectedGlobal = false;
        try { new HostComposition(Map.of(SpiritualPart.GLOBAL, 1)); }
        catch (IllegalArgumentException expected) { rejectedGlobal = true; }
        check(rejectedGlobal, "全局侵蚀不能声明为承载部位");
        System.out.println("Ghost bearing framework regression checks passed");
    }
}
