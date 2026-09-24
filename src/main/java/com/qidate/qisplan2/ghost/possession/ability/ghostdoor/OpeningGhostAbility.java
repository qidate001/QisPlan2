package com.qidate.qisplan2.ghost.possession.ability.ghostdoor;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModEntities;
import com.qidate.qisplan2.entity.AbstractGhostEntity;
import com.qidate.qisplan2.ghost.possession.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.possession.classification.GhostClassification;
import com.qidate.qisplan2.ghost.possession.classification.GhostTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public final class OpeningGhostAbility
        implements PossessedGhostAbility {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "opening_ghost"
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

        return ModEntities.OPENING_GHOST.get();
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

    @Override
    public GhostClassification classification() {

        return GhostClassification.of(
                GhostTag.ENTITY,
                GhostTag.EXISTENCE_FORM,
                GhostTag.DOOR
        );
    }

    @Override
    public GhostClassification suppressionTargets() {
        return GhostClassification.empty();
    }

    @Override
    public int suppressionUnits() {
        return 1;
    }

    @Override
    public double suppressionUnitStrength() {
        return 20.0D;
    }
}