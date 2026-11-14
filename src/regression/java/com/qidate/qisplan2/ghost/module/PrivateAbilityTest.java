package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.implementation.*;
import com.qidate.qisplan2.ghost.module.implementation.calling.CallingGhostModule;
import com.qidate.qisplan2.ghost.module.implementation.knocking.KnockingGhostModule;
import com.qidate.qisplan2.ghost.module.implementation.mist.GhostMistModule;
import com.qidate.qisplan2.ghost.module.implementation.candle.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import com.qidate.qisplan2.ghost.module.environment.GhostWorldAccess;
import com.qidate.qisplan2.ghost.module.environment.GhostSight;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
import java.util.stream.Collectors;

/** 私有能力隔离、公开操作定义及持续状态的回归检查。 */
public final class PrivateAbilityTest {
    private static int effects;
    /** 断言失败提供明确的行为说明。 */
    private static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
    /** 获取模块对外公开的能力路径。 */
    private static Set<String> skills(GhostModule module) {
        return module.getAbilities().stream().filter(GhostAbility::isPublicSkill)
                .map(a -> a.getId().getPath()).collect(Collectors.toSet());
    }
    /** 同时检查 UI 过滤和执行器拒绝，避免只隐藏图标却仍可伪造请求。 */
    public static void run() {
        var id = ResourceLocation.parse("qisplan2_test:private");
        GhostModuleRegistry.register(new GhostModule() {
            public ResourceLocation getId() { return id; }
            public List<GhostAbility> getIntrinsicAbilities() { return List.of(new GhostAbility() {
                public ResourceLocation getId() { return id; }
                public boolean isPublicSkill() { return false; }
                public boolean canExecute(GhostAbilityContext c) { return true; }
                public void execute(GhostAbilityContext c) { effects++; }
            }); }
        });
        GhostModuleHost host = () -> List.of(new GhostModuleData.Entry(id, 10, 0));
        var direct = new GhostAbilityContext(host, id, id, GhostAbilityContext.Source.POSSESSION, null);
        check(GhostAbilityCatalog.all(host).isEmpty(), "私有能力不可进入轮盘、总面板与快捷键目录");
        check(!GhostAbilityExecutor.tryExecute(direct) && GhostAbilityExecutor.prepareDeferred(direct) == null && effects == 0,
                "私有能力不得通过直接或延迟主动入口释放");
        check(GhostAbilityExecutor.tryExecute(new GhostAbilityContext(host, id, id, GhostAbilityContext.Source.RULE, null)), "规则仍可调用私有能力");
        check(GhostAbilityExecutor.tryExecute(new GhostAbilityContext(host, id, id, GhostAbilityContext.Source.CONTINUATION, null)) && effects == 2,
                "可信持续流程仍可调用私有能力");
        check(skills(new KnockingGhostModule()).equals(Set.of("knocking_ghost/knock")), "敲门仅公开敲门");
        check(skills(new CallingGhostModule()).equals(Set.of("calling_ghost/follow_call", "calling_ghost/cancel_follow")), "喊人公开喊名与取消");
        check(skills(new GhostStoneBricksModule()).isEmpty(), "鬼石砖反击是私有能力");
        check(skills(new GhostPianoModule()).equals(Set.of("ghost_piano/play", "ghost_piano/stop")), "钢琴只公开播放与停止");
        for (var module : List.of(new WhiteGhostCandleModule(), new RedGhostCandleModule()))
            check(skills(module).equals(Set.of(module.getId().getPath()+"/ignite", module.getId().getPath()+"/extinguish")), "鬼烛只公开点燃与熄灭");
        check(!skills(new GhostCarpetModule()).isEmpty() && !skills(new GhostGrassModule()).isEmpty()
                && new GhostMistModule().getAbilities().stream().anyMatch(a -> a.getId().getPath().endsWith("/attack") && a.isPublicSkill()),
                "地毯、草丛与鬼雾侵蚀不能被批量私有化");
        var piano = new GhostPianoModule();
        var entry = new GhostModuleData.Entry(piano.getId(), 50, 0);
        check(piano.soundEnabled(entry, true) && !piano.soundEnabled(entry, false), "自主状态默认播放，受控状态默认不播放");
        check(!piano.soundEnabled(entry.withDeadlocked(true), true) && !piano.soundEnabled(entry.withSleeping(true), true), "死机或沉睡不自动播放");
        var data = new CompoundTag(); data.putBoolean("musicPlaying", true); data.putBoolean("manualMusic", true);
        var manual = new GhostModuleData.Entry(piano.getId(), 50, 0, false, true, data);
        check(piano.soundEnabled(manual, false), "主动开启的音乐可绕过规则死机继续播放");
        data.putBoolean("musicPlaying", false);
        check(!piano.soundEnabled(new GhostModuleData.Entry(piano.getId(), 50, 0, false, false, data), true), "停止音乐后不得被自动默认值重新打开");
        var behind = CallingGhostModule.behindOffset(0);
        check(behind.distanceToSqr(new Vec3(0, 1, -2.5)) < 1.0e-9, "后方偏移固定于初始朝向");
        check(!GhostSight.inView(new Vec3(0, 0, 1), behind) && GhostSight.inView(behind.normalize(), behind), "看向来源才进入视野");
        check(!GhostSight.inView(new Vec3(0, -1, 0), behind), "低头看不到上方来源不能袭击");
        check(CallingGhostModule.turnAmount(2) > CallingGhostModule.turnAmount(1) && CallingGhostModule.turnAmount(3) == 180, "牵引增强到完整半圈");
        verifyCancellation();
        System.out.println("Private abilities, music state and calling angle regression checks passed");
    }
    /** 空放与独立取消不要求目标、移动或暴露，即使模块死机也能清理状态。 */
    private static void verifyCancellation() {
        var module = new CallingGhostModule();
        if (GhostModuleRegistry.get(module.getId()) == null) GhostModuleRegistry.register(module);
        CompoundTag[] saved = {new CompoundTag()};
        var world = (GhostWorldAccess)Proxy.newProxyInstance(PrivateAbilityTest.class.getClassLoader(),
                new Class<?>[]{GhostWorldAccess.class}, (proxy, method, args) -> {
                    if (method.getName().equals("aimedTarget") || method.getName().equals("target")) return null;
                    throw new AssertionError("取消不应要求世界操作: " + method.getName());
                });
        var host = (GhostModuleHost)Proxy.newProxyInstance(PrivateAbilityTest.class.getClassLoader(),
                new Class<?>[]{GhostModuleHost.class, GhostModuleCustomDataModifier.class}, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getModules" -> List.of(new GhostModuleData.Entry(module.getId(), 30, 0, false, true, saved[0]));
                        case "moduleWorld" -> world;
                        case "getModuleCustomData" -> saved[0].copy();
                        case "setModuleCustomData" -> { saved[0] = ((CompoundTag)args[1]).copy(); yield true; }
                        default -> { if (method.isDefault()) yield InvocationHandler.invokeDefault(proxy, method, args);
                            throw new AssertionError(method.getName()); }
                    };
                });
        for (var ability : List.of(CallingGhostModule.FOLLOW, CallingGhostModule.CANCEL)) {
            saved[0].putUUID("target", java.util.UUID.randomUUID());
            saved[0].putBoolean("manualCalling", true); saved[0].putFloat("pendingTurn", 140);
            check(GhostAbilityExecutor.tryExecuteWithResult(new GhostAbilityContext(host, module.getId(), ability,
                    GhostAbilityContext.Source.POSSESSION, null)), "空放与取消都应成功");
            check(!saved[0].hasUUID("target") && !saved[0].getBoolean("manualCalling")
                    && saved[0].getFloat("pendingTurn") == 0, "取消清除目标和待执行牵引");
        }
    }
}
