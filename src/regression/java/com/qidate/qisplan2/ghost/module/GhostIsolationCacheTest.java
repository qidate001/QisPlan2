package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.isolation.GhostIsolationCuboid;
import com.qidate.qisplan2.ghost.isolation.GhostIsolationRegion;
import com.qidate.qisplan2.ghost.isolation.IsolationState;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** 检查隔绝缓存的修复证明，不创建世界、不模拟洪水搜索。 */
public final class GhostIsolationCacheTest {
    private GhostIsolationCacheTest() {}
    /** 创建一块已有封闭检测证明的空间。 */
    private static GhostIsolationRegion region() {
        var min = BlockPos.ZERO; var max = new BlockPos(2,2,2);
        return new GhostIsolationRegion(UUID.randomUUID(), Level.OVERWORLD, min, max, min,
                List.of(new GhostIsolationCuboid(min,max)), IsolationState.ISOLATED);
    }
    /** 任一边界不满足时让构建失败。 */
    private static void check(boolean ok, String reason) { if (!ok) throw new AssertionError(reason); }
    /** 两个缺口、内部几何变化、存档恢复和未知失效均走实际缓存方法。 */
    public static void run() {
        var first = new BlockPos(-1,1,1); var second = new BlockPos(3,1,1);
        var r = region();
        check(!r.updateIsolationAt(first,true), "隔绝材料互换不应产生脏状态");
        check(r.updateIsolationAt(first,false) && r.canRestoreFromChanges(), "首个缺口必须失效且保留修复基线");
        check(r.updateIsolationAt(second,false), "已脏区域仍须记录第二个缺口");
        check(!r.updateIsolationAt(second,false), "重复通知不应重复计数");
        check(r.updateIsolationAt(first,true) && r.getState()==IsolationState.DIRTY, "只修一个缺口不能恢复");
        check(!r.updateIsolationAt(new BlockPos(-1,-1,-1),true), "对角位置不属于六面边界");
        var loaded = GhostIsolationRegion.load(r.save());
        check(loaded != null && loaded.canRestoreFromChanges(), "重载必须保留剩余缺口");
        check(loaded.updateIsolationAt(second,true) && loaded.getState()==IsolationState.ISOLATED,
                "最后缺口修复后直接恢复原空间");
        check(loaded.getId().equals(r.getId()) && loaded.containsExact(Level.OVERWORLD,BlockPos.ZERO),
                "快速恢复保留区域身份与几何");
        r = region();
        check(r.updateIsolationAt(BlockPos.ZERO,true), "内部添加隔绝墙改变几何，不能继续使用原空间");
        r.updateIsolationAt(first,false); r.updateIsolationAt(first,true);
        check(r.getState()==IsolationState.DIRTY, "补回外墙不能掩盖内部几何变化");
        check(r.updateIsolationAt(BlockPos.ZERO,false) && r.getState()==IsolationState.ISOLATED, "撤销内部变化也可恢复");
        r.updateIsolationAt(first,false); r.setState(IsolationState.DIRTY);
        check(!r.updateIsolationAt(first,true) && !r.canRestoreFromChanges(), "批量失效不能靠单点修复恢复");
        var old = region().save(); old.putString("State","DIRTY"); old.remove("RepairTracked"); old.remove("ChangedIsolation");
        var legacy = GhostIsolationRegion.load(old);
        check(legacy != null && !legacy.updateIsolationAt(first,true), "旧脏缓存没有证明时必须重新检测");
        System.out.println("Isolation repair cache, geometry and persistence checks passed");
    }
}
