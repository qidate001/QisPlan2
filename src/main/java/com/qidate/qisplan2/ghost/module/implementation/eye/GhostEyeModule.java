package com.qidate.qisplan2.ghost.module.implementation.eye;

import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.domain.type.eye.GhostEyeDomainController;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleContext;
import com.qidate.qisplan2.ghost.possession.classification.GhostClassification;
import com.qidate.qisplan2.ghost.possession.classification.GhostTag;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostDomainData;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class GhostEyeModule implements GhostModule {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("qisplan2", "ghost_eye");
    @Override public ResourceLocation getId() { return ID; }
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(100, 0); }


    private static final GhostCorrosion CORROSION =
            GhostCorrosion.builder()
                    .add(CorrosionType.EYE, 100)
                    .add(CorrosionType.GLOBAL, 40)
                    .build();


    /**
     * 鬼域。
     */
    @Override
    public Optional<PossessedGhostDomainData> initialDomainData() {
        return Optional.of(
                PossessedGhostDomainData.create(
                        initialStrength(),
                        80.0D
                )
        );
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
    public GhostClassification suppressionTargets() {

        return GhostClassification.of(
                GhostTag.ENTITY
        );
    }

    @Override
    public int suppressionUnits() {
        return 10;
    }

    @Override
    public double suppressionUnitStrength() {
        return 100.0D;
    }

    @Override
    public void onPlayerTick(
            PlayerGhostModuleContext context
    ) {

        GhostEyeDomainController.tick(context);
    }

    @Override
    public void onRelease(
            PlayerGhostModuleContext context
    ) {

        GhostEyeDomainController.close(context.player());
    }

}
