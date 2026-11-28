package com.qidate.qisplan2.ghost.module;

import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

/**
 * 按模块实时强度协调共享宿主的行为控制。
 *
 * 仅比较控制标记重叠的行为。高强度达到低强度两倍，或差值至少 100 时，
 * 高强度模块可以持续占用；较弱模块仅在强者没有可执行的冲突行为时运行。
 * 未达到压倒门槛的同级行为按 100 tick 轮换。模块内部保留列表顺序。
 */
public final class GhostRuleExecution {
    private static final long MAX_RUN_TICKS = 100;
    private static final long YIELD_TICKS = 6;
    private final BooleanSupplier available;
    private final DoubleSupplier strength;
    private final LongSupplier clock;
    private final ResourceLocation moduleId;
    private final int order;
    private final GhostRuleTask delegate;
    private final Supplier<List<GhostRuleExecution>> peers;
    private long startedAt;
    private long eligibleAt;
    private long checkedAt = Long.MIN_VALUE;
    private boolean ready;
    private boolean running;
    private boolean yielded;

    /** 每个任务独立调度；三个事实端口由宿主适配器提供。 */
    public GhostRuleExecution(ResourceLocation moduleId, int order, GhostRuleTask delegate,
            BooleanSupplier available, DoubleSupplier strength, LongSupplier clock,
            Supplier<List<GhostRuleExecution>> peers) {
        this.moduleId=moduleId;this.order=order;this.delegate=delegate;
        this.available=available;this.strength=strength;this.clock=clock;this.peers=peers;
    }

    /** 严格要求强者更强，避免零对零也被当作两倍优势。 */
    public static boolean dominates(double stronger, double weaker) {
        return stronger > weaker && (stronger >= weaker * 2.0D || stronger - weaker >= 100.0D);
    }

    /** 实时读取模块强度，运行期间增强或减弱立即参与下一次仲裁。 */
    private double intensity() { return strength.getAsDouble(); }

    /**
     * 每个游戏刻只探测一次原行为，避免多个竞争者反复调用 canUse，
     * 造成找门重复扫描或行为内部冷却在一刻内减少多次。
     */
    private boolean hasDemand() {
        if (!available.getAsBoolean()) return false;
        long now = clock.getAsLong();
        if (checkedAt != now) {
            checkedAt = now;
            ready = running ? delegate.canContinueToUse() : delegate.canUse();
        }
        return ready;
    }

    private boolean conflicts(GhostRuleExecution other) {
        return other != this && delegate.getFlags().stream().anyMatch(other.delegate.getFlags()::contains);
    }

    /** 只有有实际执行需求的冲突行为才能阻止当前行为，不能仅凭持有强模块就封锁弱模块。 */
    private boolean isBlocked() {
        double own = intensity();
        for (var other : peers.get()) {
            if (!conflicts(other) || !other.hasDemand()) continue;
            if (moduleId.equals(other.moduleId)) {
                if (other.order < order) return true;
            } else {
                double theirs = other.intensity();
                if (dominates(theirs, own)) return true;
                if (!dominates(own, theirs) && other.order < order) return true;
            }
        }
        return false;
    }

    private boolean hasRotationPeer() {
        double own = intensity();
        return peers.get().stream().anyMatch(other -> conflicts(other)
                && !moduleId.equals(other.moduleId) && other.order == order
                && !dominates(own, other.intensity()) && !dominates(other.intensity(), own)
                && other.hasDemand());
    }

    /** 启动请求只做判定，不接管任何物理资源。 */
    public boolean canUse() {
        return clock.getAsLong() >= eligibleAt && hasDemand() && !isBlocked();
    }
    /** 运行状态、强度竞争和时间片共同决定能否继续。 */
    public boolean canContinueToUse() {
        // 强度运行中发生变化时，弱者释放控制；无需重装模块或重启实体。
        if (!hasDemand() || isBlocked()) return false;
        if (delegate.isInterruptable() && hasRotationPeer()
                && clock.getAsLong() - startedAt >= MAX_RUN_TICKS) {
            yielded = true;
            return false;
        }
        return true;
    }
    /** 取得资源后开始一轮任务，重复调用不会重复初始化。 */
    public void start() {
        if (running || !canUse()) return;
        running = true;
        yielded = false;
        checkedAt = Long.MIN_VALUE;
        startedAt = clock.getAsLong();
        delegate.start();
    }
    /** 强制中断也会执行清理，并且同一轮仅清理一次。 */
    public void stop() {
        if (!running) return;
        if (yielded || hasRotationPeer()) eligibleAt = clock.getAsLong() + YIELD_TICKS;
        running = false;
        delegate.stop();
        checkedAt = Long.MIN_VALUE;
    }
    /** 每次推进前重查可用状态，死机或压制后不再执行任务效果。 */
    public void tick() { if (!running) return; if (!available.getAsBoolean()) { stop(); return; } delegate.tick(); }
    /** 返回任务的正常抢占策略。 */
    public boolean isInterruptable() { return delegate.isInterruptable(); }
    /** 返回任务请求的调度频率。 */
    public boolean requiresUpdateEveryTick() { return delegate.requiresUpdateEveryTick(); }
}
