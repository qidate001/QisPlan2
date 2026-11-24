package com.qidate.qisplan2.ghost.module.host;

import java.util.function.BooleanSupplier;
import java.nio.charset.StandardCharsets;

import net.minecraft.world.level.GameRules;

import com.qidate.qisplan2.ghost.module.event.DoorKnockHeardEvent;

import java.util.function.Consumer;
import net.minecraft.world.level.LightLayer;

import com.qidate.qisplan2.ghost.module.environment.GhostAttachmentAccess;
import com.qidate.qisplan2.ghost.module.host.attachment.GhostAttachments;
import com.qidate.qisplan2.core.ModTags;
import com.qidate.qisplan2.ghost.module.environment.GhostDeathCurseAccess;
import com.qidate.qisplan2.network.GhostScreenNetwork;
import com.qidate.qisplan2.ghost.module.environment.GhostWorkstationAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import java.util.List;
import java.util.function.Predicate;
import com.qidate.qisplan2.ghost.module.environment.GhostDoorAccess;

import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import com.qidate.qisplan2.ghost.module.environment.GhostDomainAccess;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.module.environment.BlockSpreadSpec;
import com.qidate.qisplan2.entity.AnimatedModularGhostEntity;
import com.qidate.qisplan2.ghost.GhostAvoidanceSystem;
import com.qidate.qisplan2.ghost.domain.*;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.environment.GhostWorldAccess;
import com.qidate.qisplan2.ghost.module.implementation.calling.CallingGhostSounds;
import com.qidate.qisplan2.ghost.module.implementation.calling.CallingGhostTargets;
import com.qidate.qisplan2.ghost.module.implementation.door.knock.KnockingGhostDoorSystem;
import com.qidate.qisplan2.ghost.module.environment.GhostActor;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.Mth;
import com.qidate.qisplan2.ghost.module.environment.GhostSight;

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
            public GhostActor actor() { return GhostActorAdapter.bind(host,id); }
            /** 坐标转换留在边界，实体以眼睛为旋转原点，静态宿主以位置为原点。 */
            public boolean facePosition(Vec3 target) {
                if(target==null) return false;
                return faceDirection(target.subtract(source()==null ? position() : source().getEyePosition()));
            }
            /** 统一朝向操作；物品不能借用携带者的身体强制转头。 */
            public boolean faceDirection(Vec3 direction) {
                if(host instanceof GhostItemModuleHost || !present() || direction==null
                        || !Double.isFinite(direction.x) || !Double.isFinite(direction.y) || !Double.isFinite(direction.z)
                        || direction.lengthSqr()<1.0E-12) return false;
                if(host instanceof BlockEntity block) {
                    var state=block.getBlockState();
                    var facing=Direction.getNearest(direction.x,direction.y,direction.z);
                    if(state.hasProperty(BlockStateProperties.FACING))
                        state=state.setValue(BlockStateProperties.FACING,facing);
                    else if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                        if(direction.horizontalDistanceSqr()<1.0E-12) return false;
                        state=state.setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.getNearest(direction.x,0,direction.z));
                    } else return false;
                    return block.getLevel().setBlock(block.getBlockPos(),state,3);
                }
                var entity=source();
                if(entity==null) return false;
                float yaw=direction.horizontalDistanceSqr()<1.0E-12 ? entity.getYRot()
                        : (float)(Math.toDegrees(Math.atan2(direction.z,direction.x))-90);
                float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
                if(entity instanceof ServerPlayer player)
                    player.connection.teleport(player.getX(),player.getY(),player.getZ(),yaw,pitch);
                entity.setYRot(yaw);entity.setXRot(pitch);entity.setYHeadRot(yaw);entity.setYBodyRot(yaw);
                if(entity instanceof Mob mob) mob.getLookControl().setLookAt(entity.getEyePosition().add(direction));
                return true;
            }

            /** 插入来源由专用适配器解释，模块只得到附着能力。 */
            @Override public GhostAttachmentAccess attachments() {
                return GhostAttachments.bind(host);
            }

            /** 返回诅咒状态适配器，不将来源实体暴露给模块。 */
            public GhostDeathCurseAccess deathCurses() { return new GhostDeathCurseAdapter(host,id); }
            /** 读者必须处于相同世界、八格内且来源暴露；不使用袭击目标过滤排除本人。 */
            public boolean canOpenScreen(ResourceLocation screen,LivingEntity user) {
                return GhostScreenNetwork.supports(screen) && present() && user instanceof ServerPlayer player
                        && player.isAlive() && !player.isSpectator() && sameWorld(player.level())
                        && player.distanceToSqr(position())<=64;
            }
            /** 只向已验证读者发送界面标识，客户端自行读取其配置。 */
            public boolean openScreen(ResourceLocation screen,LivingEntity user) {
                if (!canOpenScreen(screen,user)) return false;
                GhostScreenNetwork.open((ServerPlayer)user,screen); return true;
            }

            /** AABB 使用世界实体分区索引；空环境不进行查询。 */
            public List<LivingEntity> livingEntities(AABB bounds) {
                return present() ? level().getEntitiesOfClass(LivingEntity.class, bounds.move(position()), this::validTarget) : List.of();
            }

            /** 延迟执行重新校验实时位置与目标资格，防止离开范围后仍被袭击。 */
            public boolean inArea(LivingEntity target, AABB bounds) {
                return validTarget(target) && bounds.move(position()).intersects(target.getBoundingBox());
            }

            /** 玩家检查完整背包，普通生物检查装备；有 Container 的实体额外检查其容器。 */
            public boolean hasCarriedItem(LivingEntity target, Predicate<ItemStack> condition) {
                if (target == null || !target.isAlive() || !sameWorld(target.level())) return false;
                for (var stack : target.getAllSlots()) if (!stack.isEmpty() && condition.test(stack)) return true;
                if (target instanceof Player player) {
                    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                        var stack = player.getInventory().getItem(slot);
                        if (!stack.isEmpty() && condition.test(stack)) return true;
                    }
                } else if (target instanceof Container inventory) {
                    for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                        var stack = inventory.getItem(slot);
                        if (!stack.isEmpty() && condition.test(stack)) return true;
                    }
                }
                return false;
            }

            /** 工作台服务独立适配不同载体，不向模块公开菜单实现。 */
            public GhostWorkstationAccess workstations() { return GhostWorkstationAdapters.bind(host, id); }

            /** 表现参数来自能力定义，实际发包与声音播放留在适配层。 */
            public void attackFeedback(LivingEntity target, SimpleParticleType particle, SoundEvent sound) {
                if (target == null || !(target.level() instanceof ServerLevel server)) return;
                server.sendParticles(particle, target.getX(), target.getY()+.5, target.getZ(),20,.4,.5,.4,.05);
                server.playSound(null,target.blockPosition(),sound,SoundSource.BLOCKS,1,.8f);
            }

            /** 门操作由独立的世界适配器负责。 */
            public GhostDoorAccess doors() { return new GhostDoorWorldAdapter(host, id); }

            /** 服务端使用前方 120 度视锥及真实遮挡；不依赖客户端画面设置。 */
            public boolean sourceVisibleTo(LivingEntity observer) {
                var origin = source();
                if (!validTarget(observer) || origin == null || !observer.hasLineOfSight(origin)) return false;
                if (observer instanceof Player player && origin.isInvisibleTo(player)) return false;
                return GhostSight.inView(observer.getLookAngle(), origin.getEyePosition().subtract(observer.getEyePosition()));
            }

            /** 按实际来源计算水平和垂直角度；玩家视角通过连接同步，生物同步头部朝向。 */
            public float turnTargetTowardSource(LivingEntity target, float maximumDegrees) {
                if (!canHearCall(target) || source() == null || !Float.isFinite(maximumDegrees) || maximumDegrees <= 0) return 0;
                Vec3 offset = source().getEyePosition().subtract(target.getEyePosition());
                float yaw = (float)(Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90);
                float pitch = (float)-Math.toDegrees(Math.atan2(offset.y, offset.horizontalDistance()));
                float dy = Mth.wrapDegrees(yaw - target.getYRot());
                float dp = pitch - target.getXRot();
                float distance = Math.max(Math.abs(dy), Math.abs(dp));
                float ratio = distance == 0 ? 0 : Math.min(1, maximumDegrees / distance);
                float nextYaw = target.getYRot() + dy * ratio;
                float nextPitch = Mth.clamp(target.getXRot() + dp * ratio, -90, 90);
                if (target instanceof ServerPlayer player)
                    player.connection.teleport(player.getX(), player.getY(), player.getZ(), nextYaw, nextPitch);
                else { target.setYRot(nextYaw); target.setXRot(nextPitch); target.setYHeadRot(nextYaw); target.setYBodyRot(nextYaw); }
                return Math.min(distance, maximumDegrees);
            }

            /** 标签与白烛屏蔽决定是否听见，不让模块识别具体宿主。 */
            public boolean canHearCall(LivingEntity target) {
                return validTarget(target) && !target.getType().is(ModTags.NO_HEARING)
                        && !GhostAvoidanceSystem.hasWhiteGhostCandle(target);
            }
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
            public GhostDomainAccess domain() { return GhostDomainAdapters.bind(host, id); }

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
                if (!present() || target == null || target == source() || !sameWorld(target.level())) return false;
                if(source() != null && source().getBoundingBox().inflate(0.1).intersects(target.getBoundingBox())) return true;
                // 鬼域是本宿主的身体延伸，其他模块的接触媒介也能复用其有效覆盖。
                for(var entry : host.getModules()) {
                    var module = GhostModuleRegistry.get(entry.id());
                    var definition = module == null ? null : module.domainDefinition().orElse(null);
                    if(definition == null || !definition.touch()) continue;
                    var domain = ModuleGhostDomains.current(host,entry.id());
                    if(domain != null && ModuleGhostDomains.enabled(host,entry.id())
                            && GhostDomainEntityTracker.get((ServerLevel)level()).getEffectiveEntities(domain).contains(target)) return true;
                }
                return false;
            }

            /** 复用碰撞安全位置搜索，实际移动不包含袭击或技能逻辑。 */
            public boolean approach(LivingEntity target) {
                if (!canMove() || target == null || !(level() instanceof ServerLevel server)) return false;
                var position = GhostApproachPositions.findApproachPosition(server, target);
                if (position == null) return false;
                teleport(position.getX() + 0.5, position.getY(), position.getZ() + 0.5, target);
                return true;
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

            /** 读取模块指定的整数世界规则，不根据模块 ID 选择配置。 */
            public int integerRule(LivingEntity target, GameRules.Key<GameRules.IntegerValue> key, int fallback) {
                return target == null ? fallback : Math.max(1, target.level().getGameRules().getInt(key));
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
            /** 光照读取使用适配后的统一位置，方块、物品与实体共享实现。 */
            public int blockLight() { return level()==null?0:level().getBrightness(LightLayer.BLOCK,BlockPos.containing(position())); }
            /** 天空光为世界事实，不根据宿主类型提供不同规则。 */
            public int skyLight() { return level()==null?0:level().getBrightness(LightLayer.SKY,BlockPos.containing(position())); }

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
             * 使用指定伤害类型和强度尝试灵异击杀。
             * 先检查目标世界，再交给统一死亡处理；返回实际击杀是否成功。
             */
            public boolean attack(LivingEntity target, ResourceKey<DamageType> type, double intensity) {
                if (!host.suppression(id).canUseAbility(host.controlsIntrinsicModules())) return false;
                // 静态来源没有实体形体；由目标所在的服务端世界提供伤害注册表。
                // 有已知世界时仍严格检查维度，不能跨维度袭击。
                if (target == null || !(target.level() instanceof ServerLevel)
                        || (level() != null && !sameWorld(target.level()))) return false;
                var damage = new DamageSource(target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(type), source());
                if(source() instanceof AnimatedModularGhostEntity animated) animated.playVisualAction("attack");
                return SupernaturalDeathHandler.tryKill(target, damage, intensity);
            }

            /**
             * 判断指定位置是否满足敲门系统认可的门条件。
             */
            public boolean isDoor(BlockPos pos) { return present() && KnockingGhostDoorSystem.isDoor(level(),pos); }

            /**
             * 发起敲门的世界操作，声音传播与后续听声处理由敲门系统负责。
             */
            public void knock(BlockPos pos, double intensity, BooleanSupplier valid, Consumer<DoorKnockHeardEvent> heard) {
                if (present() && pos != null) {
                    facePosition(Vec3.atCenterOf(pos).add(0,0.5,0));
                    if(source() instanceof AnimatedModularGhostEntity animated) animated.playVisualAction("knock");
                    KnockingGhostDoorSystem.knock((ServerLevel)level(),
                            new KnockingGhostDoorSystem.Source(source(), source() != null ? source().getUUID() : UUID.nameUUIDFromBytes((level().dimension().location()+":"+position()).getBytes(StandardCharsets.UTF_8)),
                                    () -> present() && valid.getAsBoolean(), heard),pos,intensity);
                }
            }

            /**
             * 将已准备好的袭击排入敲门延迟队列。
             * 延迟任务执行时仍由准备好的能力调用重新检查有效性，不制造请求型事件。
             */
            public void scheduleKnockAttack(Runnable effect) { KnockingGhostDoorSystem.scheduleModuleAttack(effect); }

            /**
             * 检查目标是否存在可播放的喊名声音；没有对应声音时返回 false。
             */
            public boolean hasCall(LivingEntity target) { return canHearCall(target); }

            /**
             * 在目标位置播放可用的喊名声音。
             * 目标不受支持或没有声音资源时不播放。
             */
            public void playCall(LivingEntity target) {
                if (!canHearCall(target)) return;
                facePosition(target.getEyePosition());
                // 动作只作为声音的外观反馈，模块无需知道来源是否具有独特模型。
                if(source() instanceof AnimatedModularGhostEntity animated) animated.playVisualAction("call");
                var sound = target instanceof ServerPlayer player ? CallingGhostSounds.getSound(player) : null;
                if (sound == null) sound = SoundEvents.ENDERMAN_STARE;
                level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.HOSTILE, 1, 1);
            }

            /** 所有暴露在服务端世界的可变宿主都可展开已声明的鬼域。 */
            public boolean canToggleDomain() {
                var module = GhostModuleRegistry.get(id);
                return present() && host instanceof GhostModuleCustomDataModifier && module != null && module.domainDefinition().isPresent();
            }
            /** 修改当前模块的持久开关，再由统一生命周期更新世界。 */
            public boolean toggleDomain() { return canToggleDomain() && ModuleGhostDomains.toggle(host,id); }
            /** 精确匹配来源宿主，避免物品借用携带者的同名鬼域。 */
            private GhostDomain currentDomain() { return ModuleGhostDomains.current(host,id); }
            /** 覆盖事实以追踪器的最终有效集合为准，不把其他鬼域的曝光误认为自己的。 */
            public boolean containsInDomain(UUID domainId, LivingEntity target) {
                var domain = currentDomain();
                return target != null && target.isAlive() && sameWorld(target.level())
                        && domain != null && domain.getId().equals(domainId)
                        && GhostDomainEntityTracker.get((ServerLevel)level()).getEffectiveEntities(domain).contains(target);
            }
            /** 普通伤害走原版 hurt，不进入灵异袭击、死机裁决或短时压制。 */
            public boolean hurt(LivingEntity target, ResourceKey<DamageType> type, float amount) {
                if (!validTarget(target) || !Float.isFinite(amount) || amount <= 0
                        || !host.suppression(id).canUseAbility(host.controlsIntrinsicModules())) return false;
                return target.hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(type), source()), amount);
            }
            /** 对承担物理动作的身体施加停步代价，不向模块泄露载体身份。 */
            public void immobilizeSource() {
                var entity = source();
                if(entity == null || !present()) return;
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,255,false,false,false));
                entity.setDeltaMovement(Vec3.ZERO);entity.hasImpulse=true;
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
