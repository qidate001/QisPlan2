package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.block.entity.GhostDoorPlateBlockEntity;
import com.qidate.qisplan2.ghost.doorplate.GhostDoorPlateRegistry;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.environment.GhostDoorAccess;
import com.qidate.qisplan2.ghost.module.environment.GhostDoorGeometry;
import com.qidate.qisplan2.ghost.isolation.GhostIsolationBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** 将门的空间操作绑定到世界。载体判断仅存在于此适配器，模块只看到 GhostDoorAccess。 */
public final class GhostDoorWorldAdapter implements GhostDoorAccess {
    /** 弱键避免离线服务器、消失实体或物品成为永久引用；静态门牌另有持久索引。 */
    private static final Map<MinecraftServer, Map<Object, LiveLink>> LIVE = new WeakHashMap<>();
    private record LiveLink(int number, Destination door, long expires) {}
    private final GhostModuleHost host;
    private final ResourceLocation module;
    /** 绑定一次调用的环境，后续方法始终实时读取世界状态。 */
    public GhostDoorWorldAdapter(GhostModuleHost host, ResourceLocation module) { this.host = host; this.module = module; }
    /** 在适配边界内部解析承担位置的实体。 */
    private LivingEntity bearer() { return GhostEnvironmentAdapters.bearer(host); }
    /** 仅接受服务端世界。 */
    private ServerLevel level() {
        var entity = bearer(); var world = entity != null ? entity.level() : host instanceof BlockEntity b ? b.getLevel() : null;
        return world instanceof ServerLevel server ? server : null;
    }
    /** 返回世界锚点，不向模块公开载体。 */
    private BlockPos anchor() { return bearer() != null ? bearer().blockPosition() : host instanceof BlockEntity b ? b.getBlockPos() : null; }
    /** 保证物品必须暴露，移除或隐藏的来源不能操作门。 */
    private boolean exposed() { return level() != null && GhostEnvironmentAdapters.bind(host, module).exposed(); }
    /** 归一化真实门位置，并拒绝未加载区块。 */
    private BlockPos normalize(BlockPos pos) {
        if (pos == null || level() == null || !level().hasChunkAt(pos)) return null;
        var state = level().getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock)) return null;
        return (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos).immutable();
    }
    /** 固定门使用自身锚点；生命位置按面向、距离与门宽筛选最近的门。 */
    public BlockPos frontDoor() {
        if (!exposed()) return null;
        var fixed = bearer() == null ? normalize(anchor()) : null;
        if (fixed != null) return fixed;
        var entity = bearer(); if (entity == null) return null;
        BlockPos best = null; double distance = Double.MAX_VALUE;
        for (var pos : BlockPos.betweenClosed(entity.blockPosition().offset(-2,-1,-2), entity.blockPosition().offset(2,1,2))) {
            var door = normalize(pos); if (door == null) continue;
            var facing = level().getBlockState(door).getValue(DoorBlock.FACING);
            double dx = entity.getX() - door.getX() - .5, dz = entity.getZ() - door.getZ() - .5;
            double normal = dx * facing.getStepX() + dz * facing.getStepZ();
            double lateral = dx * facing.getStepZ() - dz * facing.getStepX();
            double dist = dx * dx + dz * dz;
            if (normal < 0 && normal >= -1.5 && Math.abs(lateral) <= .65
                    && entity.getY() >= door.getY() - .1 && entity.getY() < door.getY() + 2 && dist < distance) {
                best = door; distance = dist;
            }
        }
        return best;
    }
    /** 门牌固定绑定优先；其他来源只使用当前真正站入的门。 */
    public BlockPos linkedDoor() {
        if (!exposed()) return null;
        if (host instanceof GhostDoorPlateBlockEntity plate) return normalize(plate.getLinkedDoorPos());
        var entity = bearer(); if (entity == null) return normalize(anchor());
        for (var pos : BlockPos.betweenClosed(entity.blockPosition().offset(-1,-1,-1), entity.blockPosition().offset(1,1,1))) {
            var door = normalize(pos);
            if (door != null && inOpening(entity, door)) return door;
        }
        return null;
    }
    /** 查询当前门开关状态。 */
    public boolean open(BlockPos pos) {
        var door = normalize(pos); return door != null && level().getBlockState(door).getValue(DoorBlock.OPEN);
    }
    /** 原版 setOpen 负责双格同步、音效和门状态事件。 */
    public boolean close(BlockPos pos) {
        var door = normalize(pos); if (!open(door)) return false;
        var state = level().getBlockState(door);
        ((DoorBlock)state.getBlock()).setOpen(null, level(), state, door, false); return true;
    }
    /** 扫描沿门法线前后各一格的门宽区域，而不是整个三乘三区域。 */
    public List<LivingEntity> nearby(BlockPos pos) {
        var door = normalize(pos); if (door == null) return List.of();
        var f = level().getBlockState(door).getValue(DoorBlock.FACING);
        var box = new AABB(door).expandTowards(0, 1, 0).inflate(Math.abs(f.getStepX()), 0, Math.abs(f.getStepZ()));
        return level().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator());
    }
    /** 直线袭击不被普通墙体截断；隔绝注册表统一决定上下两格的阻断状态。 */
    public List<LivingEntity> attackLine(BlockPos pos, double range) {
        var door = normalize(pos); if (door == null) return List.of();
        var facing = level().getBlockState(door).getValue(DoorBlock.FACING);
        var box = GhostDoorGeometry.attackBounds(door, facing, range,
                block -> !level().hasChunkAt(block)
                        || GhostIsolationBlockRegistry.canBlockSupernatural(level().getBlockState(block)));
        return level().getEntitiesOfClass(LivingEntity.class, box, entity -> entity.isAlive() && !entity.isSpectator());
    }
    /** 门洞宽一格、高两格；必须与该空间相交，不能只在旁边路过。 */
    private boolean inOpening(LivingEntity target, BlockPos pos) {
        return target != null && target.isAlive() && !target.isSpectator() && target.level() == level()
                && target.getBoundingBox().intersects(new AABB(pos).expandTowards(0,1,0));
    }
    /** 冷却使用服务器统一时间并放在目标持久数据，跨维度与其他门牌也共享。 */
    public boolean inside(LivingEntity target, BlockPos pos) {
        return normalize(pos) != null && inOpening(target, pos)
                && target.getPersistentData().getLong("qisplan2DoorCooldown") <= level().getServer().overworld().getGameTime();
    }
    /** 每次尝试保留一秒冷却，防止同 tick 多个模块重复执行。 */
    public void cooldown(LivingEntity target) {
        if (target != null && level() != null) target.getPersistentData().putLong("qisplan2DoorCooldown", level().getServer().overworld().getGameTime() + 20);
    }
    /** 各种载体都可以成为实时门端点。物品用物品栈身份，避免与携带者自身模块覆盖。 */
    public void maintainLink(int number) {
        if (level() == null) return;
        Object key = host instanceof GhostItemModuleHost item ? item.stack() : bearer() != null ? bearer() : host;
        var links = LIVE.computeIfAbsent(level().getServer(), ignored -> new WeakHashMap<>());
        var door = linkedDoor();
        if (door == null || !host.carriesModule(module)) { links.remove(key); return; }
        links.put(key, new LiveLink(number, new Destination(level().dimension(), door, open(door)),
                level().getServer().overworld().getGameTime() + 2));
    }
    /** 校验存档中的门牌并按实际门去重；损坏的门牌不会作为幽灵目的地参与计数。 */
    public List<Destination> destinations(int number, BlockPos origin) {
        if (level() == null || origin == null) return List.of();
        var result = new LinkedHashMap<GhostDoorPlateRegistry.DoorLocation, Destination>();
        for (var location : Set.copyOf(GhostDoorPlateRegistry.getLocations(level(), number))) {
            var world = level().getServer().getLevel(location.dimension()); if (world == null) continue;
            // 仅加载已登记门牌所在区块，以确认远处目的门，而不扩大扫描范围。
            world.getChunkAt(location.pos());
            if (!(world.getBlockEntity(location.pos()) instanceof GhostDoorPlateBlockEntity plate)
                    || !plate.carriesModule(module) || plate.getNumber() != number) {
                GhostDoorPlateRegistry.unregisterPosition(world, location.pos()); continue;
            }
            var door = plate.getLinkedDoorPos(); if (door == null) continue;
            var state = world.getBlockState(door); if (!(state.getBlock() instanceof DoorBlock)) continue;
            if (world == level() && door.equals(origin)) continue;
            result.put(new GhostDoorPlateRegistry.DoorLocation(world.dimension(), door),
                    new Destination(world.dimension(), door, state.getValue(DoorBlock.OPEN)));
        }
        var live = LIVE.get(level().getServer());
        if (live != null) {
            long now = level().getServer().overworld().getGameTime();
            live.values().removeIf(link -> link.expires() < now);
            for (var link : live.values()) {
                if (link.number() != number) continue;
                var point = link.door(); var world = level().getServer().getLevel(point.dimension());
                if (world == null || !world.hasChunkAt(point.pos()) || world == level() && point.pos().equals(origin)) continue;
                var state = world.getBlockState(point.pos());
                if (!(state.getBlock() instanceof DoorBlock)) continue;
                result.put(new GhostDoorPlateRegistry.DoorLocation(point.dimension(), point.pos()),
                        new Destination(point.dimension(), point.pos(), state.getValue(DoorBlock.OPEN)));
            }
        }
        return List.copyOf(result.values());
    }
    /** 传送前重查目的门；关闭门不能被旧快照误当成正常出口。 */
    public boolean travel(LivingEntity target, Destination destination, boolean voidTravel) {
        if (level() == null || target == null || !target.isAlive() || target.level() != level()) return false;
        var world = level().getServer().getLevel(destination.dimension()); if (world == null) return false;
        var state = world.getBlockState(destination.pos());
        if (!(state.getBlock() instanceof DoorBlock) || state.getValue(DoorBlock.OPEN) == voidTravel) return false;
        float yaw = target.getYRot();
        Vec3 velocity = Vec3.ZERO;
        Vec3 arrival = new Vec3(destination.pos().getX()+.5, world.getMinBuildHeight()-64, destination.pos().getZ()+.5);
        if (!voidTravel) {
            var origin = linkedDoor(); if (origin == null) return false;
            var originFacing = level().getBlockState(origin).getValue(DoorBlock.FACING);
            var destinationFacing = state.getValue(DoorBlock.FACING);
            float rotation = GhostDoorGeometry.rotation(originFacing, destinationFacing);
            yaw += rotation;
            velocity = GhostDoorGeometry.rotate(target.getDeltaMovement(), rotation);
            arrival = GhostDoorGeometry.exitPosition(destination.pos(), originFacing, destinationFacing,
                    target.getDeltaMovement(), target.getLookAngle());
        }
        if (target instanceof ServerPlayer player) {
            player.teleportTo(world, arrival.x, arrival.y, arrival.z, yaw, player.getXRot());
            player.setDeltaMovement(velocity);
            player.hurtMarked = true; return true;
        }
        if (target.level() == world) {
            target.teleportTo(arrival.x, arrival.y, arrival.z);
            target.setYRot(yaw); target.setYHeadRot(yaw); target.setYBodyRot(yaw);
            target.setDeltaMovement(velocity); target.hurtMarked = true; return true;
        }
        return target.changeDimension(new DimensionTransition(world, arrival, velocity,
                yaw, target.getXRot(), DimensionTransition.DO_NOTHING)) != null;
    }
}
