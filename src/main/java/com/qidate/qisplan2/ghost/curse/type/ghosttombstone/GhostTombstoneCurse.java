package com.qidate.qisplan2.ghost.curse.type.ghosttombstone;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModTags;
import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.UUID;

/**
 * 鬼墓碑诅咒。
 *
 * 当前能力：
 *
 * 当诅咒目标站在自然土质方块上时，
 * 会被逐渐拉入地下。
 *
 * 当玩家整个身体都进入土壤之后，
 * 会被传送到诅咒来源墓碑的下方。
 */
public class GhostTombstoneCurse implements Curse {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "ghost_tombstone"
            );

    /**
     * 玩家每 Tick 下沉的距离。
     */
    private static final double SINK_SPEED = 0.08D;

    /**
     * 传送后暂时无法再次触发下沉的 Tick 数。
     *
     * 防止玩家传送到墓碑下方后，
     * 因为那里同样是土壤而立即再次触发。
     */
    private static final int TELEPORT_COOLDOWN_TICKS = 40;

    private final UUID id;
    private final UUID target;
    private final GhostTombstoneCurseSource source;

    private int strength;

    /**
     * 传送冷却。
     *
     * 这是运行时状态，不参与持久化。
     */
    private int teleportCooldown;

    public GhostTombstoneCurse(
            UUID target,
            GhostTombstoneCurseSource source,
            int strength
    ) {
        this(
                UUID.randomUUID(),
                target,
                source,
                strength
        );
    }

    public GhostTombstoneCurse(
            UUID id,
            UUID target,
            GhostTombstoneCurseSource source,
            int strength
    ) {
        this.id = id;
        this.target = target;
        this.source = source;
        this.strength = strength;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public ResourceLocation getType() {
        return ID;
    }

    @Override
    public UUID getTarget() {
        return target;
    }

    @Override
    public CurseSource getSource() {
        return source;
    }

    public int getStrength() {
        return strength;
    }

    /**
     * 每 Tick 执行鬼墓碑诅咒。
     */
    @Override
    public void tick(
            MinecraftServer server
    ) {

        /*
         * 找到诅咒目标。
         */
        ServerPlayer player =
                server.getPlayerList()
                        .getPlayer(target);

        /*
         * 玩家不在线时不处理。
         */
        if (player == null) {
            return;
        }

        /*
         * 传送冷却期间不触发。
         */
        if (teleportCooldown > 0) {
            teleportCooldown--;
            return;
        }

        /*
         * 玩家脚下不是自然土质，
         * 不会被拉入地下。
         */
        BlockPos below =
                BlockPos.containing(
                        player.getX(),
                        player.getY() - 0.05D,
                        player.getZ()
                );

        BlockState belowState =
                player.level()
                        .getBlockState(below);

        /*
        QisPlan2.LOGGER.info(
                "[鬼墓碑诅咒] 目标 {} 脚下方块：{}，Y：{}，玩家Y：{}",
                player.getGameProfile().getName(),
                belowState.getBlock(),
                below.getY(),
                player.getY()
        );

        QisPlan2.LOGGER.info(
                "[鬼墓碑诅咒] 方块状态 {}",
                isNaturalBurialBlock(belowState)
        );
        */

        if (!isNaturalBurialBlock(belowState)) {
            player.noPhysics = false;
            return;
        }

        /*
         * 玩家已经完全进入土壤。
         *
         * 先判断，再继续下沉，
         * 避免已经完成埋入后继续移动。
         */
        if (isCompletelyBuried(player)) {

            player.noPhysics = false;

            teleportIntoTomb(
                    server,
                    player
            );

            return;
        }

        /*
         * 开始向地下下沉。
         */
        player.noPhysics = true;

        player.connection.teleport(
                player.getX(),
                player.getY() - SINK_SPEED,
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
    }

    /**
     * 判断一个方块是否属于可以被诅咒拉入的自然土质。
     */
    private static boolean isNaturalBurialBlock(
            BlockState state
    ) {
        return state.is(
                ModTags.Blocks.GHOST_TOMBSTONE_BURIAL
        );
    }

    /**
     * 判断玩家是否已经完全埋入土中。
     *
     * 使用玩家碰撞箱判断。
     *
     * 只要碰撞箱仍然有一部分位于空气等非土质方块中，
     * 就不算完全埋入。
     */
    private static boolean isCompletelyBuried(
            ServerPlayer player
    ) {

        AABB box =
                player.getBoundingBox();

        int minX =
                BlockPos.containing(
                        box.minX,
                        0,
                        0
                ).getX();

        int maxX =
                BlockPos.containing(
                        box.maxX - 0.0001D,
                        0,
                        0
                ).getX();

        int minY =
                BlockPos.containing(
                        0,
                        box.minY,
                        0
                ).getY();

        int maxY =
                BlockPos.containing(
                        0,
                        box.maxY - 0.0001D,
                        0
                ).getY();

        int minZ =
                BlockPos.containing(
                        0,
                        0,
                        box.minZ
                ).getZ();

        int maxZ =
                BlockPos.containing(
                        0,
                        0,
                        box.maxZ - 0.0001D
                ).getZ();

        /*
         * 检查整个碰撞箱覆盖到的所有方块。
         *
         * 只要其中有一个不是自然土质，
         * 就说明玩家还没有完全被土埋住。
         */
        for (int x = minX; x <= maxX; x++) {

            for (int y = minY; y <= maxY; y++) {

                for (int z = minZ; z <= maxZ; z++) {

                    BlockState state =
                            player.level()
                                    .getBlockState(
                                            new BlockPos(
                                                    x,
                                                    y,
                                                    z
                                            )
                                    );

                    if (!isNaturalBurialBlock(state)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * 将玩家传送到鬼墓碑正下方的土中。
     */
    private void teleportIntoTomb(
            MinecraftServer server,
            ServerPlayer player
    ) {

        /*
         * 获取墓碑所在维度。
         */
        ServerLevel targetLevel =
                server.getLevel(
                        source.dimension()
                );

        if (targetLevel == null) {
            return;
        }

        BlockPos tombstonePos =
                source.pos();

        /*
         * 鬼墓碑是两格高。
         *
         * tombstonePos 为下半部分，
         * 因此墓碑正下方就是 Y - 1。
         *
         * 将玩家放进墓碑下面的土中。
         */
        double x =
                tombstonePos.getX() + 0.5D;

        double y =
                tombstonePos.getY() - 1.0D + 0.05D;

        double z =
                tombstonePos.getZ() + 0.5D;

        /*
         * 跨维度 / 同维度统一使用 ServerPlayer.teleportTo。
         */
        player.teleportTo(
                targetLevel,
                x,
                y,
                z,
                player.getYRot(),
                player.getXRot()
        );

        /*
         * 设置传送冷却。
         */
        teleportCooldown =
                TELEPORT_COOLDOWN_TICKS;
    }

    @Override
    public boolean isValid(
            MinecraftServer server
    ) {
        return true;
    }

    @Override
    public CompoundTag save() {

        CompoundTag tag =
                new CompoundTag();

        tag.putUUID(
                "Id",
                id
        );

        tag.putUUID(
                "Target",
                target
        );

        tag.putString(
                "SourceType",
                source.getType().toString()
        );

        tag.put(
                "Source",
                source.save()
        );

        tag.putInt(
                "Strength",
                strength
        );

        return tag;
    }
}