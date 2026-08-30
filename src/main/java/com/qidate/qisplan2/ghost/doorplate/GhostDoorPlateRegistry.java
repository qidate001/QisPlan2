package com.qidate.qisplan2.ghost.doorplate;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.block.entity.GhostDoorPlateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * 鬼门牌全局注册表。
 *
 * 门牌号 -> 所有拥有这个门牌号的鬼门牌。
 *
 * ============================================================
 *
 * 实际数据由 GhostDoorPlateSavedData 保存。
 *
 * Registry 本身只负责：
 *
 * 1. 提供操作接口
 * 2. 查找目标门牌
 * 3. 执行传送
 *
 * ============================================================
 */
public final class GhostDoorPlateRegistry {

    private GhostDoorPlateRegistry() {
    }


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

            /*
             * 防止外部 Mutable BlockPos 修改注册表数据。
             */
            pos =
                    pos.immutable();
        }
    }


    /*
     * ============================================================
     * 获取 SavedData
     * ============================================================
     */

    private static GhostDoorPlateSavedData getData(
            ServerLevel level
    ) {

        return GhostDoorPlateSavedData.get(
                level
        );
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

        GhostDoorPlateSavedData data =
                getData(level);

        data.register(
                number,
                level.dimension(),
                pos
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

        GhostDoorPlateSavedData data =
                getData(level);

        data.unregisterPosition(
                level.dimension(),
                pos
        );

        QisPlan2.LOGGER.info(
                "[GhostDoorPlate] 删除门牌位置：{} {}",
                level.dimension().location(),
                pos
        );
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

        GhostDoorPlateSavedData data =
                getData(currentLevel);

        Set<DoorLocation> locations =
                data.getLocations(
                        number
                );

        if (locations.isEmpty()) {
            return null;
        }

        DoorLocation current =
                new DoorLocation(
                        currentLevel.dimension(),
                        currentPos
                );

        /*
         * 找第一个不是自己的门牌。
         */
        for (
                DoorLocation location
                : locations
        ) {

            if (location.equals(current)) {
                continue;
            }

            /*
             * 确认目标维度存在。
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
            ServerLevel level,
            int number
    ) {

        return getData(level)
                .getLocations(number);
    }


    /*
     * ============================================================
     * 传送玩家
     *
     * 当前版本：
     *
     * 直接传送到目标门牌。
     *
     * 后续再接：
     *
     * 目标门是否关闭
     * ↓
     * 关闭 -> 虚空杀人规律
     * 开启 -> 正常穿越
     *
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

            QisPlan2.LOGGER.warn(
                    "[GhostDoorPlate] 找不到门牌 {} 的目标",
                    number
            );

            return false;
        }

        ServerLevel destinationLevel =
                server.getLevel(
                        destination.dimension()
                );

        if (destinationLevel == null) {
            return false;
        }

        if (!(destinationLevel.getBlockEntity(destination.pos())
                instanceof GhostDoorPlateBlockEntity plate)) {

            return false;
        }

        BlockPos destinationDoorPos =
                plate.getLinkedDoorPos();

        if (destinationDoorPos == null) {

            QisPlan2.LOGGER.warn(
                    "[GhostDoorPlate] 门牌没有绑定门：{}",
                    destination.pos()
            );

            return false;
        }

        BlockState doorState =
                destinationLevel.getBlockState(
                        destinationDoorPos
                );

        if (!(doorState.getBlock()
                instanceof DoorBlock)) {

            QisPlan2.LOGGER.warn(
                    "[GhostDoorPlate] 绑定门不存在：{}",
                    destinationDoorPos
            );

            return false;
        }

        if (!doorState.getValue(DoorBlock.OPEN)) {

            QisPlan2.LOGGER.info(
                    "[GhostDoorPlate] 玩家 {} 被关闭门吞入虚空。",
                    player.getGameProfile().getName()
            );

            player.teleportTo(
                    destinationLevel,
                    0.5,
                    -1000,
                    0.5,
                    player.getYRot(),
                    player.getXRot()
            );

            return true;
        }

        player.teleportTo(
                destinationLevel,
                destinationDoorPos.getX() + 0.5,
                destinationDoorPos.getY(),
                destinationDoorPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );

        return true;
    }
}