package com.qidate.qisplan2.ghost.possession.ability.ghosteye;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.possession.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.possession.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.domain.type.eye.GhostEyeDomainController;
import com.qidate.qisplan2.ghost.possession.classification.GhostClassification;
import com.qidate.qisplan2.ghost.possession.classification.GhostTag;
import net.minecraft.resources.ResourceLocation;

public final class GhostEyeAbility
        implements PossessedGhostAbility {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_eye"
            );

    private static final GhostCorrosion CORROSION =
            GhostCorrosion.builder()
                    .add(CorrosionType.EYE, 100)
                    .add(CorrosionType.GLOBAL, 40)
                    .build();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public double initialIntrinsicStrength() {
        return 200.0D;
    }

    @Override
    public double minimumStrengthRatio() {
        return 0.50D;
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
                GhostTag.ORGAN,
                GhostTag.EYE
        );
    }

    @Override
    public void tick(
            GhostAbilityContext context
    ) {

        GhostEyeDomainController.tick(context);
    }

    @Override
    public void onRelease(
            GhostAbilityContext context
    ) {

        GhostEyeDomainController.close(context.player());
    }
}