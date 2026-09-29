package com.qidate.qisplan2.core;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {

    private ModTags() {
    }

    public static final class Items {

        /**
         * 可以在鬼墓碑上刻字的灵异物品。
         */
        public static final TagKey<Item> GHOST_TOMBSTONE_INSCRIBABLE =
                TagKey.create(
                        Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath(
                                QisPlan2.MODID,
                                "ghost_tombstone_inscribable"
                        )
                );

        private Items() {
        }
    }

    public static final class Blocks {

        /**
         * 棺材钉镐子不能正确挖掘的方块。
         *
         * 这里故意留空。
         *
         * 这意味着：
         * 只要方块本身属于镐子可挖掘类型，
         * 就不会因为“挖掘等级不够”而无法掉落。
         */
        public static final TagKey<Block> INCORRECT_FOR_COFFIN_NAIL_TOOL =
                TagKey.create(
                        Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath(
                                QisPlan2.MODID,
                                "incorrect_for_coffin_nail_tool"
                        )
                );

        /**
         * 是否属于可以被墓碑诅咒拉入的自然土质。
         */
        public static final TagKey<Block> GHOST_TOMBSTONE_BURIAL =
                TagKey.create(
                        Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath(
                                QisPlan2.MODID,
                                "ghost_tombstone_burial"
                        )
                );

        private Blocks() {
        }
    }

    public static final class DamageTypes {

        /**
         * QisPlan2 灵异伤害
         */
        public static final TagKey<DamageType> SUPERNATURAL =
                TagKey.create(
                        Registries.DAMAGE_TYPE,
                        ResourceLocation.fromNamespaceAndPath(
                                QisPlan2.MODID,
                                "supernatural"
                        )
                );
    }
}