package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

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

        timeline.push(
                new GhostRebootCommit(
                        player.serverLevel().getGameTime(),
                        snapshot,
                        diff
                )
        );

        timeline.trim(
                MAX_COMMITS
        );
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