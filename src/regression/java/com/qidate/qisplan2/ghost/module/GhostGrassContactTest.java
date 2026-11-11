package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.implementation.GhostGrassModule;
import net.minecraft.nbt.CompoundTag;

/** 验证首次接触、冷却边界和保存恢复，直接调用规则使用的状态函数。 */
public final class GhostGrassContactTest {
    /** 测试类无需实例化。 */
    private GhostGrassContactTest() {}

    /** 条件不满足时报告对应行为。 */
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    /** 检查重复碰撞不会重复袭击，且不同目标与存档恢复各自保持正确状态。 */
    public static void run() {
        var state = new CompoundTag();
        check(GhostGrassModule.claimAttack(state, 0), "世界初始时首次接触也应立即袭击");
        check(!GhostGrassModule.claimAttack(state, 0), "相邻草丛同 tick 不能重复袭击");
        check(!GhostGrassModule.claimAttack(state, 99), "五秒内不能重复袭击");
        var saved = state.copy();
        check(GhostGrassModule.claimAttack(saved, 100), "保存恢复后冷却到期应允许袭击");
        check(!GhostGrassModule.claimAttack(saved, 199), "冷却从上次尝试重新计时");
        check(GhostGrassModule.claimAttack(saved, 200), "再次到期应允许袭击");
        check(GhostGrassModule.claimAttack(new CompoundTag(), 199), "其他目标不能继承已有冷却");
        check(GhostGrassModule.claimAttack(saved, 1), "时间倒退应重新建立冷却基线");
        check(!GhostGrassModule.claimAttack(saved, 2), "重建后仍应遵守冷却");
        System.out.println("Ghost grass contact regression checks passed");
    }
}
