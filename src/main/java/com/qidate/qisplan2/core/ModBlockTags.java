package com.qidate.qisplan2.core;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModBlockTags {

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
}