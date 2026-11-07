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

/**
 * 鬼眼模块的最小实现。
 *
 * 目前只提供玩家主动开启、关闭鬼域，以及鬼域运行时的维护与清理。
 * 不声明语义事件、触发规则或额外袭击能力，后续完善鬼眼规则时再扩展。
 * 鬼域持久状态通过玩家宿主写入模块 customData，控制器只负责鬼域操作。
 */
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

    /**
     * 玩家主动请求切换鬼域。请求由统一能力入口校验。
     * 沿用原有控制器，因此鬼域表现与持久数据格式保持一致。
     */
    @Override
    public boolean use(PlayerGhostModuleContext context) {
        if (context.target() != null) return false;
        if (GhostEyeDomainController.isOpen(context.player())) {
            GhostEyeDomainController.close(context.player());
            return true;
        }
        GhostEyeDomainController.open(context.player());
        return GhostEyeDomainController.isOpen(context.player());
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
