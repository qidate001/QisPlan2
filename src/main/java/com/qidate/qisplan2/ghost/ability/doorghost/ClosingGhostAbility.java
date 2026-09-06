package com.qidate.qisplan2.ghost.ability.doorghost;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModEntities;
import com.qidate.qisplan2.entity.AbstractGhostEntity;
import com.qidate.qisplan2.ghost.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public final class ClosingGhostAbility
        implements PossessedGhostAbility {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "closing_ghost"
            );

    private static final GhostCorrosion CORROSION =
            GhostCorrosion.builder()
                    .add(CorrosionType.GLOBAL, 10)
                    .add(CorrosionType.HAND, 20)
                    .build();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public EntityType<? extends AbstractGhostEntity> entityType() {

        return ModEntities.CLOSING_GHOST.get();
    }

    @Override
    public double initialIntrinsicStrength() {
        return 5.0D;
    }

    @Override
    public double minimumStrengthRatio() {
        return 1.0D / 3.0D;
    }

    @Override
    public GhostCorrosion corrosion() {
        return CORROSION;
    }
}