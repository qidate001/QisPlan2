package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.environment.*;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.implementation.GhostDoorModule;
import com.qidate.qisplan2.ghost.module.implementation.GhostDoorPlateModule;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;

/** 用没有任何 Minecraft 载体身份的宿主执行真实模块，检查计时、媒介与主动调用。 */
public final class GhostDoorModuleTest {
    private static final class Doors implements GhostDoorAccess {
        BlockPos front = BlockPos.ZERO;
        boolean opened = true;
        int closed;
        /** 模拟适配器报告的正面门。 */
        public BlockPos frontDoor() { return front; }
        /** 模拟实时开门状态。 */
        public boolean open(BlockPos pos) { return opened && pos != null; }
        /** 记录一次实际关门，禁止对已关门重复结算。 */
        public boolean close(BlockPos pos) { if (!open(pos)) return false; opened = false; closed++; return true; }
    }
    private static final class Host implements GhostModuleHost, GhostModuleCustomDataModifier {
        final ResourceLocation id;
        final Doors doors = new Doors();
        CompoundTag data = new CompoundTag();
        long time = 1000;
        boolean deadlocked;
        /** 两种模块均复用同一个不含实体或方块身份的测试宿主。 */
        Host(ResourceLocation id) { this.id = id; }
        /** 真实目录与运行时通过标准模块条目访问状态。 */
        public List<GhostModuleData.Entry> getModules() { return List.of(new GhostModuleData.Entry(id,20,0,false,deadlocked,data)); }
        /** 读取副本，确保修改必须经过统一写回。 */
        public CompoundTag getModuleCustomData(ResourceLocation ignored) { return data.copy(); }
        /** 替换状态，不把已删除字段合并回去。 */
        public boolean setModuleCustomData(ResourceLocation ignored, CompoundTag value) { data = value.copy(); return true; }
        /** 世界端口只包含测试需要的事实与操作。 */
        public GhostWorldAccess moduleWorld(ResourceLocation ignored) {
            return (GhostWorldAccess) Proxy.newProxyInstance(GhostWorldAccess.class.getClassLoader(),
                    new Class<?>[]{GhostWorldAccess.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "gameTime" -> time;
                        case "present" -> true;
                        case "doors" -> doors;
                        default -> {
                            if (method.isDefault()) yield InvocationHandler.invokeDefault(proxy,method,args);
                            throw new AssertionError("Unexpected world access: " + method.getName());
                        }
                    });
        }
    }
    /** 统一断言。 */
    private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
    /** 从主动入口执行能力，同时由真实执行器校验媒介与声明。 */
    private static boolean invoke(Host host, ResourceLocation ability, Integer number) {
        return GhostAbilityExecutor.tryExecuteWithResult(new GhostAbilityContext(host, host.id, ability,
                GhostAbilityContext.Source.POSSESSION, null, () -> {}, new GhostInvocationParameters(null, number)));
    }
    /** 执行周期规则、持久化回读、死机与非法主动输入等边界。 */
    public static void run() {
        if (!GhostModuleRegistry.contains(GhostDoorModule.ID)) GhostModuleRegistry.register(new GhostDoorModule());
        if (!GhostModuleRegistry.contains(GhostDoorPlateModule.ID)) GhostModuleRegistry.register(new GhostDoorPlateModule());
        var host = new Host(GhostDoorModule.ID);
        GhostModuleRuntime.dispatch(host, new ModuleTickEvent());
        long deadline = host.data.getLong("closeAt");
        check(deadline >= 1300 && deadline <= 1800, "自动关闭应在 15～40 秒之间");
        var encoded = GhostModuleData.CODEC.encodeStart(NbtOps.INSTANCE,host.getModules()).getOrThrow();
        host.data = GhostModuleData.CODEC.parse(NbtOps.INSTANCE,encoded).getOrThrow().getFirst().customData();
        host.time = deadline-1; GhostModuleRuntime.dispatch(host,new ModuleTickEvent());
        check(host.doors.closed == 0 && host.data.getLong("closeAt") == deadline, "存档读取不能重抽计时或提前关门");
        host.deadlocked = true; host.time = deadline; GhostModuleRuntime.dispatch(host,new ModuleTickEvent());
        check(host.doors.closed == 0, "死机后自动关门规则必须停止");
        check(invoke(host,GhostDoorModule.CLOSE,null) && host.doors.closed == 1, "死机不应禁止满足媒介的主动关门");
        check(!invoke(host,GhostDoorModule.CLOSE,null), "已关闭的门不能重复触发袭击");
        host.doors.opened = true; host.doors.front = null;
        check(!invoke(host,GhostDoorModule.CLOSE,null), "离开门前必须拒绝主动能力");
        host.deadlocked = false; GhostModuleRuntime.dispatch(host,new ModuleTickEvent());
        check(!host.data.contains("closeAt"), "离开门前后旧计时应失效");
        host.doors.front = BlockPos.ZERO; GhostModuleRuntime.dispatch(host,new ModuleTickEvent());
        host.time = host.data.getLong("closeAt"); GhostModuleRuntime.dispatch(host,new ModuleTickEvent());
        check(host.doors.closed == 2, "规则到期必须调用相同的关门能力");
        var plate = new Host(GhostDoorPlateModule.ID); plate.deadlocked = true;
        check(invoke(plate,GhostDoorPlateModule.NUMBER,123), "无门及死机状态下仍可修改号码");
        check(plate.data.getInt("number") == 123, "号码必须写回模块 customData");
        check(!invoke(plate,GhostDoorPlateModule.NUMBER,-1) && !invoke(plate,GhostDoorPlateModule.NUMBER,1000000000)
                && !invoke(plate,GhostDoorPlateModule.NUMBER,null), "负数、越界、缺少输入必须拒绝");
        check(plate.data.getInt("number") == 123, "非法输入不得修改原号码");
        check(!invoke(plate,GhostDoorPlateModule.TRAVEL,null), "未站在门中或缺少目标不得传送");
        check(GhostAbilityCatalog.all(plate).size() == 2, "门牌必须独立公开传送和改号");
        check(DoorTransitOutcome.decide(0,false) == DoorTransitOutcome.NO_DESTINATION, "无配对不进入虚空");
        check(DoorTransitOutcome.decide(1,true) == DoorTransitOutcome.TELEPORT, "唯一且开启的目的门正常传送");
        check(DoorTransitOutcome.decide(1,false) == DoorTransitOutcome.VOID, "唯一但关闭的目的门进入虚空");
        check(DoorTransitOutcome.decide(2,true) == DoorTransitOutcome.SPLIT
                && DoorTransitOutcome.decide(3,false) == DoorTransitOutcome.SPLIT, "一对多不能任选出口，优先撕裂");
    }
}
