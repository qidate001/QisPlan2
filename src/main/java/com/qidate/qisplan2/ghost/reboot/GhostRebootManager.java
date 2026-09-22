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

    public static void tick(
            ServerPlayer player
    ) {

//        if (GhostEyeDomainController.getEyeLayer(player) < 6) {
//            return;
//        }

        UUID playerId =
                player.getUUID();

        /*
         * 正在重启：
         * 不记录新的 Commit，只推进时间倒流。
         */
        if (REBOOTING.containsKey(playerId)) {

            tickReboot(
                    player
            );

            return;
        }

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

        UUID playerId =
                player.getUUID();

        int targetIndex =
                state.getTargetIndex();

        /*
         * 最后再用一次 Snapshot 校准。
         *
         * 这样可以保证：
         * 即使中途有细微的浮点误差，
         * 最终状态也一定精确落在目标 Commit。
         */
        GhostRebootCommit target =
                timeline.get(
                        targetIndex
                );

        if (target != null) {

            GhostRebootApplier.apply(
                    player,
                    target.snapshot()
            );
        }

        /*
         * 舍弃未来。
         */
        timeline.truncateAfter(
                targetIndex
        );

        REBOOTING.remove(
                playerId
        );

        QisPlan2.LOGGER.info(
                "[GhostReboot] 重启完成：玩家 {} 已回到 Commit #{}，" +
                        "时间线剩余 {} 个 Commit",
                player.getGameProfile().getName(),
                targetIndex + 1,
                timeline.size()
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

    public static void reboot(
            ServerPlayer player,
            int commitIndex
    ) {

        UUID playerId =
                player.getUUID();

        GhostRebootTimeline timeline =
                get(player);

        /*
         * 已经在重启，不允许重复启动。
         */
        if (REBOOTING.containsKey(playerId)) {

            QisPlan2.LOGGER.info(
                    "[GhostReboot] 玩家 {} 已经处于重启过程中",
                    player.getGameProfile().getName()
            );

            return;
        }

        GhostRebootCommit target =
                timeline.get(
                        commitIndex
                );

        if (target == null) {

            QisPlan2.LOGGER.warn(
                    "[GhostReboot] 无法重启：Commit #{} 不存在",
                    commitIndex + 1
            );

            return;
        }

        int currentIndex =
                timeline.size() - 1;

        /*
         * 不能往未来重启。
         */
        if (commitIndex >= currentIndex) {

            QisPlan2.LOGGER.info(
                    "[GhostReboot] 无法重启：目标 Commit #{} 已经是当前时间或未来",
                    commitIndex + 1
            );

            return;
        }

        GhostRebootState state =
                new GhostRebootState(
                        commitIndex,
                        currentIndex
                );

        REBOOTING.put(
                playerId,
                state
        );

        QisPlan2.LOGGER.info(
                "[GhostReboot] 开始时间倒流：玩家 {} | Commit #{} → Commit #{}",
                player.getGameProfile().getName(),
                currentIndex + 1,
                commitIndex + 1
        );
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