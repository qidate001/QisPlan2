package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.ability.nightwanderer.NightWandererAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class NightWanderer
        extends AbstractGhostEntity {

    private static final double NORMAL_SPEED = 0.25D;
    private static final double DARK_SPEED = 0.8D;
    private static final double LIGHT_SPEED = 0.12D;

    private static final ResourceLocation DARK_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "night_wanderer_dark_speed"
            );

    private static final ResourceLocation LIGHT_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "night_wanderer_light_speed"
            );

    private static final AttributeModifier DARK_SPEED_MODIFIER =
            new AttributeModifier(
                    DARK_SPEED_MODIFIER_ID,
                    DARK_SPEED - NORMAL_SPEED,
                    AttributeModifier.Operation.ADD_VALUE
            );

    private static final AttributeModifier LIGHT_SPEED_MODIFIER =
            new AttributeModifier(
                    LIGHT_SPEED_MODIFIER_ID,
                    LIGHT_SPEED - NORMAL_SPEED,
                    AttributeModifier.Operation.ADD_VALUE
            );

    @Override
    public ResourceLocation getGhostId() {
        return NightWandererAbility.ID;
    }

    /**
     * 灵异攻击后的休息时间：
     * 10 秒 = 200 tick
     */
    private static final int SUPERNATURAL_ATTACK_COOLDOWN = 200;

    /**
     * 当前灵异攻击冷却。
     */
    private int supernaturalAttackCooldown = 0;

    /**
     * 初始灵异强度。
     */
    private static final double BASE_SUPERNATURAL_STRENGTH = 5.0D;

    /**
     * 每击杀一个实体增加的灵异强度。
     */
    private static final double SUPERNATURAL_STRENGTH_PER_KILL = 0.1D;

    /**
     * 击杀数量。
     */
    private int killCount = 0;

    private static final double SUPERNATURAL_DEFENSE = 6.0D;

    private static final String NBT_KILL_COUNT =
            "QisPlan2KillCount";

    /*
     * ============================================================
     * 猎杀能力
     * ============================================================
     */

    /**
     * 超过 100 次击杀后解锁猎杀能力。
     *
     * 100 次：未解锁
     * 101 次：解锁
     */
    private static final int HUNT_UNLOCK_KILLS = 100;

    /**
     * 连续没有锁定目标多久后，
     * 强制发动猎杀。
     *
     * 30 秒 = 600 tick。
     */
    private static final int HUNT_NO_TARGET_TIME = 30 * 20;

    /**
     * 猎杀目标搜索范围。
     */
    private static final double HUNT_TARGET_SEARCH_RANGE = 64.0D;

    /**
     * 猎杀传送后的冷却时间。
     *
     * 10 秒 = 200 tick。
     */
    private static final int HUNT_TELEPORT_COOLDOWN = 10 * 20;

    /**
     * 目标附近寻找传送位置的最大范围。
     */
    private static final double HUNT_TELEPORT_RADIUS = 12.0D;

    /**
     * 与目标保持的最小距离。
     */
    private static final double HUNT_MIN_DISTANCE = 3.0D;

    /**
     * 黑暗判定。
     *
     * 方块光和天空光都不超过 7，
     * 才认为是黑暗。
     */
    private static final int HUNT_DARKNESS_THRESHOLD = 7;

    /**
     * 当前已经连续多少 tick 没有目标。
     */
    private int huntNoTargetTicks = 0;

    /**
     * 猎杀瞬移冷却。
     */
    private int huntTeleportCooldown = 0;

    @Override
    public double getSupernaturalDefense() {
        return SUPERNATURAL_DEFENSE;
    }

    public double getSupernaturalStrength() {
        return BASE_SUPERNATURAL_STRENGTH
                + killCount * SUPERNATURAL_STRENGTH_PER_KILL;
    }

    public int getKillCount() {
        return killCount;
    }

    public void onKillEntity() {
        killCount++;
    }

    public void addKillCount(int amount) {
        if (amount <= 0) {
            return;
        }

        killCount += amount;
    }

    private int getSupernaturalAttackCooldown() {
        return Math.max(
                1,
                SUPERNATURAL_ATTACK_COOLDOWN
                        - killCount / 5
        );
    }

    /**
     * 是否已经解锁猎杀能力。
     */
    public boolean hasHuntAbility() {
        return killCount > HUNT_UNLOCK_KILLS;
    }

    public NightWanderer(
            EntityType<? extends NightWanderer> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {

        /*
         * ========================================
         * 灵异攻击 Goal
         * ========================================
         */

        this.goalSelector.addGoal(
                1,
                new SupernaturalAttackGoal(
                        this,
                        1.2D,
                        3.0D
                )
        );

        /*
         * ========================================
         * 攻击其他 LivingEntity
         * ========================================
         *
         * 玩家由 tickGhostAI() 单独优先寻找。
         *
         * 这里负责其他 LivingEntity。
         */
        this.targetSelector.addGoal(
                2,
                new NearestAttackableTargetGoal<>(
                        this,
                        LivingEntity.class,
                        32,
                        true,
                        false,
                        target -> {
                            if (target instanceof Player player) {
                                return !player.isSpectator()
                                        && !player.isCreative();
                            }

                            return true;
                        }
                )
        );
    }

    /**
     * 夜游鬼每 tick 更新自身状态。
     */
    @Override
    protected void tickGhostAI() {

        /*
         * ========================================
         * 白天
         * ========================================
         *
         * 白天不主动进行移动 AI。
         *
         * 但不会清除当前目标。
         * 因此近距离目标仍然可以被攻击。
         */
        if (level().isDay()) {
            return;
        }

        /*
         * ========================================
         * 灵异攻击冷却
         * ========================================
         */
        if (supernaturalAttackCooldown > 0) {
            supernaturalAttackCooldown--;
        }

        /*
         * ========================================
         * 猎杀瞬移冷却
         * ========================================
         */
        if (huntTeleportCooldown > 0) {
            huntTeleportCooldown--;
        }

        /*
         * ========================================
         * 玩家优先
         * ========================================
         *
         * 只有当前没有有效目标时，
         * 才主动寻找玩家。
         *
         * 这样不会把正在追杀其他实体的目标强行
         * 切换回玩家。
         */
        if (!level().isClientSide()) {

            LivingEntity currentTarget =
                    getTarget();

            if (currentTarget == null
                    || !currentTarget.isAlive()
                    || currentTarget.isRemoved()) {

                Player player =
                        level().getNearestPlayer(
                                this,
                                32.0D
                        );

                if (player != null
                        && player.isAlive()
                        && !player.isSpectator()
                        && !player.isCreative()) {

                    setTarget(player);
                }
            }
        }

        /*
         * ========================================
         * 猎杀能力
         * ========================================
         */
        updateHuntAbility();

        /*
         * ========================================
         * 光照移速
         * ========================================
         */
        updateMovementSpeed();
    }

    /**
     * 更新猎杀能力。
     *
     * 猎杀规则：
     *
     * 1. 击杀数超过 100 后解锁。
     * 2. 如果当前存在有效目标，计时归零。
     * 3. 如果连续 30 秒没有目标：
     *      - 搜索附近所有 LivingEntity
     *      - 黑暗中的实体优先
     *      - 同样都是黑暗时，距离近的优先
     *      - 没有黑暗目标时，选择最近的普通目标
     * 4. 找到目标后，寻找目标附近的黑暗位置。
     * 5. 瞬移过去并锁定目标。
     */
    private void updateHuntAbility() {

        /*
         * ========================================
         * 是否解锁
         * ========================================
         */
        if (!hasHuntAbility()) {
            huntNoTargetTicks = 0;
            return;
        }

        /*
         * ========================================
         * 瞬移冷却
         * ========================================
         */
        if (huntTeleportCooldown > 0) {
            return;
        }

        /*
         * ========================================
         * 当前目标
         * ========================================
         */
        LivingEntity target = getTarget();

        /*
         * 有有效目标。
         *
         * 猎杀计时归零。
         */
        if (target != null
                && target.isAlive()
                && !target.isRemoved()) {

            huntNoTargetTicks = 0;
            return;
        }

        /*
         * ========================================
         * 没有目标
         * ========================================
         */
        huntNoTargetTicks++;

        /*
         * 还没有达到 30 秒。
         */
        if (huntNoTargetTicks < HUNT_NO_TARGET_TIME) {
            return;
        }

        /*
         * 防止同一时刻重复触发。
         */
        huntNoTargetTicks = 0;

        /*
         * ========================================
         * 强制寻找猎杀目标
         * ========================================
         */
        LivingEntity huntTarget =
                findHuntTarget();

        if (huntTarget == null) {

            QisPlan2.LOGGER.info(
                    "[夜游鬼猎杀] 夜游鬼 {} 等待了 30 秒，但附近没有找到可猎杀的实体",
                    getUUID()
            );

            return;
        }

        /*
         * ========================================
         * 寻找目标附近的黑暗位置
         * ========================================
         */
        BlockPos teleportPos =
                findHuntTeleportPosition(
                        huntTarget
                );

        if (teleportPos == null) {

            QisPlan2.LOGGER.info(
                    "[夜游鬼猎杀] 夜游鬼 {} 找到了目标 {}，但目标附近没有合适的黑暗位置",
                    getUUID(),
                    huntTarget.getUUID()
            );

            /*
             * 即使没有找到黑暗位置，
             * 仍然锁定这个目标。
             */
            setTarget(huntTarget);

            return;
        }

        /*
         * ========================================
         * 执行猎杀
         * ========================================
         */
        Vec3 teleportPosVec =
                Vec3.atBottomCenterOf(
                        teleportPos
                );

        teleportTo(
                teleportPosVec.x,
                teleportPosVec.y,
                teleportPosVec.z
        );

        /*
         * 停止原来的导航。
         */
        getNavigation().stop();

        /*
         * 锁定目标。
         */
        setTarget(huntTarget);

        /*
         * 设置瞬移冷却。
         */
        huntTeleportCooldown =
                HUNT_TELEPORT_COOLDOWN;

        QisPlan2.LOGGER.info(
                "[夜游鬼猎杀] 夜游鬼 {} 强制猎杀目标 {}，"
                        + "目标是否处于黑暗：{}，"
                        + "传送位置：{}",
                getUUID(),
                huntTarget.getUUID(),
                isDarkEntity(huntTarget),
                teleportPos
        );
    }

    /**
     * 寻找猎杀目标。
     *
     * 优先级：
     *
     * 第一优先级：
     *     黑暗中的实体
     *
     * 第二优先级：
     *     光照中的实体
     *
     * 同一优先级下：
     *     距离夜游鬼最近的实体
     */
    private LivingEntity findHuntTarget() {

        AABB searchBox =
                getBoundingBox().inflate(
                        HUNT_TARGET_SEARCH_RANGE
                );

        List<LivingEntity> entities =
                level().getEntitiesOfClass(
                        LivingEntity.class,
                        searchBox,
                        entity -> {

                            /*
                             * 不能选择自己。
                             */
                            if (entity == this) {
                                return false;
                            }

                            /*
                             * 必须还活着。
                             */
                            if (!entity.isAlive()) {
                                return false;
                            }

                            /*
                             * 已经被移除的实体不能成为目标。
                             */
                            if (entity.isRemoved()) {
                                return false;
                            }

                            /*
                             * 创造和旁观玩家排除。
                             */
                            if (entity instanceof Player player) {
                                return !player.isCreative()
                                        && !player.isSpectator();
                            }

                            /*
                             * 其他 LivingEntity 全部可以成为目标。
                             */
                            return true;
                        }
                );

        if (entities.isEmpty()) {
            return null;
        }

        LivingEntity bestTarget = null;

        boolean bestIsDark = false;

        double bestDistance = Double.MAX_VALUE;

        for (LivingEntity entity : entities) {

            boolean dark =
                    isDarkEntity(entity);

            double distance =
                    distanceToSqr(entity);

            /*
             * 当前还没有候选目标。
             */
            if (bestTarget == null) {

                bestTarget = entity;
                bestIsDark = dark;
                bestDistance = distance;

                continue;
            }

            /*
             * 黑暗优先。
             *
             * 当前是黑暗，而原目标不是黑暗：
             * 直接替换。
             */
            if (dark && !bestIsDark) {

                bestTarget = entity;
                bestIsDark = true;
                bestDistance = distance;

                continue;
            }

            /*
             * 当前是光亮，而原目标是黑暗：
             * 不替换。
             */
            if (!dark && bestIsDark) {
                continue;
            }

            /*
             * 光照条件相同：
             *
             * 距离近的优先。
             */
            if (distance < bestDistance) {

                bestTarget = entity;
                bestIsDark = dark;
                bestDistance = distance;
            }
        }

        return bestTarget;
    }

    /**
     * 判断一个实体是否处于黑暗中。
     */
    private boolean isDarkEntity(
            LivingEntity entity
    ) {

        BlockPos pos =
                entity.blockPosition();

        return isDarkPosition(pos);
    }

    /**
     * 判断一个位置是否属于黑暗。
     */
    private boolean isDarkPosition(BlockPos pos) {
        int blockLight =
                level().getBrightness(
                        LightLayer.BLOCK,
                        pos
                );

        if (blockLight > HUNT_DARKNESS_THRESHOLD) {
            return false;
        }

        /*
         * 夜晚的露天空旷区域虽然仍然存在天空光，
         * 但对于夜游鬼来说属于可接受的黑暗环境。
         */
        if (level().isNight()) {
            return true;
        }

        int skyLight =
                level().getBrightness(
                        LightLayer.SKY,
                        pos
                );

        return skyLight <= HUNT_DARKNESS_THRESHOLD;
    }

    /**
     * 在目标附近寻找一个黑暗、安全的传送位置。
     */
    private BlockPos findHuntTeleportPosition(
            LivingEntity target
    ) {
        BlockPos targetPos = target.blockPosition();

        BlockPos bestPosition = null;
        double bestDistance = Double.MAX_VALUE;

        int radius = Mth.ceil(HUNT_TELEPORT_RADIUS);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {

                double horizontalDistanceSqr =
                        x * x + z * z;

                if (horizontalDistanceSqr
                        < HUNT_MIN_DISTANCE * HUNT_MIN_DISTANCE) {
                    continue;
                }

                if (horizontalDistanceSqr
                        > HUNT_TELEPORT_RADIUS * HUNT_TELEPORT_RADIUS) {
                    continue;
                }

                int worldX = targetPos.getX() + x;
                int worldZ = targetPos.getZ() + z;

                /*
                 * 目标上下几格都检查。
                 *
                 * 这样即使目标站在坑里、楼梯上、
                 * 高低差地形上，也能找到合适位置。
                 */
                for (int yOffset = -3; yOffset <= 3; yOffset++) {

                    int worldY =
                            targetPos.getY() + yOffset;

                    BlockPos candidate =
                            new BlockPos(
                                    worldX,
                                    worldY,
                                    worldZ
                            );

                    if (!isValidHuntTeleportPosition(
                            candidate,
                            target
                    )) {
                        continue;
                    }

                    double distanceSqr =
                            candidate.distSqr(targetPos);

                    if (bestPosition == null
                            || distanceSqr < bestDistance) {

                        bestPosition = candidate;
                        bestDistance = distanceSqr;
                    }
                }
            }
        }

        return bestPosition;
    }

    /**
     * 判断一个位置是否可以作为猎杀传送位置。
     */
    private boolean isValidHuntTeleportPosition(
            BlockPos pos,
            LivingEntity target
    ) {
        /*
         * 必须是黑暗位置。
         */
        if (!isDarkPosition(pos)) {
            return false;
        }

        /*
         * 至少与目标保持一定距离，
         * 避免直接与目标重叠。
         */
        double distanceSqr =
                pos.distSqr(target.blockPosition());

        if (distanceSqr
                < HUNT_MIN_DISTANCE * HUNT_MIN_DISTANCE) {
            return false;
        }

        /*
         * 脚下必须可以站立。
         *
         * 比 isSolid() 更适合判断“能不能站在这里”。
         */
        if (!level().getBlockState(
                pos.below()
        ).isFaceSturdy(
                level(),
                pos.below(),
                net.minecraft.core.Direction.UP
        )) {
            return false;
        }

        /*
         * 自己所在位置必须为空。
         */
        if (!level().getBlockState(pos).getCollisionShape(
                level(),
                pos
        ).isEmpty()) {
            return false;
        }

        /*
         * 头部空间必须为空。
         */
        if (!level().getBlockState(pos.above()).getCollisionShape(
                level(),
                pos.above()
        ).isEmpty()) {
            return false;
        }

        /*
         * 最终进行实体碰撞检查。
         */
        Vec3 position =
                Vec3.atBottomCenterOf(pos);

        AABB movedBox =
                getBoundingBox().move(
                        position.x - getX(),
                        position.y - getY(),
                        position.z - getZ()
                );

        return level().noCollision(
                this,
                movedBox
        );
    }

    /**
     * 更新夜游鬼的光照移动速度。
     */
    private void updateMovementSpeed() {

        if (level().isClientSide()) {
            return;
        }

        AttributeInstance speedAttribute =
                getAttribute(
                        Attributes.MOVEMENT_SPEED
                );

        if (speedAttribute == null) {
            return;
        }

        int blockLight =
                level().getBrightness(
                        LightLayer.BLOCK,
                        blockPosition()
                );

        int skyLight =
                level().getBrightness(
                        LightLayer.SKY,
                        blockPosition()
                );

        boolean isDay =
                level().isDay();

        /*
         * ========================================
         * 亮处
         * ========================================
         *
         * 1. 方块光很强：火把、灯笼、萤石等
         * 2. 白天天空光很强：露天环境
         */
        boolean bright =
                blockLight >= 8
                        || (isDay && skyLight >= 8);

        /*
         * ========================================
         * 暗处
         * ========================================
         *
         * 1. 方块光很低
         * 2. 夜晚不考虑天空光本身
         *
         * 因此夜晚露天也可以进入高速状态。
         */
        boolean dark =
                blockLight <= 3
                        && (!isDay || skyLight <= 3);

        if (dark) {

            removeLightSpeedModifier(
                    speedAttribute
            );

            if (!speedAttribute.hasModifier(
                    DARK_SPEED_MODIFIER_ID
            )) {
                speedAttribute.addTransientModifier(
                        DARK_SPEED_MODIFIER
                );
            }

        } else if (bright) {

            removeDarkSpeedModifier(
                    speedAttribute
            );

            if (!speedAttribute.hasModifier(
                    LIGHT_SPEED_MODIFIER_ID
            )) {
                speedAttribute.addTransientModifier(
                        LIGHT_SPEED_MODIFIER
                );
            }

        } else {

            removeDarkSpeedModifier(
                    speedAttribute
            );

            removeLightSpeedModifier(
                    speedAttribute
            );
        }
    }

    private void removeDarkSpeedModifier(
            AttributeInstance attribute
    ) {
        if (attribute.hasModifier(
                DARK_SPEED_MODIFIER_ID
        )) {
            attribute.removeModifier(
                    DARK_SPEED_MODIFIER_ID
            );
        }
    }

    private void removeLightSpeedModifier(
            AttributeInstance attribute
    ) {
        if (attribute.hasModifier(
                LIGHT_SPEED_MODIFIER_ID
        )) {
            attribute.removeModifier(
                    LIGHT_SPEED_MODIFIER_ID
            );
        }
    }

    /**
     * 实体属性。
     */
    public static AttributeSupplier.Builder createAttributes() {

        return Mob.createMobAttributes()
                .add(
                        Attributes.MAX_HEALTH,
                        20.0D
                )
                .add(
                        Attributes.MOVEMENT_SPEED,
                        NORMAL_SPEED
                )
                .add(
                        Attributes.FOLLOW_RANGE,
                        32.0D
                );
    }

    /**
     * 自定义灵异攻击 Goal。
     */
    private static class SupernaturalAttackGoal
            extends Goal {

        private final NightWanderer mob;

        private final double speedModifier;

        private final double attackRangeSqr;

        public SupernaturalAttackGoal(
                NightWanderer mob,
                double speedModifier,
                double attackRange
        ) {

            this.mob = mob;

            this.speedModifier =
                    speedModifier;

            this.attackRangeSqr =
                    attackRange * attackRange;

            this.setFlags(
                    EnumSet.of(
                            Goal.Flag.MOVE,
                            Goal.Flag.LOOK
                    )
            );
        }

        /**
         * 冷却结束并且存在有效目标时开始 Goal。
         */
        @Override
        public boolean canUse() {

            /*
             * 白天禁止移动。
             */
            if (mob.level().isDay()) {
                return false;
            }

            LivingEntity target =
                    mob.getTarget();

            return !mob.isSupernaturallyStunned()
                    && mob.supernaturalAttackCooldown <= 0
                    && target != null
                    && target.isAlive();
        }

        /**
         * 冷却开始或者目标消失时结束 Goal。
         */
        @Override
        public boolean canContinueToUse() {

            /*
             * 白天禁止移动。
             */
            if (mob.level().isDay()) {
                return false;
            }

            LivingEntity target =
                    mob.getTarget();

            return !mob.isSupernaturallyStunned()
                    && mob.supernaturalAttackCooldown <= 0
                    && target != null
                    && target.isAlive();
        }

        @Override
        public void start() {

            LivingEntity target =
                    mob.getTarget();

            if (target != null) {

                mob.getNavigation().moveTo(
                        target,
                        speedModifier
                );
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {

            LivingEntity target =
                    mob.getTarget();

            if (target == null
                    || !target.isAlive()) {

                return;
            }

            /*
             * 一直看向目标。
             */
            mob.getLookControl().setLookAt(
                    target,
                    30.0F,
                    30.0F
            );

            double distanceSqr =
                    mob.distanceToSqr(target);

            /*
             * ========================================
             * 还没进入攻击距离
             * ========================================
             */
            if (distanceSqr > attackRangeSqr) {

                mob.getNavigation().moveTo(
                        target,
                        speedModifier
                );

                return;
            }

            /*
             * ========================================
             * 灵异攻击
             * ========================================
             */

            mob.getNavigation().stop();

            mob.swing(
                    InteractionHand.MAIN_HAND
            );

            boolean killed =
                    SupernaturalDeathHandler.tryKill(
                            target,
                            ModDamageTypes.ghostNightWanderer(mob),
                            mob.getSupernaturalStrength()
                    );

            if (killed) {
                mob.onKillEntity();
            }

            /*
             * ========================================
             * 攻击结束，进入休息时间
             * ========================================
             */
            mob.supernaturalAttackCooldown =
                    mob.getSupernaturalAttackCooldown();
        }
    }

    @Override
    public void addAdditionalSaveData(
            CompoundTag tag
    ) {

        super.addAdditionalSaveData(tag);

        tag.putInt(
                NBT_KILL_COUNT,
                killCount
        );
    }

    @Override
    public void readAdditionalSaveData(
            CompoundTag tag
    ) {

        super.readAdditionalSaveData(tag);

        killCount =
                Math.max(
                        0,
                        tag.getInt(NBT_KILL_COUNT)
                );
    }
}