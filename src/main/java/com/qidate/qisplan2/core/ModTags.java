package com.qidate.qisplan2.core;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {

    private ModTags() {
    }

    public static final class Items {

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
}
