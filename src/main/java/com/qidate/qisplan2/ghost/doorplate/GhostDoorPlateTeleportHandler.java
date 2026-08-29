package com.qidate.qisplan2.ghost.doorplate;

import com.qidate.qisplan2.block.entity.GhostDoorPlateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GhostDoorPlateTeleportHandler {

    private GhostDoorPlateTeleportHandler() {
    }

    /*
     * ============================================================
     * 传送冷却
     * ============================================================
     *
     * 防止：
     *
     * A门
     *  ↓
     * B门
     *  ↓
     * 下一 tick 又被 B 门传回 A
     *
     * 这里给玩家一个短暂冷却。
     */
    private static final Map<
            UUID,
            Integer
            > TELEPORT_COOLDOWN =
            new HashMap<>();


    /*
     * ============================================================
     * 上一 Tick 的位置
     * ============================================================
     *
     * 用来判断：
     *
     * 玩家是不是「穿过」了门。
     *
     * 而不是单纯站在门附近。
     */
    private static final Map<
            UUID,
            PlayerPosition
            > LAST_POSITIONS =
            new HashMap<>();


    private record PlayerPosition(
            double x,
            double y,
            double z
    ) {
    }


    /*
     * ============================================================
     * 注册事件
     * ============================================================
     */

    public static void register() {

        NeoForge.EVENT_BUS.register(
                GhostDoorPlateTeleportHandler.class
        );
    }


    /*
     * ============================================================
     * 玩家 Tick
     * ============================================================
     */

    @SubscribeEvent
    public static void onPlayerTick(
            PlayerTickEvent.Post event
    ) {

        if (!(event.getEntity()
                instanceof ServerPlayer player)) {

            return;
        }


        UUID uuid =
                player.getUUID();


        /*
         * ========================================================
         * 更新冷却
         * ========================================================
         */

        Integer cooldown =
                TELEPORT_COOLDOWN.get(uuid);

        if (cooldown != null) {

            if (cooldown <= 1) {

                TELEPORT_COOLDOWN.remove(uuid);

            } else {

                TELEPORT_COOLDOWN.put(
                        uuid,
                        cooldown - 1
                );
            }
        }


        /*
         * ========================================================
         * 获取上一 Tick 位置
         * ========================================================
         */

        PlayerPosition previous =
                LAST_POSITIONS.put(
                        uuid,
                        new PlayerPosition(
                                player.getX(),
                                player.getY(),
                                player.getZ()
                        )
                );


        /*
         * 第一次记录位置。
         */
        if (previous == null) {
            return;
        }


        /*
         * 仍在传送冷却中。
         */
        if (TELEPORT_COOLDOWN.containsKey(uuid)) {
            return;
        }


        /*
         * ========================================================
         * 检查玩家是否穿过鬼门牌对应的门
         * ========================================================
         */

        checkDoorCrossing(
                player,
                previous
        );
    }


    /*
     * ============================================================
     * 检查穿门
     * ============================================================
     */

    private static void checkDoorCrossing(
            ServerPlayer player,
            PlayerPosition previous
    ) {

        BlockPos currentPos =
                player.blockPosition();


        /*
         * 玩家附近寻找门。
         *
         * 门本身通常只需要检查玩家当前位置
         * 附近 1 格。
         */
        for (BlockPos pos :
                BlockPos.betweenClosed(
                        currentPos.offset(-1, -1, -1),
                        currentPos.offset(1, 1, 1)
                )) {

            /*
             * ====================================================
             * 找门
             * ====================================================
             */

            BlockState doorState =
                    player.serverLevel()
                            .getBlockState(pos);


            if (!(doorState.getBlock()
                    instanceof DoorBlock)) {

                continue;
            }


            /*
             * 只处理门的下半部分。
             */
            if (doorState.hasProperty(
                    DoorBlock.HALF
            )
                    && doorState.getValue(
                    DoorBlock.HALF
            ) != DoubleBlockHalf.LOWER) {

                continue;
            }


            /*
             * ====================================================
             * 找这个门上的鬼门牌
             * ====================================================
             */

            GhostDoorPlateBlockEntity plate =
                    findDoorPlate(
                            player,
                            pos,
                            doorState
                    );


            if (plate == null) {
                continue;
            }


            /*
             * ====================================================
             * 确认玩家真的穿过门
             * ====================================================
             */

            if (!crossedDoor(
                    player,
                    previous,
                    pos,
                    doorState
            )) {

                continue;
            }


            /*
             * ====================================================
             * 获取门牌号
             * ====================================================
             */

            int number =
                    plate.getNumber();


            /*
             * ====================================================
             * 传送
             * ====================================================
             */

            if (GhostDoorPlateRegistry
                    .teleportToLinkedDoor(
                            player,
                            number,
                            player.serverLevel(),
                            plate.getBlockPos()
                    )) {

                /*
                 * 防止立即从另一扇门传回来。
                 */
                TELEPORT_COOLDOWN.put(
                        player.getUUID(),
                        20
                );

                return;
            }
        }
    }


    /*
     * ============================================================
     * 寻找挂在门上的鬼门牌
     * ============================================================
     */

    private static GhostDoorPlateBlockEntity findDoorPlate(
            ServerPlayer player,
            BlockPos doorPos,
            BlockState doorState
    ) {

        Direction doorFacing =
                doorState.getValue(
                        DoorBlock.FACING
                );


        /*
         * 门牌通常就在门的侧面/附近。
         *
         * 第一版直接检查门周围一圈。
         */
        for (BlockPos platePos :
                BlockPos.betweenClosed(
                        doorPos.offset(-1, 0, -1),
                        doorPos.offset(1, 2, 1)
                )) {

            if (!(player.serverLevel()
                    .getBlockEntity(platePos)
                    instanceof GhostDoorPlateBlockEntity plate)) {

                continue;
            }


            /*
             * 找到了门牌。
             */
            return plate;
        }

        return null;
    }


    /*
     * ============================================================
     * 判断玩家是否穿过门
     * ============================================================
     */

    private static boolean crossedDoor(
            ServerPlayer player,
            PlayerPosition previous,
            BlockPos doorPos,
            BlockState doorState
    ) {

        Direction facing =
                doorState.getValue(
                        DoorBlock.FACING
                );


        /*
         * 门的中心。
         */
        double doorX =
                doorPos.getX() + 0.5D;

        double doorY =
                doorPos.getY();

        double doorZ =
                doorPos.getZ() + 0.5D;


        /*
         * 玩家上一 Tick 相对于门的位置。
         */
        double previousSide =
                getSide(
                        previous.x(),
                        previous.z(),
                        doorX,
                        doorZ,
                        facing
                );


        /*
         * 玩家当前相对于门的位置。
         */
        double currentSide =
                getSide(
                        player.getX(),
                        player.getZ(),
                        doorX,
                        doorZ,
                        facing
                );


        /*
         * ========================================================
         * 穿过门的核心判断
         * ========================================================
         *
         * 一边 -> 另一边
         *
         * 例如：
         *
         * previousSide < 0
         * currentSide >= 0
         *
         * 或反过来。
         */
        if (!(
                previousSide < 0
                        && currentSide >= 0
                        ||
                        previousSide > 0
                                && currentSide <= 0
        )) {

            return false;
        }


        /*
         * ========================================================
         * 确认玩家确实在门洞范围
         * ========================================================
         */

        double horizontalOffset;

        if (facing == Direction.NORTH
                || facing == Direction.SOUTH) {

            horizontalOffset =
                    Math.abs(
                            player.getX()
                                    - doorX
                    );

        } else {

            horizontalOffset =
                    Math.abs(
                            player.getZ()
                                    - doorZ
                    );
        }


        /*
         * 门宽约 1 格。
         */
        if (horizontalOffset > 0.65D) {
            return false;
        }


        /*
         * 玩家 Y 范围必须经过门。
         */
        double playerY =
                player.getY();

        return playerY >= doorY - 0.1D
                && playerY <= doorY + 2.1D;
    }


    /*
     * ============================================================
     * 计算玩家位于门的哪一侧
     * ============================================================
     */

    private static double getSide(
            double x,
            double z,
            double doorX,
            double doorZ,
            Direction facing
    ) {

        return switch (facing) {

            case NORTH ->
                    z - doorZ;

            case SOUTH ->
                    doorZ - z;

            case WEST ->
                    x - doorX;

            case EAST ->
                    doorX - x;

            default ->
                    0.0D;
        };
    }
}