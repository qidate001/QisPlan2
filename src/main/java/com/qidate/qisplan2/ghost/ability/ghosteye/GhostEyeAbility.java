package com.qidate.qisplan2.ghost.ability.ghosteye;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.GhostAbilityContext;
import com.qidate.qisplan2.ghost.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
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
    public void tick(
            GhostAbilityContext context
    ) {

        GhostEyeSystem.tick(context);
    }

    @Override
    public void onRelease(
            GhostAbilityContext context
    ) {

        GhostEyeSystem.removeDomain(
                context.player()
        );
    }
}