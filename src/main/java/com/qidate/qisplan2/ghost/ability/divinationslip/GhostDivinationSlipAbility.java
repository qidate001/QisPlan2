package com.qidate.qisplan2.ghost.ability.divinationslip;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.client.key.ModKeyMappings;
import com.qidate.qisplan2.ghost.GhostAbilityContext;
import com.qidate.qisplan2.ghost.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import net.minecraft.resources.ResourceLocation;

public final class GhostDivinationSlipAbility
        implements PossessedGhostAbility {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_divination_slip"
            );

    private static final GhostCorrosion CORROSION =
            GhostCorrosion.builder()
                    .add(CorrosionType.GLOBAL, 20)
                    .build();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public double initialIntrinsicStrength() {
        return 20.0D;
    }

    @Override
    public GhostCorrosion corrosion() {
        return CORROSION;
    }

    /**
     * 主动技能：
     *
     * 小键盘1/2/3。
     */
    @Override
    public boolean use(
            GhostAbilityContext context
    ) {

        var player =
                context.player();

        if (ModKeyMappings.GHOST_DIVINATION_LIFE.isDown()) {

            GhostDivinationSlipSystem.useLife(
                    player
            );

            return true;
        }

        if (ModKeyMappings.GHOST_DIVINATION_DEATH.isDown()) {

            GhostDivinationSlipSystem.useDeath(
                    player
            );

            return true;
        }

        if (ModKeyMappings.GHOST_DIVINATION_GHOST.isDown()) {

            GhostDivinationSlipSystem.useGhost(
                    player
            );

            return true;
        }

        return false;
    }
}