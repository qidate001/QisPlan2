package com.qidate.qisplan2.entity.ai;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.GhostAvoidanceSystem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;

import java.util.EnumSet;

/**
 * 所有普通厉鬼都可以使用的基础随机游荡 Goal。
 *
 * 作用：
 *
 * 1. 没有更高优先级行为时四处游荡。
 * 2. 每次选择一个随机位置。
 * 3. 到达后等待一段时间。
 * 4. 不主动寻找或攻击生物。
 */
public class GhostWanderGoal extends Goal {

    protected final PathfinderMob mob;

    private final double speedModifier;

    /**
     * 随机游荡最大水平距离。
     */
    private final int horizontalDistance;

    /**
     * 随机游荡最大垂直距离。
     */
    private final int verticalDistance;

    /**
     * 两次游荡之间的最短等待时间。
     */
    private final int minCooldown;

    /**
     * 两次游荡之间的最长等待时间。
     */
    private final int maxCooldown;

    private int cooldown = 0;
    private LivingEntity candleTarget;

    /**
     * 默认配置。
     */
    public GhostWanderGoal(
            PathfinderMob mob,
            double speedModifier
    ) {
        this(
                mob,
                speedModifier,
                12,
                4,
                20,
                60
        );
    }

    /**
     * 完整配置。
     */
    public GhostWanderGoal(
            PathfinderMob mob,
            double speedModifier,
            int horizontalDistance,
            int verticalDistance,
            int minCooldown,
            int maxCooldown
    ) {
        this.mob =
                mob;

        this.speedModifier =
                speedModifier;

        this.horizontalDistance =
                horizontalDistance;

        this.verticalDistance =
                verticalDistance;

        this.minCooldown =
                minCooldown;

        this.maxCooldown =
                maxCooldown;

        setFlags(
                EnumSet.of(
                        Flag.MOVE
                )
        );
    }

    /**
     * 判断当前行为是否可以开始执行。
     * 检查行为需要的模块状态、环境或目标条件，交由 Goal 调度器决定是否取得控制。
     */
    @Override
    public boolean canUse() {

        /*
         * 死机期间不游荡。
         */
        if (mob instanceof ModularGhostEntity ghost && ghost.isSupernaturallyStunned()) {

            return false;
        }

        candleTarget = GhostAvoidanceSystem.preferredTarget(mob, 32, p -> true);
        if (candleTarget != null) return true;

        /*
         * 冷却。
         */
        if (cooldown > 0) {

            cooldown--;

            return false;
        }

        /*
         * 偶尔开始一次。
         *
         * 避免每次到达目标后立即再次选择位置。
         */
        return mob.getRandom().nextInt(20) == 0;
    }

    /**
     * 重新检查行为是否仍可继续。
     * 条件不满足时由调度器停止行为，释放占用的控制资源。
     */
    @Override
    public boolean canContinueToUse() {

        /*
         * 死机立即停止。
         */
        if (mob instanceof ModularGhostEntity ghost && ghost.isSupernaturallyStunned()) {

            return false;
        }

        /*
         * 到达目标以后结束。
         */
        var preferred = GhostAvoidanceSystem.preferredTarget(mob, 32, p -> true);
        if (candleTarget != null && preferred == null) return false;
        candleTarget = preferred;
        return candleTarget != null || !mob.getNavigation().isDone();
    }

    /**
     * 取得行为控制后初始化本轮行动，开始对应的移动或目标处理。
     */
    @Override
    public void start() {

        if (candleTarget != null) mob.getNavigation().moveTo(candleTarget, speedModifier);
        else chooseRandomPosition();

        cooldown =
                minCooldown
                        + mob.getRandom().nextInt(
                                Math.max(
                                        1,
                                        maxCooldown
                                                - minCooldown
                                                + 1
                                )
                        );
    }

    /**
     * 停止当前行为，释放移动控制并清理该行为持有的临时目标。
     */
    @Override public void stop() {
        if (candleTarget != null) mob.getNavigation().stop();
        candleTarget = null;
    }

    /**
     * 推进当前 tick 的行为或待处理任务；具体执行受本方法的有效性检查约束。
     */
    @Override public void tick() {
        if (candleTarget == null) return;
        if (mob.distanceToSqr(candleTarget) <= 4) mob.getNavigation().stop();
        else if (mob.tickCount % 10 == 0) mob.getNavigation().moveTo(candleTarget, speedModifier);
    }

    /**
     * 为游荡行为选择候选位置；没有合适位置时按现有逻辑放弃本次寻路。
     */
    private void chooseRandomPosition() {

        /*
         * LandRandomPos 专门用于寻找适合
         * PathfinderMob 行走的随机位置。
         */
        net.minecraft.world.phys.Vec3 position =
                LandRandomPos.getPos(
                        mob,
                        horizontalDistance,
                        verticalDistance
                );

        if (position == null) {
            return;
        }

        mob.getNavigation().moveTo(
                position.x,
                position.y,
                position.z,
                speedModifier
        );
    }
}