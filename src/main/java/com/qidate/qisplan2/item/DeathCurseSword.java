package com.qidate.qisplan2.item;

import net.minecraft.tags.TagKey;
import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.host.item.GhostModuleItemSupport;
import com.qidate.qisplan2.ghost.module.implementation.DeathCurseSwordModule;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public class DeathCurseSword extends SwordItem implements GhostModuleItemSupport {

    private static final Tier DEATH_CURSE_TIER = new Tier() {

        @Override
        public int getUses() {
            return 15;
        }

        @Override
        public float getSpeed() {
            return Tiers.WOOD.getSpeed();
        }

        @Override
        public float getAttackDamageBonus() {
            return Tiers.WOOD.getAttackDamageBonus();
        }

        @Override
        public int getEnchantmentValue() {
            return Tiers.WOOD.getEnchantmentValue();
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return Tiers.WOOD.getIncorrectBlocksForDrops();
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Tiers.WOOD.getRepairIngredient();
        }
    };

    /** 保留旧剑材质和十五点耐久，新增默认完整厉鬼模块。 */
    public DeathCurseSword(Properties properties) {
        super(DEATH_CURSE_TIER, properties.component(ModDataComponents.GHOST_MODULES.get(),
                List.of(new GhostModuleData.Entry(DeathCurseSwordModule.ID,3))));
    }
    /** 使用与驾驭通过通用模块入口处理。 */
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        return useModules(level,player,hand);
    }
    /** 周期事件与物品上的所有模块共用，不再在物品中写诅咒逻辑。 */
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected) {
        super.inventoryTick(stack,level,entity,slot,selected); tickModules(stack,level,entity);
    }
}