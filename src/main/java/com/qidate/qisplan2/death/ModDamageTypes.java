package com.qidate.qisplan2.death;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

public class ModDamageTypes {

    /**
     * 鬼地毯诅咒
     */
    public static final ResourceKey<DamageType> GHOST_CARPET =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_carpet"
                    )
            );



    public static final ResourceKey<DamageType> DEATH_CURSE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "death_curse"
                    )
            );


    /**
     * 创建鬼地毯死亡 DamageSource
     */
    public static DamageSource ghostCarpet(Entity entity) {

        return new DamageSource(
                entity.level()
                        .registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(GHOST_CARPET),

                entity
        );
    }

    public static DamageSource deathCurse(Entity entity) {

        return new DamageSource(
                entity.level()
                        .registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(DEATH_CURSE),
                entity
        );
    }
}