package com.qidate.qisplan2.ghost.module.host;


import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.qidate.qisplan2.ghost.module.implementation.mist.GhostMistModule;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import com.qidate.qisplan2.ghost.possession.ability.ghostdoor.GhostDoorAbilityHandler;
import com.qidate.qisplan2.ghost.possession.ability.ghostdoor.GhostDoorMarkManager;
import com.qidate.qisplan2.ghost.module.environment.GhostDomainAccess;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.core.ModGameRules;
import com.qidate.qisplan2.ghost.module.environment.BlockSpreadSpec;
import com.qidate.qisplan2.ghost.module.implementation.GhostCarpetModule;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.GhostAvoidanceSystem;
import com.qidate.qisplan2.ghost.domain.*;
import com.qidate.qisplan2.ghost.domain.type.eye.GhostEyeDomainController;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleContext;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.ghost.domain.type.mist.GhostMistDomainController;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.environment.GhostWorldAccess;
import com.qidate.qisplan2.ghost.module.implementation.calling.CallingGhostSounds;
import com.qidate.qisplan2.ghost.module.implementation.calling.CallingGhostTargets;
import com.qidate.qisplan2.ghost.module.implementation.knocking.KnockingGhostDoorSystem;
import com.qidate.qisplan2.ghost.module.implementation.nightwanderer.NightWandererBehavior;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** 把各类物理载体转换成统一世界操作；这里是唯一允许处理载体差异的边界之一。 */
public final class GhostWorldAdapters {
    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
    private GhostWorldAdapters() {}

    /**
     * 为指定载体与模块创建世界操作接口。
     * 内部可以识别实体、玩家、物品和方块，但返回接口不提供取回载体的方法。
     */
    public static GhostWorldAccess bind(GhostModuleHost host, ResourceLocation id) {
        return new GhostWorldAccess() {
            /** 返回服务端世界时间。 */
            public long gameTime() { return level() == null ? 0 : level().getGameTime(); }
            /** 将门上半部归一化为下半部。 */
            public BlockPos normalizeDoor(BlockPos pos) {
                if (pos == null || level() == null) return null;
                var state = level().getBlockState(pos);
                return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                        && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            }
            /** 实际声音传播范围由敲门系统决定。 */
            public int hearingTargets(BlockPos pos) {
                return level() instanceof ServerLevel server && source() != null && pos != null
                        ? KnockingGhostDoorSystem.countHearingTargets(server, source(), pos) : 0;
            }
            /** 查询特殊门世界状态。 */
            public boolean supernaturalDoor(BlockPos pos) { return level() != null && pos != null && KnockingGhostDoorSystem.isGhostDoor(level(), pos); }
            /** 普通方块操作经过原世界销毁入口。 */
            public void destroyBlock(BlockPos pos) { if (level() instanceof ServerLevel server && pos != null) server.destroyBlock(pos, false); }

            /** 鬼域接口绑定当前模块，只提供基础读写操作。 */
            public GhostDomainAccess domain() { return GhostDomainAdapters.bind(source(), id); }

            /** 通过服务器视线解析目标，墙体和距离均由世界实际状态决定。 */
            public LivingEntity aimedTarget(double range) {
                var origin = source();
                if (!present() || origin == null || range <= 0 || range > 128) return null;
                var start = origin.getEyePosition();
                var end = start.add(origin.getLookAngle().scale(range));
                LivingEntity best = null;
                double nearest = range * range;
                for (var entity : level().getEntitiesOfClass(LivingEntity.class,
                        origin.getBoundingBox().expandTowards(origin.getLookAngle().scale(range)).inflate(1),
                        e -> e != origin && e.isAlive() && !e.isSpectator())) {
                    var hit = entity.getBoundingBox().inflate(0.1).clip(start, end);
                    if (hit.isPresent() && origin.hasLineOfSight(entity) && start.distanceToSqr(hit.get()) < nearest) {
                        nearest = start.distanceToSqr(hit.get()); best = entity;
                    }
                }
                return best;
            }

            /** 将输入映射为已加载的方块位置，不接受客户端传来的坐标。 */
            public BlockPos aimedBlock(double range) {
                if (!present() || source() == null || range <= 0 || range > 128) return null;
                var hit = source().pick(range, 0, false);
                return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                        && level().hasChunkAt(block.getBlockPos()) ? block.getBlockPos() : null;
            }

            /** 当前物理来源与目标包围盒接触时才满足接触媒介。 */
            public boolean touching(LivingEntity target) {
                return present() && source() != null && target != null && target != source()
                        && sameWorld(target.level()) && source().getBoundingBox().inflate(0.1).intersects(target.getBoundingBox());
            }

            /** 已存在标记的检索是世界事实，技能是否使用它由模块决定。 */
            public LivingEntity doorMarkedTarget(boolean opening) {
                if (!(source() instanceof ServerPlayer player)) return null;
                return GhostDoorMarkManager.findNearestMarkedTarget(player, player.serverLevel(), e -> opening
                        ? GhostDoorMarkManager.hasOpeningMark(player, e.getUUID())
                        : GhostDoorMarkManager.hasClosingMark(player, e.getUUID()));
            }

            /** 复用碰撞安全位置搜索，实际移动不包含袭击或技能逻辑。 */
            public boolean approach(LivingEntity target) {
                if (!canMove() || target == null || !(level() instanceof ServerLevel server)) return false;
                var position = GhostDoorAbilityHandler.findApproachPosition(server, target);
                if (position == null) return false;
                teleport(position.getX() + 0.5, position.getY(), position.getZ() + 0.5, target);
                return true;
            }

            /** 完成本次追袭后清理标记。 */
            public void clearDoorMarks(LivingEntity target) {
                if (source() instanceof ServerPlayer player && target != null)
                    GhostDoorMarkManager.clearMarks(player, target.getUUID());
            }

            /**
             * 永久损失沿用传入的属性标识，兼容旧存档已支付的代价。
             * 扣除前检查预算；属性更新后同步当前生命上限并播放抵挡反馈。
             */
            public boolean consumeMaxHealth(ResourceLocation lossId, double cost, double minimum) {
                var entity = source();
                if (entity == null || !(entity.level() instanceof ServerLevel) || !entity.isAlive()
                        || !Double.isFinite(cost) || cost <= 0 || !Double.isFinite(minimum) || minimum < 1
                        || entity.getMaxHealth() - cost < minimum) return false;
                var attribute = entity.getAttribute(Attributes.MAX_HEALTH);
                if (attribute == null) return false;
                var old = attribute.getModifier(lossId);
                double amount = (old == null ? 0 : old.amount()) - cost;
                attribute.addOrReplacePermanentModifier(new AttributeModifier(
                        lossId, amount, AttributeModifier.Operation.ADD_VALUE));
                entity.setHealth(Math.min(entity.getHealth(), entity.getMaxHealth()));
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL, 1, 1);
                return true;
            }

            /** 执行持续侵蚀；与旧寿衣相同，扣除当前生命而非最大生命。 */
            public boolean drainHealth(float amount) {
                var entity = source();
                if (entity == null || !(entity.level() instanceof ServerLevel) || !entity.isAlive()
                        || !Float.isFinite(amount) || amount <= 0) return false;
                entity.setHealth(Math.max(0, entity.getHealth() - amount));
                return true;
            }

            /** 保留旧世界的鬼地毯时间设置；默认 300 tick，异常非正值至少按一 tick 处理。 */
            public int contactIntervalTicks(LivingEntity target, int fallback) {
                return id.equals(GhostCarpetModule.ID)
                        ? Math.max(1, target.level().getGameRules().getInt(ModGameRules.GHOST_CARPET_KILL_TIME))
                        : fallback;
            }

            /** 接触袭击表现由中间层落实，不向模块暴露来源载体。 */
            public void contactAttackFeedback(LivingEntity target) { GhostCarpetWorldEffects.feedback(target); }

            /** 使用调用参数提供的世界位置和蔓延策略，不检查来源载体或具体模块种类。 */
            public int spreadBlocks(LivingEntity anchor, BlockSpreadSpec spec) {
                if (!(anchor.level() instanceof ServerLevel server)) return 0;
                return BlockSpreadWorldAdapter.spread(server, anchor.blockPosition(), spec);
            }

            /**
             * 在世界适配器内部读取生命实体来源。
             * 可能返回 null；该对象不得通过模块可见接口向外返回。
             */
            private LivingEntity source() { return GhostEnvironmentAdapters.bearer(host); }

            /**
             * 解析当前来源的世界；生命实体和方块均不存在有效世界时返回 null。
             */
            private Level level() {
                var source = source();
                return source != null ? source.level() : host instanceof BlockEntity b ? b.getLevel() : null;
            }

            /**
             * 判断当前来源是否位于服务端世界且满足暴露条件。
             */
            public boolean present() {
                return level() instanceof ServerLevel && GhostEnvironmentAdapters.bind(host, id).exposed();
            }

            /**
             * 判断事件中的实体是否对应当前能力来源。
             * 只返回关联关系，不把来源对象交给模块。
             */
            public boolean isOrigin(Entity entity) { return entity != null && entity == source(); }

            /**
             * 判断给定世界是否与当前来源处于同一世界实例，防止跨维度作用。
             */
            public boolean sameWorld(Level other) { return other != null && level() == other; }

            /**
             * 查询当前环境是否为白天；当前实现缺少世界时按白天处理。
             */
            public boolean isDay() { return level() == null || level().isDay(); }

            /**
             * 读取当前环境位置，用于距离和移动计算。
             * 缺少位置时返回零向量；调用方应先检查环境是否有效。
             */
            public Vec3 position() {
                var source = source();
                return source != null ? source.position() : host instanceof BlockEntity b
                        ? Vec3.atCenterOf(b.getBlockPos()) : Vec3.ZERO;
            }

            /**
             * 判断当前环境是否能够接受移动操作。
             * 不具备移动出口时，模块不能假定瞬移或速度设置会产生效果。
             */
            public boolean canMove() { return present() && source() != null; }

            /**
             * 将可移动来源传送到指定坐标。
             * 有自主 AI 时同步朝向目标、停止旧寻路并清除旧攻击目标。
             */
            public void teleport(double x, double y, double z, LivingEntity lookAt) {
                if (!canMove()) return;
                source().teleportTo(x,y,z);
                if (source() instanceof Mob mob) {
                    mob.getLookControl().setLookAt(lookAt,30,30);
                    mob.getNavigation().stop();
                    mob.setTarget(null);
                    mob.setAggressive(false);
                }
            }

            /**
             * 设置当前可移动来源的速度和朝向。
             * 不具备移动能力时不修改世界状态。
             */
            public void move(Vec3 velocity, float yaw) {
                if (!canMove()) return;
                source().setDeltaMovement(velocity);
                source().setYRot(yaw);
                source().setYBodyRot(yaw);
            }

            /**
             * 停止可用的导航并播放主手挥动动作，供袭击能力表现使用。
             */
            public void stopAndSwing() {
                if (source() instanceof Mob mob) mob.getNavigation().stop();
                if (source() != null) source().swing(InteractionHand.MAIN_HAND);
            }

            /**
             * 检查目标存活、维度、来源排除及环境屏蔽等目标资格。
             * 不合格目标不能继续进入追踪或袭击媒介。
             */
            public boolean validTarget(LivingEntity target) {
                return present() && target != null && !isOrigin(target) && target.isAlive()
                        && !target.isRemoved() && sameWorld(target.level())
                        && !(target instanceof Player p && (p.isCreative() || p.isSpectator()))
                        && !GhostAvoidanceSystem.hasWhiteGhostCandle(target);
            }

            /**
             * 从本模块 customData 中保存的目标 UUID 恢复生命实体。
             * 目标不存在或已失效时返回 null，不创建第二份持久目标状态。
             */
            public LivingEntity target() {
                var data = new GhostModuleContext(host,id).getCustomData();
                if (!(level() instanceof ServerLevel server) || !data.hasUUID("target")) return null;
                var entity = server.getEntity(data.getUUID("target"));
                return entity instanceof LivingEntity living && validTarget(living) ? living : null;
            }

            /**
             * 校验目标资格与空间阻隔，判断是否允许继续追踪。
             */
            public boolean canTrack(LivingEntity target) {
                return validTarget(target) && CallingGhostTargets.canTrack(level(), position(), target);
            }

            /**
             * 推进已有夜游鬼猎杀行为的世界操作。
             * 当前适配器仅在具备模块化实体行动出口时驱动该行为。
             */
            public void tickHunt() {
                if (source() instanceof ModularGhostEntity ghost) NightWandererBehavior.tickActivity(ghost);
            }

            /**
             * 使用指定伤害类型和强度尝试灵异击杀。
             * 先检查目标世界，再交给统一死亡处理；返回实际击杀是否成功。
             */
            public boolean attack(LivingEntity target, ResourceKey<DamageType> type, double intensity) {
                // 静态来源没有实体形体；由目标所在的服务端世界提供伤害注册表。
                // 有已知世界时仍严格检查维度，不能跨维度袭击。
                if (target == null || !(target.level() instanceof ServerLevel)
                        || (level() != null && !sameWorld(target.level()))) return false;
                var damage = new DamageSource(target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(type), source());
                return SupernaturalDeathHandler.tryKill(target, damage, intensity);
            }

            /**
             * 判断指定位置是否满足敲门系统认可的门条件。
             */
            public boolean isDoor(BlockPos pos) { return present() && KnockingGhostDoorSystem.isDoor(level(),pos); }

            /**
             * 发起敲门的世界操作，声音传播与后续听声处理由敲门系统负责。
             */
            public void knock(BlockPos pos, double intensity) {
                if (present() && source() != null) KnockingGhostDoorSystem.knock((ServerLevel)level(),source(),pos,intensity);
            }

            /**
             * 将已准备好的袭击排入敲门延迟队列。
             * 延迟任务执行时仍由准备好的能力调用重新检查有效性，不制造请求型事件。
             */
            public void scheduleKnockAttack(Runnable effect) { KnockingGhostDoorSystem.scheduleModuleAttack(effect); }

            /**
             * 检查目标是否存在可播放的喊名声音；没有对应声音时返回 false。
             */
            public boolean hasCall(LivingEntity target) {
                return target instanceof ServerPlayer player && CallingGhostSounds.getSound(player) != null;
            }

            /**
             * 在目标位置播放可用的喊名声音。
             * 目标不受支持或没有声音资源时不播放。
             */
            public void playCall(LivingEntity target) {
                if (level() != null && target instanceof ServerPlayer player) {
                    var sound = CallingGhostSounds.getSound(player);
                    if (sound != null) level().playSound(null,player.getX(),player.getY(),player.getZ(),sound,SoundSource.HOSTILE,1,1);
                }
            }

            /**
             * 判断当前适配器是否提供鬼域维护出口。
             * 当前实现支持模块化实体和玩家模块载体，其他载体返回 false。
             */
            public boolean canMaintainDomain() {
                return present() && (host instanceof ModularGhostEntity || host instanceof PlayerGhostModuleHost);
            }

            /**
             * 判断当前适配器是否提供鬼域开关出口。
             * 当前实现要求有效的服务端玩家模块载体。
             */
            public boolean canToggleDomain() {
                return present() && host instanceof PlayerGhostModuleHost && source() instanceof ServerPlayer;
            }

            /**
             * 通过已有鬼眼控制器切换鬼域开关。
             * 不具备出口时返回 false；开启后返回实际开启状态，关闭成功返回 true。
             */
            public boolean toggleDomain() {
                if (!canToggleDomain()) return false;
                var player = (ServerPlayer)source();
                if (id.equals(GhostMistModule.ID)) {
                    boolean before = GhostMistDomainController.isOpen(player);
                    GhostMistDomainController.toggle(player);
                    return before != GhostMistDomainController.isOpen(player);
                }
                if (GhostEyeDomainController.isOpen(player)) {
                    GhostEyeDomainController.close(player);
                    return true;
                }
                GhostEyeDomainController.open(player);
                return GhostEyeDomainController.isOpen(player);
            }

            /**
             * 维护鬼雾运行时鬼域。
             * 实体来源创建或保持对应鬼域，玩家来源沿用持久开关状态驱动控制器。
             */
            public void maintainDomain() {
                if (source() instanceof ModularGhostEntity ghost)
                    GhostMistDomainController.createDomain((ServerLevel)level(),ghost);
                else if (host instanceof PlayerGhostModuleHost && source() instanceof ServerPlayer player)
                    GhostMistDomainController.tick(new PlayerGhostModuleContext(player,id,PossessionHandler.getData(player,id)));
            }

            /**
             * 按来源 UUID 和模块类型查询当前运行时鬼域。
             * 缺少世界、来源或对应鬼域时返回 null。
             */
            private GhostDomain currentDomain() {
                return level() instanceof ServerLevel server && source() != null
                        ? GhostDomainManager.get(server).getBySourceAndType(source().getUUID(),id) : null;
            }

            /**
             * 检查目标是否仍属于指定鬼域的有效覆盖集合。
             * 同时验证目标资格和鬼域 ID，避免旧曝光事实作用于已更换的鬼域。
             */
            public boolean containsInDomain(UUID domainId, LivingEntity target) {
                var domain = currentDomain();
                return validTarget(target) && domain != null && domain.getId().equals(domainId)
                        && GhostDomainEntityTracker.get((ServerLevel)level()).getEffectiveEntities(domain).contains(target);
            }

            /**
             * 在当前鬼域存在时委托控制器结算鬼雾成长。
             * 调用方应仅在袭击实际成功后调用。
             */
            public void growDomain() {
                var domain = currentDomain();
                if (domain == null) return;
                if (source() instanceof ModularGhostEntity ghost) GhostMistDomainController.grow((ServerLevel)level(),ghost,domain);
                else if (source() instanceof ServerPlayer player) GhostMistDomainController.grow(player,domain);
            }

            /**
             * 检查适配器是否提供题字能力出口。
             */
            public boolean canInscribe() { return host instanceof GhostInscriptionCapability; }

            /**
             * 把题字操作交给具备题字能力的载体，传入实际交互者。
             * 不支持题字时不执行操作。
             */
            public void inscribe(LivingEntity user) {
                if (host instanceof GhostInscriptionCapability capability) capability.requestInscription(user);
            }
        };
    }
}
