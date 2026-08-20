package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * 死亡诅咒客户端处理器
 *
 * 只负责记录玩家最后一次攻击的目标。
 */
@EventBusSubscriber(
        modid = QisPlan2.MODID,
        value = Dist.CLIENT
)
public class DeathCurseClientHandler {

    /**
     * 最后一次攻击的目标
     */
    private static LivingEntity lastAttackTarget;

    /**
     * 玩家攻击实体时记录目标
     */
    @SubscribeEvent
    public static void onAttackEntity(
            AttackEntityEvent event
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        // 只处理客户端自己的玩家
        if (minecraft.player == null) {
            return;
        }

        if (event.getEntity() != minecraft.player) {
            return;
        }

        // 只记录生物实体
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        lastAttackTarget = target;
    }

    /**
     * 获取最后攻击目标
     */
    public static LivingEntity getLastAttackTarget() {
        return lastAttackTarget;
    }

    /**
     * 清除最后攻击目标
     */
    public static void clearLastAttackTarget() {
        lastAttackTarget = null;
    }
}