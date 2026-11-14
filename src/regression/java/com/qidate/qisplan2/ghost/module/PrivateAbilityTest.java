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
        check(skills(new CallingGhostModule()).equals(Set.of("calling_ghost/follow_call")), "喊人仅公开持续喊名");
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
        check(!CallingGhostModule.isTurnAround(0, 45) && !CallingGhostModule.isTurnAround(0, 160)
                && CallingGhostModule.isTurnAround(0, 180) && CallingGhostModule.isTurnAround(170, -10), "回头依据遭遇起始角度并处理跨越正负180度");
        check(CallingGhostModule.turnAmount(2) > CallingGhostModule.turnAmount(1)
                && CallingGhostModule.turnAmount(1000) == 90, "喊名转动逐次增加且有单次幅度上限");
        System.out.println("Private abilities, music state and calling angle regression checks passed");
    }
}
