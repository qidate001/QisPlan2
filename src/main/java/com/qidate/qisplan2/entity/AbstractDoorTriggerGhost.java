package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractDoorTriggerGhost
        extends AbstractGhostEntity {

    /**
     * 触发范围。
     */
    public static final double TRIGGER_RADIUS = 100.0D;

    /**
     * 灵异攻击强度。
     *
     * 与敲门鬼保持一致。
     */
    protected static final double ATTACK_STRENGTH = 5.0D;

    protected AbstractDoorTriggerGhost(
            EntityType<? extends AbstractDoorTriggerGhost> entityType,
            Level level
    ) {
        super(
                entityType,
                level
        );
    }

    /**
     * 子类决定自己的攻击类型。
     */
    protected abstract net.minecraft.world.damagesource.DamageSource getAttackDamageSource(
            Entity source
    );

    /**
     * 触发门事件。
     *
     * @param source 谁打开/关闭了门
     * @param doorPos 门下半部分位置
     * @param doorState 门当前状态
     */
    public void triggerDoorEvent(
            Entity source,
            BlockPos doorPos,
            BlockState doorState
    ) {

        if (isSupernaturallyStunned()) {
            return;
        }

        if (!(source instanceof LivingEntity living)) {
            return;
        }

        if (!living.isAlive()) {
            return;
        }

        if (!(level() instanceof ServerLevel)) {
            return;
        }

        /*
         * ========================================================
         * 判断玩家 / 生物在门的哪一侧
         * ========================================================
         *
         * FACING 作为门平面的法线。
         */
        Direction facing =
                doorState.getValue(
                        DoorBlock.FACING
                );

        double doorX =
                doorPos.getX() + 0.5D;

        double doorY =
                doorPos.getY();

        double doorZ =
                doorPos.getZ() + 0.5D;

        double dx =
                source.getX() - doorX;

        double dz =
                source.getZ() - doorZ;

        double side =
                dx * facing.getStepX()
                        + dz * facing.getStepZ();

        /*
         * 如果恰好卡在门平面上，
         * 默认选择 FACING 的反方向。
         */
        int sideSign =
                side >= 0.0D
                        ? 1
                        : -1;

        /*
         * ========================================================
         * 瞬移到门前 / 门后
         * ========================================================
         */
        teleportToDoorSide(
                doorPos,
                facing,
                sideSign,
                source
        );

        /*
         * ========================================================
         * 对触发者发动灵异袭击
         * ========================================================
         */
        SupernaturalDeathHandler.tryKill(
                living,
                getAttackDamageSource(source),
                ATTACK_STRENGTH
        );
    }

    /**
     * 瞬移到门的某一侧。
     */
    private void teleportToDoorSide(
            BlockPos doorPos,
            Direction facing,
            int sideSign,
            Entity source
    ) {

        /*
         * ========================================================
         * 目标位置
         * ========================================================
         *
         * 距离门中心约 0.8 格。
         */
        double x =
                doorPos.getX()
                        + 0.5D
                        + facing.getStepX()
                        * sideSign
                        * 0.8D;

        double z =
                doorPos.getZ()
                        + 0.5D
                        + facing.getStepZ()
                        * sideSign
                        * 0.8D;

        /*
         * 高度跟随触发者。
         *
         * 这样如果有人站在二层门边，
         * 就不会突然跑到门底。
         */
        double y =
                Math.max(
                        doorPos.getY(),
                        source.getY()
                );

        teleportTo(
                x,
                y,
                z
        );

        /*
         * 瞬移后朝向触发者。
         */
        getLookControl().setLookAt(
                source,
                30.0F,
                30.0F
        );

        getNavigation().stop();
        setTarget(null);
        setAggressive(false);
    }
}