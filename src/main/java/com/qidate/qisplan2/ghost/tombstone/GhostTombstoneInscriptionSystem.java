package com.qidate.qisplan2.ghost.tombstone;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.block.entity.GhostTombstoneBlockEntity;
import com.qidate.qisplan2.core.ModSounds;
import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.ghost.curse.CurseRegistry;
import com.qidate.qisplan2.ghost.curse.CurseType;
import com.qidate.qisplan2.ghost.curse.type.ghosttombstone.GhostTombstoneCurseSource;
import com.qidate.qisplan2.ghost.curse.type.ghosttombstone.GhostTombstoneCurseType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GhostTombstoneInscriptionSystem {

    /**
     * 每 0.6 秒播放一次刻字音效。
     */
    public static final int SOUND_INTERVAL = 12;

    private static final Map<
            UUID,
            InscriptionTask
            > TASKS =
            new HashMap<>();

    private GhostTombstoneInscriptionSystem() {
    }


    /**
     * 开始一次刻字。
     */
    public static void start(
            MinecraftServer server,
            ServerPlayer inscriber,
            GhostTombstoneBlockEntity blockEntity,
            String inscription
    ) {

        /*
         * 同一个玩家不能同时刻多个墓碑。
         */
        TASKS.remove(
                inscriber.getUUID()
        );

        ServerPlayer target =
                server.getPlayerList()
                        .getPlayerByName(
                                inscription
                        );

        UUID targetId =
                target != null
                        ? target.getUUID()
                        : null;

        /*
         * 根据旧铭文与新铭文的实际差异，
         * 计算擦除与刻写所需要的时间。
         */
        int duration =
                calculateInscriptionDuration(
                        blockEntity.getInscription(),
                        inscription
                );

        /*
         * 没有任何实际操作。
         *
         * 例如：
         * 原铭文为空，新铭文也是空。
         *
         * 这种情况不能创建一个永远不会完成的任务，
         * 而应该直接完成。
         */
        if (duration <= 0) {

            InscriptionTask task =
                    new InscriptionTask(
                            inscriber.getUUID(),
                            blockEntity.getBlockPos(),
                            inscription,
                            targetId,
                            0
                    );

            finish(
                    server,
                    inscriber,
                    task
            );

            return;
        }

        InscriptionTask task =
                new InscriptionTask(
                        inscriber.getUUID(),
                        blockEntity.getBlockPos(),
                        inscription,
                        targetId,
                        duration
                );

        TASKS.put(
                inscriber.getUUID(),
                task
        );

        QisPlan2.LOGGER.info(
                "[鬼墓碑] {} 开始刻字：{}，预计 {} tick",
                inscriber.getGameProfile().getName(),
                inscription,
                duration
        );
    }


    /**
     * 每个服务器 Tick 调用。
     */
    public static void tick(
            MinecraftServer server
    ) {

        var iterator =
                TASKS.entrySet().iterator();

        while (iterator.hasNext()) {

            InscriptionTask task =
                    iterator.next().getValue();

            ServerPlayer inscriber =
                    server.getPlayerList()
                            .getPlayer(
                                    task.inscriber()
                            );

            /*
             * 刻字者已经离线。
             *
             * 当前阶段直接取消刻字。
             */
            if (inscriber == null) {
                iterator.remove();
                continue;
            }

            task.ticks++;

            /*
             * ====================================================
             * 每 0.6 秒播放一次声音
             * ====================================================
             */

            if (task.ticks % SOUND_INTERVAL == 0) {

                playInscriptionSound(
                        server,
                        inscriber,
                        task
                );
            }

            /*
             * ====================================================
             * 完成刻字
             * ====================================================
             */

            if (task.ticks >= task.duration()) {

                finish(
                        server,
                        inscriber,
                        task
                );

                iterator.remove();
            }
        }
    }


    /**
     * 播放刻字声音。
     *
     * <p>
     * 刻字者与被刻名字的玩家都会听见。
     */
    private static void playInscriptionSound(
            MinecraftServer server,
            ServerPlayer inscriber,
            InscriptionTask task
    ) {

        /*
         * 刻字者听见。
         */
        inscriber.playNotifySound(
                ModSounds.GHOST_TOMBSTONE_INSCRIBE.get(),
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );

        /*
         * 被刻名字的玩家听见。
         */
        if (task.targetId() != null) {

            ServerPlayer target =
                    server.getPlayerList()
                            .getPlayer(
                                    task.targetId()
                            );

            if (target != null
                    && target != inscriber) {

                target.playNotifySound(
                        ModSounds.GHOST_TOMBSTONE_INSCRIBE.get(),
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F
                );
            }
        }
    }


    /**
     * 刻字完成。
     */
    private static void finish(
            MinecraftServer server,
            ServerPlayer inscriber,
            InscriptionTask task
    ) {

        if (!(inscriber.level()
                .getBlockEntity(
                        task.pos()
                )
                instanceof GhostTombstoneBlockEntity blockEntity)) {

            return;
        }

        /*
         * ========================================================
         * 先移除墓碑原本产生的诅咒。
         * ========================================================
         */
        CurseManager.removeBySource(
                server,
                curse -> {

                    if (!(curse.getSource()
                            instanceof GhostTombstoneCurseSource source)) {

                        return false;
                    }

                    return source.dimension().equals(
                            inscriber.level().dimension()
                    )
                            && source.pos().equals(
                            task.pos()
                    );
                }
        );

        /*
         * ========================================================
         * 保存新的刻字。
         *
         * 即使新铭文为空，也允许正常保存。
         * ========================================================
         */
        blockEntity.setInscription(
                task.inscription()
        );

        /*
         * ========================================================
         * 如果最终铭文为空，
         * 那么前面的旧诅咒已经移除，
         * 此次刻字到此结束。
         * ========================================================
         */
        if (task.inscription().isEmpty()) {

            QisPlan2.LOGGER.info(
                    "[鬼墓碑] {} 完成擦除：墓碑铭文已清空",
                    inscriber.getGameProfile().getName()
            );

            return;
        }

        /*
         * ========================================================
         * 查找新的目标。
         * ========================================================
         */
        ServerPlayer target =
                server.getPlayerList()
                        .getPlayerByName(
                                task.inscription()
                        );

        if (target == null) {
            QisPlan2.LOGGER.info(
                    "[鬼墓碑] {} 完成刻字：{}，但目标玩家不在线",
                    inscriber.getGameProfile().getName(),
                    task.inscription()
            );

            return;
        }

        /*
         * ========================================================
         * 获取墓碑诅咒类型。
         * ========================================================
         */
        CurseType curseType =
                CurseRegistry.get(
                        GhostTombstoneCurseType.ID
                );

        if (curseType == null) {
            return;
        }

        /*
         * ========================================================
         * 创建诅咒来源。
         * ========================================================
         */
        GhostTombstoneCurseSource source =
                new GhostTombstoneCurseSource(
                        inscriber.level().dimension(),
                        task.pos()
                );

        CompoundTag initialState =
                new CompoundTag();

        initialState.putInt(
                "Strength",
                1
        );

        /*
         * ========================================================
         * 创建诅咒。
         * ========================================================
         */
        Curse curse =
                curseType.create(
                        target.getUUID(),
                        source,
                        initialState
                );

        CurseManager.add(
                server,
                curse
        );

        QisPlan2.LOGGER.info(
                "[鬼墓碑] {} 完成刻字：{}",
                inscriber.getGameProfile().getName(),
                task.inscription()
        );
    }


    /**
     * 计算一次刻字操作需要的时间。
     *
     * <p>
     * 使用最长公共子序列（LCS）寻找旧铭文与新铭文
     * 中可以保留的部分。
     *
     * <p>
     * 不在公共子序列中的旧字符视为需要擦除，
     * 不在公共子序列中的新字符视为需要刻写。
     */
    private static int calculateInscriptionDuration(
            String oldText,
            String newText
    ) {

        int[] oldCodePoints =
                oldText.codePoints().toArray();

        int[] newCodePoints =
                newText.codePoints().toArray();

        int oldLength =
                oldCodePoints.length;

        int newLength =
                newCodePoints.length;

        /*
         * dp[i][j]：
         *
         * oldText 前 i 个字符
         * 与
         * newText 前 j 个字符
         *
         * 所需要的最小操作时间。
         */
        int[][] dp =
                new int[
                        oldLength + 1
                        ][
                        newLength + 1
                        ];

        /*
         * ========================================================
         * 初始化：
         *
         * 从空字符串变成目标字符串，
         * 只能进行刻写。
         * ========================================================
         */
        for (int j = 1; j <= newLength; j++) {

            dp[0][j] =
                    dp[0][j - 1]
                            + getInscriptionTicks(
                            newCodePoints[j - 1]
                    );
        }

        /*
         * ========================================================
         * 初始化：
         *
         * 从旧字符串变成空字符串，
         * 只能进行擦除。
         * ========================================================
         */
        for (int i = 1; i <= oldLength; i++) {

            dp[i][0] =
                    dp[i - 1][0]
                            + getEraseTicks(
                            oldCodePoints[i - 1]
                    );
        }

        /*
         * ========================================================
         * 动态规划。
         * ========================================================
         */
        for (int i = 1; i <= oldLength; i++) {

            for (int j = 1; j <= newLength; j++) {

                int oldCodePoint =
                        oldCodePoints[i - 1];

                int newCodePoint =
                        newCodePoints[j - 1];

                /*
                 * 两个字符相同。
                 *
                 * 直接保留，不需要任何时间。
                 */
                if (oldCodePoint == newCodePoint) {

                    dp[i][j] =
                            dp[i - 1][j - 1];

                    continue;
                }

                /*
                 * 删除旧字符。
                 */
                int eraseCost =
                        dp[i - 1][j]
                                + getEraseTicks(
                                oldCodePoint
                        );

                /*
                 * 新增新字符。
                 */
                int inscriptionCost =
                        dp[i][j - 1]
                                + getInscriptionTicks(
                                newCodePoint
                        );

                /*
                 * 当前选择耗时更少的方案。
                 */
                dp[i][j] =
                        Math.min(
                                eraseCost,
                                inscriptionCost
                        );
            }
        }

        return dp[oldLength][newLength];
    }


    /**
     * 获取刻写一个新字符需要的时间。
     */
    private static int getInscriptionTicks(
            int codePoint
    ) {

        /*
         * 空白不需要刻写时间。
         */
        if (Character.isWhitespace(codePoint)) {
            return 0;
        }

        Character.UnicodeScript script =
                Character.UnicodeScript.of(codePoint);

        /*
         * 中文。
         */
        if (script == Character.UnicodeScript.HAN) {
            return 120;
        }

        /*
         * 日文平假名 / 片假名。
         */
        if (script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA) {
            return 60;
        }

        /*
         * 英文字母。
         */
        if (script == Character.UnicodeScript.LATIN
                && Character.isLetter(codePoint)) {
            return 30;
        }

        /*
         * 下划线、标点、数字等。
         */
        return 10;
    }


    /**
     * 获取擦除一个已有字符需要的时间。
     */
    private static int getEraseTicks(
            int codePoint
    ) {

        /*
         * 空白不需要擦除时间。
         */
        if (Character.isWhitespace(codePoint)) {
            return 0;
        }

        Character.UnicodeScript script =
                Character.UnicodeScript.of(codePoint);

        /*
         * 中文。
         */
        if (script == Character.UnicodeScript.HAN) {
            return 8;
        }

        /*
         * 日文。
         */
        if (script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA) {
            return 5;
        }

        /*
         * 英文。
         */
        if (script == Character.UnicodeScript.LATIN
                && Character.isLetter(codePoint)) {
            return 3;
        }

        /*
         * 下划线、标点、数字等。
         */
        return 1;
    }


    /**
     * 一次正在进行的刻字任务。
     */
    private static final class InscriptionTask {

        private final UUID inscriber;
        private final BlockPos pos;
        private final String inscription;
        private final UUID targetId;
        private final int duration;

        private int ticks = 0;

        private InscriptionTask(
                UUID inscriber,
                BlockPos pos,
                String inscription,
                UUID targetId,
                int duration
        ) {
            this.inscriber = inscriber;
            this.pos = pos;
            this.inscription = inscription;
            this.targetId = targetId;
            this.duration = duration;
        }

        private UUID inscriber() {
            return inscriber;
        }

        private BlockPos pos() {
            return pos;
        }

        private String inscription() {
            return inscription;
        }

        private UUID targetId() {
            return targetId;
        }

        private int duration() {
            return duration;
        }
    }
}