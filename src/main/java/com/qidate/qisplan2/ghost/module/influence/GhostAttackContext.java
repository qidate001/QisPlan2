package com.qidate.qisplan2.ghost.module.influence;

import com.qidate.qisplan2.ghost.module.event.SupernaturalAttackEvent;
import java.util.function.DoubleSupplier;
import net.minecraft.resources.ResourceLocation;

/** 单次袭击的对抗上下文。只暴露语义事实及结算端口，不暴露任何宿主。 */
public final class GhostAttackContext {
    private final SupernaturalAttackEvent event;
    private final ResourceLocation sourceModule;
    private final Runnable deadlockSource;
    private final DoubleSupplier recentIntensity;
    private Double accumulated;

    /** 累计强度采用延迟查询：没有相关保护时，不读取或维护其历史。 */
    public GhostAttackContext(SupernaturalAttackEvent event, ResourceLocation sourceModule,
            Runnable deadlockSource, DoubleSupplier recentIntensity) {
        this.event = event;
        this.sourceModule = sourceModule;
        this.deadlockSource = deadlockSource;
        this.recentIntensity = recentIntensity;
    }

    /** 取得可逐层抵挡的本次袭击事实。 */
    public SupernaturalAttackEvent event() { return event; }

    /** 沿用现有同类灵异的同源语义；不把它冒充持久化模块实例身份。 */
    public boolean isSameNature(ResourceLocation module) { return module.equals(sourceModule); }

    /** 本次袭击只记录一次累计值；多个持续保护共享同一次记录。 */
    public double recentIntensity() {
        if (accumulated == null) accumulated = recentIntensity.getAsDouble();
        return accumulated;
    }

    /** 同源矛盾使来袭模块死机，并停止本次袭击；宿主写入由中间层完成。 */
    public void deadlock() { deadlockSource.run(); event.deadlock(); }
}
