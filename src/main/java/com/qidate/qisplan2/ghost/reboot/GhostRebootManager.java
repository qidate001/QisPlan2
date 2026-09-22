package com.qidate.qisplan2.ghost.reboot;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModGameRules;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GhostRebootManager {

    /**
     * 玩家 → 历史时间线
     */
    private static final Map<UUID, GhostRebootTimeline> TIMELINES =
            new HashMap<>();

    /**
     * 玩家 → 当前是否正在倒流
     */
    private static final Map<UUID, GhostRebootState> REBOOTING =
            new HashMap<>();

    private GhostRebootManager() {
    }

    public static void reboot(
            ServerPlayer player,
            int commitIndex,
            GhostRebootSource source
    ) {

        startReboot(
                player,
                commitIndex,
                source,
                false
        );
    }

    private static void startReboot(
            ServerPlayer player,
            int commitIndex,
            GhostRebootSource source,
            boolean continuous
    ) {

        UUID playerId = player.getUUID();

        GhostRebootTimeline timeline =
                get(player);

        if (REBOOTING.containsKey(playerId)) {

            QisPlan2.LOGGER.info(
                    "[GhostReboot] 玩家 {} 已经处于重启过程中",
                    player.getGameProfile().getName()
            );

            return;
        }

        if (source == null) {

            QisPlan2.LOGGER.warn(
                    "[GhostReboot] 无法重启：未提供重启来源"
            );

            return;
        }

        GhostRebootCommit target =
                timeline.get(commitIndex);

        if (target == null) {

            QisPlan2.LOGGER.warn(
                    "[GhostReboot] 无法重启：Commit #{} 不存在",
                    commitIndex + 1
            );

            return;
        }

        int currentIndex =
                timeline.size() - 1;

        if (commitIndex >= currentIndex) {

            QisPlan2.LOGGER.info(
                    "[GhostReboot] 无法重启：目标 Commit #{} 已经是当前时间或未来",
                    commitIndex + 1
            );

            return;
        }

        GhostRebootState state =
                new GhostRebootState(
                        source,
                        commitIndex,
                        currentIndex,
                        continuous
                );

        REBOOTING.put(
                playerId,
                state
        );

        QisPlan2.LOGGER.info(
                "[GhostReboot] 开始时间倒流：" +
                        "玩家 {} | 来源={} | Commit #{} → Commit #{} | 持续={}",
                player.getGameProfile().getName(),
                describeSource(source),
                currentIndex + 1,
                commitIndex + 1,
                continuous
        );
    }

    public static void tick(ServerPlayer player) {

        UUID playerId =
                player.getUUID();

        /*
         * 当前正在执行一轮倒流。
         */
        if (REBOOTING.containsKey(playerId)) {

            tickReboot(player);

            return;
        }

        /*
         * 正常记录时间线。
         */
        GhostRebootTimeline timeline =
                get(player);

        int commitInterval =
                player.level()
                        .getGameRules()
                        .getInt(
                                ModGameRules.GHOST_REBOOT_COMMIT_INTERVAL
                        );

        if (timeline.shouldCommit(
                player.level().getGameTime(),
                commitInterval
        )) {

            record(
                    player,
                    timeline
            );
        }
    }

    private static void tickReboot(
            ServerPlayer player
    ) {

        UUID playerId =
                player.getUUID();

        GhostRebootState state =
                REBOOTING.get(playerId);

        if (state == null) {
            return;
        }

        /*
         * 倒流期间不让玩家继续移动。
         *
         * 这里暂时只清除速度，
         * 不做更复杂的控制。
         */
        player.setDeltaMovement(
                0.0D,
                0.0D,
                0.0D
        );

        /*
         * 距离下一次倒流还有时间。
         */
        if (state.getTickCounter() > 0) {

            state.tickCounterDown();

            return;
        }

        GhostRebootTimeline timeline =
                get(player);

        int currentIndex =
                state.getCurrentIndex();

        int targetIndex =
                state.getTargetIndex();

        /*
         * 已经到达目标。
         */
        if (currentIndex <= targetIndex) {

            finishReboot(
                    player,
                    timeline,
                    state
            );

            return;
        }

        /*
         * currentIndex 对应：
         *
         * #6
         *
         * 它的 diff 描述的是：
         *
         * #5 → #6
         *
         * 所以逆向应用这个 Diff，
         * 就可以：
         *
         * #6 → #5
         */
        GhostRebootCommit current =
                timeline.get(
                        currentIndex
                );

        if (current == null) {

            QisPlan2.LOGGER.warn(
                    "[GhostReboot] 倒流失败：Commit #{} 不存在",
                    currentIndex + 1
            );

            REBOOTING.remove(
                    playerId
            );

            return;
        }

        QisPlan2.LOGGER.info(
                "[GhostReboot] 时间倒流：Commit #{} → Commit #{}",
                currentIndex + 1,
                currentIndex
        );

        GhostRebootDiffApplier.applyReverse(
                player,
                current.diff()
        );

        /*
         * 当前已经退回上一 Commit。
         */
        currentIndex--;

        state.setCurrentIndex(
                currentIndex
        );

        /*
         * 下一次倒流等待。
         */
        int stepInterval =
                player.level()
                        .getGameRules()
                        .getInt(
                                ModGameRules.GHOST_REBOOT_STEP_INTERVAL
                        );

        state.setTickCounter(
                Math.max(1, stepInterval)
        );

        /*
         * 这一步已经抵达目标。
         */
        if (currentIndex <= targetIndex) {

            finishReboot(
                    player,
                    timeline,
                    state
            );
        }
    }

    private static void finishReboot(
            ServerPlayer player,
            GhostRebootTimeline timeline,
            GhostRebootState state
    ) {
        UUID playerId = player.getUUID();

        int targetIndex = state.getTargetIndex();

        GhostRebootCommit target = timeline.get(targetIndex);

        if (target != null) {
            GhostRebootApplier.apply(
                    player,
                    target.snapshot()
            );
        }

        if (state.isContinuous()) {

            if (targetIndex > 0) {

                state.setCurrentIndex(targetIndex);
                state.setTargetIndex(targetIndex - 1);

                state.setTickCounter(
                        Math.max(
                                1,
                                player.level()
                                        .getGameRules()
                                        .getInt(
                                                ModGameRules.GHOST_REBOOT_STEP_INTERVAL
                                        )
                        )
                );

                QisPlan2.LOGGER.info(
                        "[GhostReboot] 持续重启：继续 Commit #{} → Commit #{}",
                        targetIndex + 1,
                        targetIndex
                );

                return;
            }

            /*
             * 已经到达历史最早点。
             *
             * 此时玩家回到了 Commit #1，
             * 所有位于它之后的旧时间线都应该被抛弃。
             */
            timeline.truncateAfter(targetIndex);

            QisPlan2.LOGGER.info(
                    "[GhostReboot] 持续重启到达历史极限，自动停止：玩家 {}",
                    player.getGameProfile().getName()
            );

            REBOOTING.remove(playerId);

            QisPlan2.LOGGER.info(
                    "[GhostReboot] 重启完成：玩家 {} 已回到 Commit #{}",
                    player.getGameProfile().getName(),
                    targetIndex + 1
            );

            return;
        }

        /*
         * 一次性重启：
         * 回到目标时间点后，直接丢弃未来。
         */
        timeline.truncateAfter(targetIndex);

        REBOOTING.remove(playerId);

        QisPlan2.LOGGER.info(
                "[GhostReboot] 重启完成：玩家 {} 已回到 Commit #{}",
                player.getGameProfile().getName(),
                targetIndex + 1
        );
    }

    public static void record(
            ServerPlayer player,
            GhostRebootTimeline timeline
    ) {

        GhostRebootSnapshot snapshot =
                GhostRebootSnapshotBuilder.capture(
                        player
                );

        GhostRebootCommit previous =
                timeline.latest();

        GhostRebootDiff diff;

        if (previous == null) {

            diff =
                    new GhostRebootDiff();

        } else {

            boolean restoreInventory =
                    player.level()
                            .getGameRules()
                            .getBoolean(
                                    ModGameRules.GHOST_REBOOT_RESTORE_INVENTORY
                            );

            diff =
                    GhostRebootDiffBuilder.build(
                            previous.snapshot(),
                            snapshot,
                            restoreInventory
                    );
        }

        GhostRebootCommit commit =
                new GhostRebootCommit(
                        player.serverLevel().getGameTime(),
                        snapshot,
                        diff
                );

        timeline.push(commit);

        int maxCommits =
                player.level()
                        .getGameRules()
                        .getInt(
                                ModGameRules.GHOST_REBOOT_MAX_COMMITS
                        );

        timeline.trim(maxCommits);

        logCommit(
                player,
                timeline,
                commit
        );
    }

    public static void toggleContinuousReboot(
            ServerPlayer player,
            GhostRebootSource source
    ) {

        UUID playerId = player.getUUID();

        GhostRebootState state = REBOOTING.get(playerId);

        if (state != null && state.isContinuous()) {

            state.setContinuous(false);

            QisPlan2.LOGGER.info(
                    "[GhostReboot] 玩家 {} 已关闭持续重启",
                    player.getGameProfile().getName()
            );

            return;
        }

        GhostRebootTimeline timeline = get(player);

        if (timeline.size() <= 1) {

            QisPlan2.LOGGER.info(
                    "[GhostReboot] 玩家 {} 无法开始持续重启：时间线中没有足够的历史记录",
                    player.getGameProfile().getName()
            );

            return;
        }

        startReboot(
                player,
                timeline.size() - 2,
                source,
                true
        );
    }

    private static void logCommit(
            ServerPlayer player,
            GhostRebootTimeline timeline,
            GhostRebootCommit commit
    ) {

        GhostRebootSnapshot snapshot =
                commit.snapshot();

        GhostRebootDiff diff =
                commit.diff();

        QisPlan2.LOGGER.info(
                "[GhostReboot] Commit #{} | player={} | gameTime={} | " +
                        "pos=({}, {}, {}) | health={} | food={} | " +
                        "diff={}",
                timeline.size(),
                player.getGameProfile().getName(),
                commit.gameTime(),
                String.format("%.2f", snapshot.x),
                String.format("%.2f", snapshot.y),
                String.format("%.2f", snapshot.z),
                String.format("%.1f", snapshot.health),
                snapshot.food,
                describeDiff(diff)
        );
    }

    private static String describeSource(
            GhostRebootSource source
    ) {

        if (source == null) {
            return "UNKNOWN";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                source.type()
        );

        if (source.sourceType() != null) {

            result.append(
                    "("
            );

            result.append(
                    source.sourceType()
            );

            result.append(
                    ")"
            );
        }

        if (source.sourceUUID() != null) {

            result.append(
                    "["
            );

            result.append(
                    source.sourceUUID()
            );

            result.append(
                    "]"
            );
        }

        return result.toString();
    }

    private static String describeDiff(
            GhostRebootDiff diff
    ) {

        if (diff.isEmpty()) {
            return "无变化";
        }

        StringBuilder result =
                new StringBuilder();

        if (diff.dx != null
                || diff.dy != null
                || diff.dz != null
                || diff.dyaw != null
                || diff.dpitch != null) {

            result.append("位置");

            if (diff.dx != null) {
                result.append(
                        String.format(
                                " Δx=%.2f",
                                diff.dx
                        )
                );
            }

            if (diff.dy != null) {
                result.append(
                        String.format(
                                " Δy=%.2f",
                                diff.dy
                        )
                );
            }

            if (diff.dz != null) {
                result.append(
                        String.format(
                                " Δz=%.2f",
                                diff.dz
                        )
                );
            }

            if (diff.dyaw != null) {
                result.append(
                        String.format(
                                " Δyaw=%.1f",
                                diff.dyaw
                        )
                );
            }

            if (diff.dpitch != null) {
                result.append(
                        String.format(
                                " Δpitch=%.1f",
                                diff.dpitch
                        )
                );
            }

            result.append("; ");
        }

        if (diff.health != null) {

            result.append(
                    String.format(
                            "生命 Δ%.1f; ",
                            diff.health
                    )
            );
        }

        if (diff.food != null) {

            result.append(
                    "饥饿 Δ"
                            + diff.food
                            + "; "
            );
        }

        if (!diff.ghostChanges.isEmpty()) {

            result.append(
                    "驭鬼变化="
                            + diff.ghostChanges.size()
                            + "; "
            );
        }

        if (!diff.inventoryChanges.isEmpty()) {

            result.append(
                    "背包变化="
                            + diff.inventoryChanges.size()
                            + "格; "
            );
        }

        return result.toString();
    }

    public static GhostRebootTimeline get(
            ServerPlayer player
    ) {

        return TIMELINES.computeIfAbsent(
                player.getUUID(),
                ignored -> new GhostRebootTimeline()
        );
    }
}