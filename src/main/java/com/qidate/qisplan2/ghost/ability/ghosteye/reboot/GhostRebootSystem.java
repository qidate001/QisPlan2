package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.domain.type.eye.GhostEyeDomainController;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GhostRebootSystem {

    private static final int COMMIT_INTERVAL = 200;

    private static final int MAX_COMMITS = 18;

    private static final Map<UUID, GhostRebootTimeline> TIMELINES =
            new HashMap<>();

    private GhostRebootSystem() {
    }

    public static void tick(
            ServerPlayer player
    ) {

//        if (GhostEyeDomainController.getEyeLayer(player) < 6) {
//            return;
//        }

        GhostRebootTimeline timeline =
                get(player);

        long gameTime =
                player.serverLevel().getGameTime();

        if (!timeline.shouldCommit(gameTime)) {
            return;
        }

        record(
                player,
                timeline
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

            diff =
                    GhostRebootDiffBuilder.build(
                            previous.snapshot(),
                            snapshot
                    );
        }

        GhostRebootCommit commit =
                new GhostRebootCommit(
                        player.serverLevel().getGameTime(),
                        snapshot,
                        diff
                );

        timeline.push(commit);

        timeline.trim(
                MAX_COMMITS
        );

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

        if (timeline.size() > 3){
            GhostRebootSystem.reboot(player, 1);
        }
    }

    public static void reboot(
            ServerPlayer player,
            int commitIndex
    ) {

        GhostRebootTimeline timeline =
                get(player);

        GhostRebootCommit commit =
                timeline.get(commitIndex);

        if (commit == null) {
            QisPlan2.LOGGER.warn(
                    "[GhostReboot] 无法重启：Commit #{} 不存在",
                    commitIndex
            );
            return;
        }

        QisPlan2.LOGGER.info(
                "[GhostReboot] 开始重启玩家 {} → Commit #{}",
                player.getGameProfile().getName(),
                commitIndex + 1
        );

        GhostRebootApplier.apply(
                player,
                commit.snapshot()
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
                || diff.dz != null) {

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

            result.append("; ");
        }

        if (diff.health != null) {

            result.append(
                    String.format(
                            "生命→%.1f; ",
                            diff.health
                    )
            );
        }

        if (diff.food != null) {

            result.append(
                    "饥饿→"
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