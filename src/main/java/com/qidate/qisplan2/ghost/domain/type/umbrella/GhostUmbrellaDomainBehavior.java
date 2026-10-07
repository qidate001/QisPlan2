package com.qidate.qisplan2.ghost.domain.type.umbrella;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainBehavior;
import com.qidate.qisplan2.item.GhostUmbrellaItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * ========================================
 * 鬼雨伞鬼域行为
 * ========================================
 *
 * 鬼雨伞鬼域的主要规则：
 *
 * 1. 只要鬼伞处于开启状态，
 *    持伞者每 tick 都受到鬼伞副作用。
 *
 * 2. 鬼伞每秒对鬼域内符合条件的生物
 *    发动一次灵异袭击。
 *
 * 3. 鬼伞会按照一定强度
 *    反噬持伞者。
 *
 * 4. 只有当鬼伞鬼域是目标当前的
 *    最终生效鬼域时，鬼伞才能发动袭击。
 *
 * <p>
 * 实体关系由 GhostDomainEntityTracker
 * 统一维护。
 * </p>
 *
 * <p>
 * GhostDomainManager 负责调用本行为，
 * 本行为只负责执行鬼伞规则。
 * </p>
 */
public final class GhostUmbrellaDomainBehavior
        implements GhostDomainBehavior {

    /**
     * 每秒攻击一次。
     */
    private static final int ATTACK_INTERVAL = 20;

    /**
     * 初始灵异攻击强度。
     */
    private static final double INITIAL_ATTACK_STRENGTH = 10.0D;

    /**
     * 反噬分母。
     */
    private static final double SELF_ATTACK_DIVISOR = 4.0D;

    /**
     * 鬼伞行为执行间隔。
     *
     * <p>
     * 鬼伞存在每 tick 的持伞者副作用，
     * 因此必须让 GhostDomainManager 每 tick
     * 调用一次本行为。
     * </p>
     *
     * @return 1 tick
     */
    @Override
    public int getTickInterval() {
        return 1;
    }

    /**
     * 每 tick 执行鬼伞鬼域规则。
     *
     * <p>
     * 本方法包含两类行为：
     *
     * <ul>
     *     <li>每 tick：处理持伞者副作用</li>
     *     <li>每 20 tick：发动灵异袭击与反噬</li>
     * </ul>
     *
     * <p>
     * 实体集合由 GhostDomainManager
     * 从 GhostDomainEntityTracker 获取后传入。
     * 本行为不会自行访问 Tracker。
     *
     * @param level 当前服务器维度
     * @param domain 当前鬼雨伞鬼域
     * @param entities 当前最终生效于该鬼域的实体
     */
    @Override
    public void onTick(
            ServerLevel level,
            GhostDomain domain,
            Set<Entity> entities
    ) {

        /*
         * ========================================
         * 获取持伞者
         * ========================================
         */
        ServerPlayer source =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(
                                domain.getSourceUUID()
                        );

        if (source == null) {
            return;
        }

        /*
         * 玩家已经不在这个维度了。
         *
         * 正常情况下跨维度事件会提前清理 GhostDomain，
         * 这里再做一道保险。
         */
        if (source.serverLevel() != level) {
            return;
        }

        /*
         * 鬼伞已经关闭。
         *
         * 正常情况下 GhostUmbrellaDomain.tick()
         * 会负责删除领域。
         *
         * 这里仍然保留安全检查。
         */
        if (!hasOpenUmbrella(source)) {
            return;
        }

        /*
         * ========================================
         * 每 tick：持伞者副作用
         * ========================================
         */
        updateUmbrellaHolderState(source);

        /*
         * ========================================
         * 每秒一次：灵异攻击
         * ========================================
         */
        if (level.getGameTime()
                % ATTACK_INTERVAL
                != 0L) {

            return;
        }

        /*
         * 计算本次攻击强度。
         */
        double strength =
                getAttackStrength(source);

        /*
         * 攻击当前最终生效于鬼伞鬼域的其他生物。
         */
        attackEntities(
                level,
                domain,
                source,
                strength,
                entities
        );

        /*
         * 反噬持伞者。
         *
         * 持伞者本身如果属于当前鬼伞鬼域，
         * 则 Tracker 已经会将其包含在 entities 中。
         */
        attackUmbrellaHolder(
                level,
                domain,
                source,
                strength / SELF_ATTACK_DIVISOR,
                entities
        );
    }

    /**
     * 持伞者的鬼伞副作用。
     *
     * <p>
     * 当前鬼伞会：
     *
     * <ul>
     *     <li>施加极强缓慢</li>
     *     <li>将玩家速度归零</li>
     * </ul>
     *
     * @param player 当前持伞者
     */
    private void updateUmbrellaHolderState(
            ServerPlayer player
    ) {

        /*
         * ========================================
         * 失明
         * ========================================
         *
         * 当前暂时关闭。
         */
//        player.addEffect(
//                new MobEffectInstance(
//                        MobEffects.BLINDNESS,
//                        40,
//                        0,
//                        false,
//                        false,
//                        true
//                )
//        );

        /*
         * ========================================
         * 极强缓慢
         * ========================================
         */
        player.addEffect(
                new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        40,
                        255,
                        false,
                        false,
                        false
                )
        );

        /*
         * ========================================
         * 彻底禁止移动
         * ========================================
         */
        player.setDeltaMovement(
                0.0D,
                0.0D,
                0.0D
        );

        player.hasImpulse = true;
    }

    /**
     * 反噬持伞者。
     *
     * <p>
     * 持伞者必须是当前鬼伞鬼域的
     * 最终生效实体，鬼伞才会对其进行反噬。
     *
     * <p>
     * 这个关系已经由 Tracker 在上游计算完成，
     * 因此这里直接检查传入的实体集合。
     *
     * @param level 当前服务器维度
     * @param domain 当前鬼雨伞鬼域
     * @param player 持伞者
     * @param strength 反噬强度
     * @param entities 当前最终生效于鬼域的实体
     */
    private void attackUmbrellaHolder(
            ServerLevel level,
            GhostDomain domain,
            ServerPlayer player,
            double strength,
            Set<Entity> entities
    ) {

        if (!player.isAlive()) {
            return;
        }

        /*
         * 只有 Tracker 确认持伞者当前最终生效于
         * 这个鬼伞鬼域时，鬼伞才会反噬持伞者。
         */
        if (!entities.contains(player)) {
            return;
        }

        SupernaturalDeathHandler.tryKill(
                player,
                ModDamageTypes.ghostUmbrella(player),
                strength
        );
    }

    /**
     * 攻击已经由 Tracker 确认最终生效于
     * 当前鬼域的其他生物。
     *
     * <p>
     * 本方法不进行 AABB 搜索，
     * 也不重新判断实体最终属于哪个鬼域。
     * </p>
     *
     * @param level 当前服务器维度
     * @param domain 当前鬼雨伞鬼域
     * @param source 持伞者
     * @param strength 攻击强度
     * @param entities 当前最终生效于该鬼域的实体
     */
    private void attackEntities(
            ServerLevel level,
            GhostDomain domain,
            ServerPlayer source,
            double strength,
            Set<Entity> entities
    ) {

        for (Entity entity : entities) {

            /*
             * 不攻击持伞者自己。
             *
             * 持伞者的反噬由单独逻辑处理。
             */
            if (entity == source) {
                continue;
            }

            /*
             * 只攻击生物。
             */
            if (!(entity instanceof LivingEntity target)) {
                continue;
            }

            /*
             * 已经死亡的目标跳过。
             */
            if (!target.isAlive()) {
                continue;
            }

            /*
             * ========================================
             * 发动灵异袭击
             * ========================================
             */
            SupernaturalDeathHandler.tryKill(
                    target,
                    ModDamageTypes.ghostUmbrella(source),
                    strength
            );
        }
    }

    /**
     * 判断玩家是否正在开启鬼伞。
     *
     * @param player 玩家
     * @return 是否至少有一只开启的鬼伞
     */
    private static boolean hasOpenUmbrella(
            ServerPlayer player
    ) {

        return isOpenUmbrella(
                player.getMainHandItem()
        ) || isOpenUmbrella(
                player.getOffhandItem()
        );
    }

    /**
     * 判断物品是否为开启状态的鬼伞。
     *
     * @param stack 待检查的物品
     * @return 是否为开启的鬼伞
     */
    private static boolean isOpenUmbrella(
            ItemStack stack
    ) {

        return stack.getItem()
                instanceof GhostUmbrellaItem
                && GhostUmbrellaItem.isOpen(stack);
    }

    /**
     * 获取当前鬼雨攻击强度。
     *
     * <p>
     * 当前使用固定的初始攻击强度。
     * 后续可以根据鬼伞状态、驭鬼者状态等
     * 动态计算。
     *
     * @param source 持伞者
     * @return 当前攻击强度
     */
    private static double getAttackStrength(
            ServerPlayer source
    ) {
        return INITIAL_ATTACK_STRENGTH;
    }

    /**
     * 禁用化虹。
     *
     * @param level 当前服务器维度
     * @param domain 当前鬼伞鬼域
     * @param player 尝试化虹的玩家
     * @return false，禁止化虹
     */
    @Override
    public boolean canTeleport(
            ServerLevel level,
            GhostDomain domain,
            ServerPlayer player
    ) {
        return false;
    }

    /**
     * 禁用鬼域视觉描边。
     *
     * @param level 当前服务器维度
     * @param domain 当前鬼伞鬼域
     * @return false，不显示实体视觉描边
     */
    @Override
    public boolean hasEntityVision(
            ServerLevel level,
            GhostDomain domain
    ) {
        return false;
    }
}