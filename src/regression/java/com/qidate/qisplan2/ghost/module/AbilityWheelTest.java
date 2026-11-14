package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.implementation.calling.CallingGhostModule;
import com.qidate.qisplan2.ghost.module.implementation.candle.WhiteGhostCandleModule;
import com.qidate.qisplan2.ghost.module.implementation.candle.RedGhostCandleModule;
import com.qidate.qisplan2.ghost.module.implementation.InvisibleGhostModule;
import com.qidate.qisplan2.ghost.module.implementation.GhostPianoModule;
import com.qidate.qisplan2.ghost.module.implementation.GhostTombstoneModule;
import com.qidate.qisplan2.ghost.module.implementation.GhostStoneBricksModule;
import com.qidate.qisplan2.ghost.module.implementation.GhostShroudModule;
import com.qidate.qisplan2.ghost.module.implementation.GhostCarpetModule;
import com.qidate.qisplan2.ghost.module.implementation.GhostGrassModule;
import com.qidate.qisplan2.ghost.module.implementation.UntouchableGhostModule;
import com.qidate.qisplan2.client.screen.AbilityWheelLayout;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.environment.GhostWorldAccess;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonParser;
import com.qidate.qisplan2.ghost.module.implementation.OpeningGhostModule;
import com.qidate.qisplan2.ghost.module.implementation.ClosingGhostModule;
import com.qidate.qisplan2.ghost.module.implementation.eye.GhostEyeModule;
import com.qidate.qisplan2.ghost.module.implementation.mist.GhostMistModule;
import com.qidate.qisplan2.ghost.module.implementation.knocking.KnockingGhostModule;
import com.qidate.qisplan2.ghost.module.implementation.nightwanderer.NightWandererModule;
import com.qidate.qisplan2.ghost.module.implementation.divinationslip.GhostDivinationSlipModule;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** 无需客户端窗口：验证轮盘命中、目录过滤及真正通过世界端口执行的主动媒介。 */
public final class AbilityWheelTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("qisplan2_test", "wheel");
    private static boolean allowed;
    private static int effects;
    private static class Host implements GhostModuleHost {
        /** 故意使用死机模块，确保主动渠道仍可使用。 */
        public List<GhostModuleData.Entry> getModules() {
            return List.of(new GhostModuleData.Entry(ID, 30, 0, true, false, new CompoundTag()));
        }
        /** 不提供任何玩家类型，证明主动能力定义只依赖抽象世界端口。 */
        public GhostWorldAccess moduleWorld(ResourceLocation id) {
            return (GhostWorldAccess)Proxy.newProxyInstance(GhostWorldAccess.class.getClassLoader(),
                    new Class<?>[]{GhostWorldAccess.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "canToggleDomain" -> allowed;
                        case "toggleDomain" -> { effects++; yield true; }
                        default -> throw new AssertionError("Unexpected world operation: " + method.getName());
                    });
        }
    }
    /** 执行一个断言并提供可定位的失败原因。 */
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    /** 覆盖一到十个槽位的方向、取消区和十项截取，以及执行期媒介。 */
    public static void run() {
        verifyDefinitions();
        for (int count = 1; count <= 10; count++) {
            for (int index = 0; index < count; index++) {
                double angle = -Math.PI / 2 + index * Math.PI * 2 / count;
                check(AbilityWheelLayout.index(Math.cos(angle)*80, Math.sin(angle)*80, 40, 120, count) == index,
                        "每个扇区中心应命中对应槽位");
            }
        }
        check(AbilityWheelLayout.index(0, 0, 40, 120, 10) == -1, "中心必须取消");
        check(AbilityWheelLayout.index(121, 0, 40, 120, 10) == -1, "圆环外必须取消");
        check(AbilityWheelLayout.index(80, 0, 40, 120, 0) == -1, "空目录不能命中");
        GhostModuleRegistry.register(new GhostModule() {
            /** 测试模块使用单独命名空间，不污染正式模块。 */
            public ResourceLocation getId() { return ID; }
            /** 任意模块直接声明十二个技能，框架不需要认识其 ID。 */
            public List<GhostAbility> getIntrinsicAbilities() {
                var list = new ArrayList<GhostAbility>();
                for (int i = 0; i < 12; i++) list.add(new DefinedGhostAbility(
                        ResourceLocation.fromNamespaceAndPath(ID.getNamespace(), "custom_" + i),
                        ResourceLocation.parse("minecraft:textures/item/ender_eye.png"),
                        c -> c.getModuleContext().world().canToggleDomain(),
                        c -> c.getModuleContext().world().toggleDomain()));
                return list;
            }
        });
        var host = new Host();
        check(GhostAbilityCatalog.all(host).size() == 12, "列出全部模块声明能力且死机不隐藏技能");
        check(GhostAbilityCatalog.wheel(host).size() == 10, "轮盘只显示前十项");
        var first = GhostAbilityCatalog.wheel(host).getFirst();
        var call = new GhostAbilityContext(host, ID, first.ability().getId(), GhostAbilityContext.Source.POSSESSION, null);
        allowed = false;
        check(!GhostAbilityExecutor.tryExecuteWithResult(call) && effects == 0, "媒介不符不能产生效果");
        allowed = true;
        check(GhostAbilityExecutor.tryExecuteWithResult(call) && effects == 1, "死机仍可通过媒介主动释放");
        var deferred = GhostAbilityExecutor.prepareDeferred(call);
        check(deferred != null, "媒介满足时应可准备延迟能力");
        allowed = false;
        deferred.run();
        check(effects == 1, "实际执行前必须重新校验媒介");
        System.out.println("Ability catalog, media and wheel geometry regression checks passed");
    }
    /** 检查正式模块能力 ID 唯一、主动声明有效，中英文语言资源完整。 */
    private static void verifyDefinitions() {
        try (var zhStream = AbilityWheelTest.class.getResourceAsStream("/assets/qisplan2/lang/zh_cn.json");
             var enStream = AbilityWheelTest.class.getResourceAsStream("/assets/qisplan2/lang/en_us.json")) {
            check(zhStream != null && enStream != null, "语言资源必须被打包");
            var zh = JsonParser.parseReader(new InputStreamReader(zhStream, StandardCharsets.UTF_8)).getAsJsonObject();
            var en = JsonParser.parseReader(new InputStreamReader(enStream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (GhostModule module : List.of(new GhostEyeModule(), new GhostMistModule(), new OpeningGhostModule(),
                    new ClosingGhostModule(), new KnockingGhostModule(), new NightWandererModule(), new GhostDivinationSlipModule(),
                    new UntouchableGhostModule(), new GhostGrassModule(), new GhostCarpetModule(), new GhostShroudModule(), new GhostStoneBricksModule(), new GhostTombstoneModule(), new GhostPianoModule(), new InvisibleGhostModule(), new RedGhostCandleModule(), new WhiteGhostCandleModule(), new CallingGhostModule())) {
                var ids = new HashSet<ResourceLocation>();
                for (var ability : module.getAbilities()) {
                    check(ids.add(ability.getId()), "模块内部能力 ID 重复: " + ability.getId());
                    check(zh.has(ability.translationKey()) && en.has(ability.translationKey()), "缺少技能译名: " + ability.getId());
                    check(ability.iconTexture() != null, "能力缺少图标: " + ability.getId());
                    if (ability.iconTexture().getNamespace().equals("qisplan2"))
                        check(AbilityWheelTest.class.getResource("/assets/qisplan2/" + ability.iconTexture().getPath()) != null,
                                "能力图标不存在: " + ability.iconTexture());
                }
            }
        } catch (IOException error) { throw new AssertionError("无法读取语言文件", error); }
    }

}
