package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.event.EntityPerceivedEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class GhostPerceptionHandler {

    private static final double DETECTION_RANGE = 64.0D;
    private static final double LOOK_THRESHOLD = 0.85D;
    private static final int DETECTION_INTERVAL = 20;

    private GhostPerceptionHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % DETECTION_INTERVAL != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                detectPerceivedGhosts(player);
            }
        }
    }

    private static void detectPerceivedGhosts(ServerPlayer player) {
        for (ModularGhostEntity ghost : player.level().getEntitiesOfClass(
                ModularGhostEntity.class,
                player.getBoundingBox().inflate(DETECTION_RANGE)
        )) {
            if (!ghost.isAlive() || ghost.isSupernaturallyStunned()) {
                continue;
            }

            if (!isPerceivedBy(player, ghost)) {
                continue;
            }

            GhostModuleRuntime.dispatch(
                    ghost,
                    new EntityPerceivedEvent(player, ghost)
            );
        }
    }

    private static boolean isPerceivedBy(
            ServerPlayer player,
            ModularGhostEntity ghost
    ) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 targetPosition = ghost.getBoundingBox().getCenter();

        Vec3 direction = targetPosition.subtract(eyePosition);
        double distanceSqr = direction.lengthSqr();

        if (distanceSqr > DETECTION_RANGE * DETECTION_RANGE
                || distanceSqr < 1.0E-8D) {
            return false;
        }

        if (!player.hasLineOfSight(ghost)) {
            return false;
        }

        Vec3 lookDirection = player.getViewVector(1.0F).normalize();
        Vec3 targetDirection = direction.normalize();

        return lookDirection.dot(targetDirection) >= LOOK_THRESHOLD;
    }
}