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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.UUID;

public final class GhostTombstoneInscriptionSystem {

    /**
     * 每 0.6 秒播放一次刻字音效。
     */
    public static final int SOUND_INTERVAL =
            12;

    private static final java.util.Map<
            UUID,
            InscriptionTask
            > TASKS =
            new java.util.HashMap<>();

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

        int duration =
                calculateInscriptionDuration(
                        inscription
                );

        if (duration <= 0) {
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
         * ========================================================
         */
        blockEntity.setInscription(
                task.inscription()
        );


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
         * 创建诅咒。
         * ========================================================
         */
        GhostTombstoneCurseSource source =
                new GhostTombstoneCurseSource(
                        inscriber.level().dimension(),
                        task.pos()
                );

        net.minecraft.nbt.CompoundTag initialState =
                new net.minecraft.nbt.CompoundTag();

        initialState.putInt(
                "Strength",
                1
        );

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
     * 计算整段文字需要的刻字时间。
     */
    public static int calculateInscriptionDuration(
            String text
    ) {

        int ticks = 0;

        for (int i = 0; i < text.length();) {

            int codePoint =
                    text.codePointAt(i);

            ticks +=
                    getInscriptionTicks(
                            codePoint
                    );

            i +=
                    Character.charCount(
                            codePoint
                    );
        }

        return ticks;
    }


    /**
     * 获取单个 Unicode 字符的刻字时间。
     */
    private static int getInscriptionTicks(
            int codePoint
    ) {

        /*
         * 空格、制表符等空白字符：
         * 不计入时间。
         */
        if (Character.isWhitespace(codePoint)) {
            return 0;
        }

        Character.UnicodeScript script =
                Character.UnicodeScript.of(
                        codePoint
                );


        /*
         * 英文字母：
         * 1.5 秒 = 30 tick。
         */
        if (
                script == Character.UnicodeScript.LATIN
                        && Character.isLetter(codePoint)
        ) {
            return 30;
        }


        /*
         * 中文：
         * 6 秒 = 120 tick。
         */
        if (
                script == Character.UnicodeScript.HAN
        ) {
            return 120;
        }


        /*
         * 日文平假名：
         * 3 秒 = 60 tick。
         */
        if (
                script == Character.UnicodeScript.HIRAGANA
        ) {
            return 60;
        }


        /*
         * 日文片假名：
         * 3 秒 = 60 tick。
         */
        if (
                script == Character.UnicodeScript.KATAKANA
        ) {
            return 60;
        }


        /*
         * 下划线、标点符号、其他字符：
         * 0.5 秒 = 10 tick。
         */
        return 10;
    }


    /**
     * 一次正在进行的刻字任务。
     */
    private static final class InscriptionTask {

        private final UUID inscriber;
        private final net.minecraft.core.BlockPos pos;
        private final String inscription;
        private final UUID targetId;
        private final int duration;

        private int ticks = 0;

        private InscriptionTask(
                UUID inscriber,
                net.minecraft.core.BlockPos pos,
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

        private net.minecraft.core.BlockPos pos() {
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