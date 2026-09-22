package com.qidate.qisplan2.ghost.domain;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class GhostDomainTargetHandler {

    private static final double MAX_DISTANCE = 128.0D;

    private GhostDomainTargetHandler() {
    }

    public static Entity getLookTarget(
            ServerPlayer player
    ) {

        Vec3 start =
                player.getEyePosition();

        Vec3 look =
                player.getLookAngle();

        Vec3 end =
                start.add(
                        look.scale(MAX_DISTANCE)
                );

        AABB searchBox =
                player.getBoundingBox()
                        .expandTowards(
                                look.scale(MAX_DISTANCE)
                        )
                        .inflate(1.0D);

        EntityHitResult result = null;

        double closestDistance =
                MAX_DISTANCE * MAX_DISTANCE;

        for (Entity entity :
                player.level().getEntities(
                        player,
                        searchBox,
                        entity -> entity.isPickable()
                )) {

            AABB boundingBox =
                    entity.getBoundingBox()
                            .inflate(0.3D);

            var hit =
                    boundingBox.clip(
                            start,
                            end
                    );

            if (hit.isEmpty()) {
                continue;
            }

            double distance =
                    start.distanceToSqr(
                            hit.get()
                    );

            if (distance < closestDistance) {
                closestDistance = distance;

                result =
                        new EntityHitResult(
                                entity,
                                hit.get()
                        );
            }
        }

        return result == null
                ? null
                : result.getEntity();
    }
}