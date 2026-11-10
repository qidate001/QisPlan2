package com.qidate.qisplan2.item;

import com.qidate.qisplan2.ghost.module.host.item.GhostModuleItemSupport;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;

/**
 * 通用灵异白板剑。
 *
 * 默认不携带模块，用于承载后续安装的厉鬼模块。
 * 保留 SwordItem 的原版剑行为，模块交互通过共用接口提供。
 *
 * 每把剑的模块数据保存在各自 ItemStack 的 Data Components 中。
 * 右键使用时，创建临时模块宿主适配器，并分发 GhostUseEvent。
 *
 * 本类不关心剑上具体安装了什么模块，
 * 具体行为由模块自身的规则决定。
 */
public class GhostBlankSwordItem extends SwordItem implements GhostModuleItemSupport {

    public GhostBlankSwordItem(Properties properties) {
        super(
                Tiers.IRON,
                properties.attributes(
                        SwordItem.createAttributes(
                                Tiers.IRON,
                                3.0F,
                                -2.4F
                        )
                )
        );
    }

    /**
     * 右键使用白板剑时，触发通用灵异使用事件。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        return useModules(level, player, hand);
    }
}