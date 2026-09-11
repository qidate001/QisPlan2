package com.qidate.qisplan2.ghost.ability.divinationslip;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.client.key.ModKeyMappings;
import com.qidate.qisplan2.ghost.GhostAbilityContext;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.ghost.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

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

    private static final double REVIVAL_GAIN = 20.0D;

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
     * 主动技能
     */
    public static void useResult(
            ServerPlayer player,
            int result
    ) {
        // 增长复苏值
        addRevival(player);

        // 执行能力
        switch (result) {
            case 0 ->
                    GhostDivinationSlipSystem.useLife(
                            player
                    );

            case 1 ->
                    GhostDivinationSlipSystem.useDeath(
                            player
                    );

            case 2 ->
                    GhostDivinationSlipSystem.useGhost(
                            player
                    );

            default -> {
            }
        }
    }

    public static void addRevival(
            ServerPlayer player
    ) {

        PossessedGhostState state =
                PossessionHandler.getState(
                        player,
                        ID
                );

        if (state == null) {
            return;
        }

        PossessedGhostState newState =
                PossessionHandler.addRevival(
                        player,
                        ID,
                        state,
                        REVIVAL_GAIN,
                        CorrosionType.GLOBAL
                );

        PossessionHandler.setState(
                player,
                ID,
                newState
        );
    }

    @Override
    public boolean use(
            GhostAbilityContext context
    ) {
        return false;
    }
}