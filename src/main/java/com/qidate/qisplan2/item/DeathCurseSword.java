package com.qidate.qisplan2.item;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public class DeathCurseSword extends SwordItem {

    private static final Tier DEATH_CURSE_TIER = new Tier() {

        @Override
        public int getUses() {
            return 20;
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

    public DeathCurseSword(Properties properties) {
        super(DEATH_CURSE_TIER, properties);
    }
}