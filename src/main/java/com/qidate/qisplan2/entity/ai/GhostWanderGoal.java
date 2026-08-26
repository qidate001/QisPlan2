package com.qidate.qisplan2.entity.ai;

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
public class GhostWanderGoal
        extends Goal {

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

    @Override
    public boolean canUse() {

        /*
         * 死机期间不游荡。
         */
        if (mob instanceof
                com.qidate.qisplan2.entity.AbstractGhostEntity ghost
                && ghost.isSupernaturallyStunned()) {

            return false;
        }

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

    @Override
    public boolean canContinueToUse() {

        /*
         * 死机立即停止。
         */
        if (mob instanceof
                com.qidate.qisplan2.entity.AbstractGhostEntity ghost
                && ghost.isSupernaturallyStunned()) {

            return false;
        }

        /*
         * 到达目标以后结束。
         */
        return !mob.getNavigation().isDone();
    }

    @Override
    public void start() {

        chooseRandomPosition();

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

    @Override
    public void stop() {

        /*
         * 不主动停止导航。
         *
         * 更高优先级 Goal 接管时，
         * 由 AI 系统处理。
         */
    }

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