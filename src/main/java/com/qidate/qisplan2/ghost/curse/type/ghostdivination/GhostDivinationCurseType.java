package com.qidate.qisplan2.ghost.curse.type.ghostdivination;

import com.qidate.qisplan2.ghost.module.implementation.divinationslip.GhostDivinationSlipModule;
import com.qidate.qisplan2.ghost.module.event.SupernaturalSuppressionEvent;
import com.qidate.qisplan2.ghost.module.component.GhostComponentProgram;
import com.qidate.qisplan2.ghost.module.event.SupernaturalAttackEvent;
import com.qidate.qisplan2.ghost.curse.CurseAbilityContext;
import java.util.List;

import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseSource;
import com.qidate.qisplan2.ghost.curse.CurseType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class GhostDivinationCurseType
        implements CurseType {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "ghost_divination"
            );

    private static final ResourceLocation PROTECT=ResourceLocation.fromNamespaceAndPath("qisplan2","life_curse/protect");
    private static final GhostComponentProgram<CurseAbilityContext> PROGRAM = new GhostComponentProgram<>(
        List.of(new GhostComponentProgram.Ability<>(PROTECT,c->c.incoming()!=null&&c.incoming().event().remainingIntensity()>0,
            c->GhostDivinationSlipModule.resolveLifeProtection(c.incoming(),c.strength(),c.remove()))),
        List.of(new GhostComponentProgram.Rule<>(SupernaturalAttackEvent.class,c->true,PROTECT),
            new GhostComponentProgram.Rule<>(SupernaturalSuppressionEvent.class,c->true,PROTECT)));
    /** HUD 图标也随模块定义提供。 */
    @Override public ResourceLocation icon(){return ResourceLocation.fromNamespaceAndPath("qisplan2","textures/item/life_sign.png");}
    /** 生签只有受到袭击或压制才调用保护能力；持续时间仍由生命周期推进。 */
    @Override public GhostComponentProgram<CurseAbilityContext> program(){return PROGRAM;}

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Curse create(
            UUID target,
            CurseSource source,
            CompoundTag initialState
    ) {

        int strength =
                initialState.contains("Strength")
                        ? initialState.getInt("Strength")
                        : GhostDivinationCurse.DEFAULT_STRENGTH;

        int remainingTicks =
                initialState.contains("RemainingTicks")
                        ? initialState.getInt("RemainingTicks")
                        : GhostDivinationCurse.DEFAULT_DURATION;

        return new GhostDivinationCurse(
                target,
                (GhostDivinationCurseSource) source,
                strength,
                remainingTicks
        );
    }

    @Override
    public Curse load(
            CompoundTag tag
    ) {

        UUID id =
                tag.getUUID("Id");

        UUID target =
                tag.getUUID("Target");

        GhostDivinationCurseSource source =
                GhostDivinationCurseSource.load(
                        tag.getCompound("Source")
                );

        int strength =
                tag.getInt("Strength");

        int remainingTicks =
                tag.getInt("RemainingTicks");

        return new GhostDivinationCurse(
                id,
                target,
                source,
                strength,
                remainingTicks
        );
    }
}
