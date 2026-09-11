package com.qidate.qisplan2.ghost.ability.divinationslip;

import com.qidate.qisplan2.QisPlan2;
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

    /*
     * 鬼签的灵异侵蚀。
     *
     * 这里先给一个基础值，
     * 后续可以按照正式设定调整。
     */
    private static final GhostCorrosion CORROSION =
            GhostCorrosion.builder()
                    .add(CorrosionType.GLOBAL, 20)
                    .build();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /**
     * 鬼签初始本质强度。
     */
    @Override
    public double initialIntrinsicStrength() {
        return 20.0D;
    }

    /**
     * 鬼签当前没有主动能力。
     */
    @Override
    public boolean use(
            GhostAbilityContext context
    ) {
        return false;
    }

    /**
     * 鬼签没有方块主动能力。
     */
    @Override
    public boolean useOnBlock(
            GhostAbilityContext context,
            net.minecraft.core.BlockPos pos
    ) {
        return false;
    }

    @Override
    public GhostCorrosion corrosion() {
        return CORROSION;
    }
}