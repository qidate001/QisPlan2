package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.implementation.GhostCarpetModule;
import net.minecraft.nbt.CompoundTag;

/** 无游戏世界的接触计时边界检查；直接执行生产代码使用的累计函数。 */
public final class GhostCarpetContactTest {
    /** 测试入口无需创建实例。 */
    private GhostCarpetContactTest() {}

    /** 不满足回归条件时给出明确失败原因。 */
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    /** 检查阈值、离开暂停、保存恢复、独立目标、重复触发和旧世界设置兼容。 */
    public static void run() {
        var first = new CompoundTag();
        var second = new CompoundTag();
        for (int tick = 0; tick < 299; tick++)
            check(GhostCarpetModule.accumulate(first, 1) == 0, "不足15秒不能袭击");
        check(GhostCarpetModule.accumulate(first, 1) == 1, "第300 tick必须触发一次");
        check(first.getInt("contactTicks") == 0, "发起一轮袭击后清除该轮累计量");
        check(GhostCarpetModule.accumulate(first, 150) == 0, "第二轮必须重新累计");
        var saved = first.copy();
        // 离开时不会调用累计函数；读档只恢复原进度，不补算离开或离线的时间。
        check(GhostCarpetModule.accumulate(saved, 0) == 0 && saved.getInt("contactTicks") == 150,
                "离开期间必须暂停并保留进度");
        check(GhostCarpetModule.accumulate(second, 149) == 0, "其他目标应独立累计");
        check(GhostCarpetModule.accumulate(saved, 150) == 1, "重新接触应从原进度继续");
        check(second.getInt("contactTicks") == 149, "一个目标触发不能清空另一目标");
        check(GhostCarpetModule.accumulate(saved, 610) == 2 && saved.getInt("contactTicks") == 10,
                "批量接触时间应保留轮数和余数");
        check(GhostCarpetModule.accumulate(new CompoundTag(), 100, 100) == 1,
                "保留既有世界配置的接触间隔");
        var corrupt = new CompoundTag();
        corrupt.putInt("contactTicks", -10);
        check(GhostCarpetModule.accumulate(corrupt, 1) == 0 && corrupt.getInt("contactTicks") == 1,
                "负进度应按零恢复");

        // 不变的时间配置仍然来自模块，静态载体声明与模块初始强度一致。
        check(new GhostCarpetModule().getInitialData().intensity() == 2, "迁移应保留初始袭击强度");
        check(GhostCarpetModule.CONTACT_TICKS == 300, "默认接触时间应为15秒");

        boolean rejected = false;
        try { GhostCarpetModule.accumulate(new CompoundTag(), -1); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "不能使用负接触时间返还已经经过的时间");
        System.out.println("Ghost carpet contact regression checks passed");
    }
}
