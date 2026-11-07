package com.qidate.qisplan2.entity.ai;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.List;
import java.util.function.Supplier;

/**
 * 按模块实时强度协调共享宿主的行为控制。
 *
 * 仅比较控制标记重叠的行为。高强度达到低强度两倍，或差值至少 100 时，
 * 高强度模块可以持续占用；较弱模块仅在强者没有可执行的冲突行为时运行。
 * 未达到压倒门槛的同级行为仍按 100 tick 轮换。模块内部保留列表顺序。
 */
public final class ModuleScheduledGoal extends Goal {
    private static final long MAX_RUN_TICKS = 100;
    private static final long YIELD_TICKS = 6;
    private final ModularGhostEntity host;
    private final ResourceLocation moduleId;
    private final int order;
    private final Goal delegate;
    private final Supplier<List<ModuleScheduledGoal>> peers;
    private long startedAt;
    private long eligibleAt;
    private long checkedAt = Long.MIN_VALUE;
    private boolean ready;
    private boolean running;
    private boolean yielded;

    public ModuleScheduledGoal(ModularGhostEntity host, ResourceLocation moduleId, int order,
            Goal delegate, Supplier<List<ModuleScheduledGoal>> peers) {
        this.host = host;
        this.moduleId = moduleId;
        this.order = order;
        this.delegate = delegate;
        this.peers = peers;
        setFlags(delegate.getFlags());
    }

    /** 严格要求强者更强，避免零对零也被当作两倍优势。 */
    public static boolean dominates(double stronger, double weaker) {
        return stronger > weaker && (stronger >= weaker * 2.0D || stronger - weaker >= 100.0D);
    }

    private double intensity() {
        return host.getModules().stream().filter(e -> e.id().equals(moduleId))
                .mapToDouble(e -> e.intensity()).findFirst().orElse(0);
    }

    /**
     * 每个游戏刻只探测一次原行为，避免多个竞争者反复调用 canUse，
     * 造成找门重复扫描或行为内部冷却在一刻内减少多次。
     */
    private boolean hasDemand() {
        if (!host.isAlive() || host.isSupernaturallyStunned() || host.isCoffinNailed()
                || host.getModules().stream().noneMatch(e -> e.id().equals(moduleId)
                        && !e.sleeping() && !e.deadlocked())) return false;
        long now = host.level().getGameTime();
        if (checkedAt != now) {
            checkedAt = now;
            ready = running ? delegate.canContinueToUse() : delegate.canUse();
        }
        return ready;
    }

    private boolean conflicts(ModuleScheduledGoal other) {
        return other != this && getFlags().stream().anyMatch(other.getFlags()::contains);
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

    @Override public boolean canUse() {
        return host.level().getGameTime() >= eligibleAt && hasDemand() && !isBlocked();
    }
    @Override public boolean canContinueToUse() {
        // 强度运行中发生变化时，弱者释放控制；无需重装模块或重启实体。
        if (!hasDemand() || isBlocked()) return false;
        if (delegate.isInterruptable() && hasRotationPeer()
                && host.level().getGameTime() - startedAt >= MAX_RUN_TICKS) {
            yielded = true;
            return false;
        }
        return true;
    }
    @Override public void start() {
        running = true;
        yielded = false;
        checkedAt = Long.MIN_VALUE;
        startedAt = host.level().getGameTime();
        delegate.start();
    }
    @Override public void stop() {
        if (yielded || hasRotationPeer()) eligibleAt = host.level().getGameTime() + YIELD_TICKS;
        running = false;
        delegate.stop();
        checkedAt = Long.MIN_VALUE;
    }
    @Override public void tick() { delegate.tick(); }
    @Override public boolean isInterruptable() { return delegate.isInterruptable(); }
    @Override public boolean requiresUpdateEveryTick() { return delegate.requiresUpdateEveryTick(); }
}
