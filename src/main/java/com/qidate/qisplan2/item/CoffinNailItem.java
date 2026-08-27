package com.qidate.qisplan2.item;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.entity.AbstractGhostEntity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CoffinNailItem
        extends Item {

    public CoffinNailItem(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {

        /*
         * ========================================================
         * 只处理厉鬼。
         * ========================================================
         */
        if (!(target instanceof AbstractGhostEntity ghost)) {

            return InteractionResult.PASS;
        }

        /*
         * ========================================================
         * Shift + 右键：
         *
         * 不在这里处理。
         *
         * 拔钉由 AbstractGhostEntity.mobInteract()
         * 统一处理，而且空手也可以拔。
         * ========================================================
         */
        if (player.isShiftKeyDown()) {

            return InteractionResult.PASS;
        }

        /*
         * ========================================================
         * 客户端预测。
         * ========================================================
         */
        if (player.level().isClientSide()) {

            return InteractionResult.SUCCESS;
        }

        /*
         * ========================================================
         * 已经被棺材钉钉住。
         *
         * 不重复钉，也不消耗物品。
         * ========================================================
         */
        if (ghost.isCoffinNailed()) {

            return InteractionResult.CONSUME;
        }

        /*
         * ========================================================
         * 钉入棺材钉。
         * ========================================================
         */
        ghost.setCoffinNailed(
                true
        );

        /*
         * 立即开始普通死机。
         */
        ghost.setSupernaturalStunTicks(
                20
        );

        /*
         * 非创造模式消耗一根。
         */
        if (!player.isCreative()) {

            stack.shrink(1);
        }

        return InteractionResult.CONSUME;
    }
}