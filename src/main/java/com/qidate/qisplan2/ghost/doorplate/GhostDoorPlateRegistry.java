package com.qidate.qisplan2.ghost.doorplate;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * 鬼门牌全局注册表。
 *
 * 门牌号 -> 所有拥有这个门牌号的鬼门牌。
 *
 * 数据属于整个服务器，而不是某一个维度。
 */
public final class GhostDoorPlateRegistry {

    private GhostDoorPlateRegistry() {
    }

    /*
     * ============================================================
     * 注册表
     * ============================================================
     */

    private static final Map<
            Integer,
            Set<DoorLocation>
            > PLATES = new HashMap<>();


    /*
     * ============================================================
     * 门位置
     * ============================================================
     */

    public record DoorLocation(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {

        public DoorLocation {
            pos = pos.immutable();
        }
    }


    /*
     * ============================================================
     * 注册
     * ============================================================
     */

    public static void register(
            int number,
            ServerLevel level,
            BlockPos pos
    ) {

        unregisterPosition(
                level,
                pos
        );

        PLATES
                .computeIfAbsent(
                        number,
                        ignored -> new HashSet<>()
                )
                .add(
                        new DoorLocation(
                                level.dimension(),
                                pos
                        )
                );

        QisPlan2.LOGGER.info(
                "[GhostDoorPlate] 注册门牌：{} -> {} {}",
                number,
                level.dimension().location(),
                pos
        );
    }


    /*
     * ============================================================
     * 删除某个位置
     * ============================================================
     */

    public static void unregisterPosition(
            ServerLevel level,
            BlockPos pos
    ) {

        ResourceKey<Level> dimension =
                level.dimension();

        for (Iterator<
                Map.Entry<Integer, Set<DoorLocation>>
                > iterator =
             PLATES.entrySet().iterator();
             iterator.hasNext();) {

            Map.Entry<
                    Integer,
                    Set<DoorLocation>
                    > entry =
                    iterator.next();

            Set<DoorLocation> locations =
                    entry.getValue();

            locations.removeIf(
                    location ->
                            location.dimension()
                                    .equals(dimension)
                                    && location.pos()
                                    .equals(pos)
            );

            if (locations.isEmpty()) {
                iterator.remove();
            }
        }
    }


    /*
     * ============================================================
     * 找传送目标
     *
     * 不返回自己。
     * ============================================================
     */

    public static DoorLocation findDestination(
            MinecraftServer server,
            int number,
            ServerLevel currentLevel,
            BlockPos currentPos
    ) {

        Set<DoorLocation> locations =
                PLATES.get(number);

        if (locations == null
                || locations.isEmpty()) {

            return null;
        }

        DoorLocation current =
                new DoorLocation(
                        currentLevel.dimension(),
                        currentPos
                );

        /*
         * 目前先取第一个不是自己的门。
         */
        for (DoorLocation location : locations) {

            if (location.equals(current)) {
                continue;
            }

            /*
             * 确认目标维度仍然存在。
             */
            if (server.getLevel(
                    location.dimension()
            ) == null) {
                continue;
            }

            return location;
        }

        return null;
    }


    /*
     * ============================================================
     * 获取某个门牌号的所有门
     * ============================================================
     */

    public static Set<DoorLocation> getLocations(
            int number
    ) {

        Set<DoorLocation> locations =
                PLATES.get(number);

        if (locations == null) {
            return Set.of();
        }

        return Collections.unmodifiableSet(
                locations
        );
    }


    /*
     * ============================================================
     * 传送玩家
     * ============================================================
     */

    public static boolean teleportToLinkedDoor(
            ServerPlayer player,
            int number,
            ServerLevel currentLevel,
            BlockPos currentPos
    ) {

        MinecraftServer server =
                player.server;

        DoorLocation destination =
                findDestination(
                        server,
                        number,
                        currentLevel,
                        currentPos
                );

        if (destination == null) {
            return false;
        }

        ServerLevel destinationLevel =
                server.getLevel(
                        destination.dimension()
                );

        if (destinationLevel == null) {
            return false;
        }

        BlockPos destinationPos =
                destination.pos();

        /*
         * 传送到门牌前方一点。
         *
         * 暂时直接根据目标门牌位置计算。
         */
        double x =
                destinationPos.getX()
                        + 0.5D;

        double y =
                destinationPos.getY()
                        + 0.1D;

        double z =
                destinationPos.getZ()
                        + 0.5D;

        player.teleportTo(
                destinationLevel,
                x,
                y,
                z,
                player.getYRot(),
                player.getXRot()
        );

        QisPlan2.LOGGER.info(
                "[GhostDoorPlate] 玩家 {} 通过门牌 {} 传送：{} {} -> {} {}",
                player.getGameProfile().getName(),
                number,
                currentLevel.dimension().location(),
                currentPos,
                destination.dimension().location(),
                destinationPos
        );

        return true;
    }
}