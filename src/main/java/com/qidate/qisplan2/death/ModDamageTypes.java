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



    public static final ResourceKey<DamageType> GHOST_STONE_BRICKS =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_stone_bricks"
                    )
            );

    /**
     * 鬼灶台诅咒
     */
    public static final ResourceKey<DamageType> GHOST_STOVE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_stove"
                    )
            );

    /**
     * 鬼草丛诅咒
     */
    public static final ResourceKey<DamageType> GHOST_GRASS =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_grass"
                    )
            );


    /**
     * 夜游鬼灵异攻击
     */
    public static final ResourceKey<DamageType> GHOST_NIGHT_WANDERER =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_night_wanderer"
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

    public static DamageSource ghostStoneBricks(
            Entity entity
    ) {
        return new DamageSource(
                entity.level()
                        .registryAccess()
                        .registryOrThrow(
                                Registries.DAMAGE_TYPE
                        )
                        .getHolderOrThrow(
                                GHOST_STONE_BRICKS
                        ),
                entity
        );
    }

    /**
     * 鬼钢琴灵异攻击
     */
    public static final ResourceKey<DamageType> GHOST_PIANO =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_piano"
                    )
            );


    /**
     * 创建鬼灶台死亡 DamageSource
     */
    public static DamageSource ghostStove(Entity entity) {

        return new DamageSource(
                entity.level()
                        .registryAccess()
                        .registryOrThrow(
                                Registries.DAMAGE_TYPE
                        )
                        .getHolderOrThrow(
                                GHOST_STOVE
                        ),
                entity
        );
    }

    /**
     * 创建鬼草丛死亡 DamageSource
     */
    public static DamageSource ghostGrass(Entity entity) {

        return new DamageSource(
                entity.level()
                        .registryAccess()
                        .registryOrThrow(
                                Registries.DAMAGE_TYPE
                        )
                        .getHolderOrThrow(
                                GHOST_GRASS
                        ),
                entity
        );
    }

    /**
     * 创建夜游鬼灵异攻击 DamageSource
     */
    public static DamageSource ghostNightWanderer(
            Entity entity
    ) {
        return new DamageSource(
                entity.level()
                        .registryAccess()
                        .registryOrThrow(
                                Registries.DAMAGE_TYPE
                        )
                        .getHolderOrThrow(
                                GHOST_NIGHT_WANDERER
                        ),
                entity
        );
    }

    /**
     * 创建鬼钢琴死亡 DamageSource
     */
    public static DamageSource ghostPiano(
            Entity entity
    ) {
        return new DamageSource(
                entity.level()
                        .registryAccess()
                        .registryOrThrow(
                                Registries.DAMAGE_TYPE
                        )
                        .getHolderOrThrow(
                                GHOST_PIANO
                        ),
                entity
        );
    }
}