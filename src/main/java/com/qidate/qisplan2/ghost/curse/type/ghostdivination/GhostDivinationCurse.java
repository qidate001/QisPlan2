package com.qidate.qisplan2.ghost.curse.type.ghostdivination;

import com.qidate.qisplan2.ghost.curse.CurseAbilityContext;

import com.qidate.qisplan2.ghost.module.influence.GhostAttackResponses;
import com.qidate.qisplan2.ghost.module.implementation.divinationslip.GhostDivinationSlipModule;
import com.qidate.qisplan2.ghost.curse.AbstractCurse;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceProvider;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public class GhostDivinationCurse
        extends AbstractCurse
        implements GhostInfluenceProvider {

    /**
     * 生签诅咒类型。
     */
    public static final ResourceLocation TYPE =
            GhostDivinationCurseType.ID;

    /**
     * 生签默认强度。
     */
    public static final int DEFAULT_STRENGTH = 50;

    /**
     * 灵异冲突式死机状态下的生签强度。
     */
    public static final int STUN_STRENGTH = 20;

    /**
     * 生签持续时间。
     */
    public static final int DEFAULT_DURATION =
            5 * 60 * 20;

    /**
     * 创建新的生签诅咒。
     */
    public GhostDivinationCurse(
            UUID target,
            GhostDivinationCurseSource source,
            int strength,
            int remainingTicks
    ) {
        this(
                UUID.randomUUID(),
                target,
                source,
                strength,
                remainingTicks
        );
    }

    /**
     * 从保存数据恢复生签诅咒。
     */
    public GhostDivinationCurse(
            UUID id,
            UUID target,
            GhostDivinationCurseSource source,
            int strength,
            int remainingTicks
    ) {
        super(
                id,
                GhostDivinationCurseType.ID,
                target,
                source,
                strength,
                remainingTicks
        );
    }

    /**
     * 增加剩余持续时间。
     *
     * @param ticks 增加的 Tick 数
     */
    public void addTime(
            int ticks
    ) {
        if (ticks <= 0) {
            return;
        }

        setRemainingTicks(
                getRemainingTicks() + ticks
        );
    }

    @Override
    public void tick(
            MinecraftServer server
    ) {
        // 持续时间由管理器统一推进，沉寂时也不会冻结寿命。
    }

    /** 持续诅咒只桥接模块声明，不要求原宿主仍在场，也不把保护挂在袭击者身上。 */
    @Override public GhostAttackResponses.Response attackResponse(Runnable remove) {
        return new GhostAttackResponses.Response(200,
                attack -> new GhostDivinationCurseType().program().dispatch(attack.event(),
                    new CurseAbilityContext(getId(),getTarget(),getStrength(),attack.event(),attack,remove,null)));
    }

    /** 生签保护来自鬼签模块，不能与鬼草丛等其他模块误判为同源死机。 */
    @Override public ResourceLocation influenceModuleId() { return GhostDivinationSlipModule.ID; }

    @Override
    public GhostInfluenceType getInfluenceType() {
        return GhostInfluenceType.PROTECTION;
    }

    @Override
    public double getInfluenceIntensity() {
        return getStrength();
    }
}
