package com.qidate.qisplan2.core;

import net.minecraft.world.level.GameRules;

public class ModGameRules {

    private ModGameRules() {}

    public static void init() {
        /*
         * 故意留空。
         *
         * 调用这个方法本身，就会强制 JVM
         * 在正确的时机完成 ModRecipes 的静态初始化。
         */
    }

    /**
     * 灵异攻击是否强制抹杀玩家
     */
    public static final GameRules.Key<GameRules.BooleanValue> GHOST_DAMAGE_INSTANTLY_KILL =
            GameRules.register("ghostDamageInstantlyKill", GameRules.Category.MISC, GameRules.BooleanValue.create(true));

    /**
     * 启用/禁用 许愿鬼
     */
    public static final GameRules.Key<GameRules.BooleanValue> ISAY_ENABLED =
            GameRules.register("isayEnabled", GameRules.Category.MISC, GameRules.BooleanValue.create(true));

    /**
     * 鬼地毯灵异叠加花费时间
     */
    public static final GameRules.Key<GameRules.IntegerValue> GHOST_CARPET_KILL_TIME =
            GameRules.register("ghostCarpetKillTime", GameRules.Category.MISC, GameRules.IntegerValue.create(300));

    /**
     * 多久 Commit 一次重启数据
     */
    public static final GameRules.Key<GameRules.IntegerValue> GHOST_REBOOT_COMMIT_INTERVAL =
            GameRules.register(
                    "ghostRebootCommitInterval",
                    GameRules.Category.MISC,
                    GameRules.IntegerValue.create(200)
            );

    /**
     * 最大存在多少个 Commit
     */
    public static final GameRules.Key<GameRules.IntegerValue> GHOST_REBOOT_MAX_COMMITS =
            GameRules.register(
                    "ghostRebootMaxCommits",
                    GameRules.Category.MISC,
                    GameRules.IntegerValue.create(64)
            );

    /**
     * 重启回溯的速度
     */
    public static final GameRules.Key<GameRules.IntegerValue> GHOST_REBOOT_STEP_INTERVAL =
            GameRules.register(
                    "ghostRebootStepInterval",
                    GameRules.Category.MISC,
                    GameRules.IntegerValue.create(2)
            );

    /**
     * 重启是否回档玩家的位置和视角
     *
     * 包括：
     * X / Y / Z
     * Yaw / Pitch
     */
    public static final GameRules.Key<GameRules.BooleanValue> GHOST_REBOOT_RESTORE_POSITION =
            GameRules.register(
                    "ghostRebootRestorePosition",
                    GameRules.Category.MISC,
                    GameRules.BooleanValue.create(false)
            );

    /**
     * 重启是否回档玩家背包
     *
     * 默认关闭，避免通过时间回溯复制物品。
     */
    public static final GameRules.Key<GameRules.BooleanValue> GHOST_REBOOT_RESTORE_INVENTORY =
            GameRules.register(
                    "ghostRebootRestoreInventory",
                    GameRules.Category.MISC,
                    GameRules.BooleanValue.create(false)
            );
}
