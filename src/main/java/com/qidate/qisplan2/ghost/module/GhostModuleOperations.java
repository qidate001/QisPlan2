package com.qidate.qisplan2.ghost.module;

import net.minecraft.resources.ResourceLocation;

/** 命令、物品与外部系统共用的模块状态操作，不接受玩家或具体载体类型。 */
public final class GhostModuleOperations {
    public static final String REVIVAL_RESERVE = "revivalReserve";
    private GhostModuleOperations() {}
    /** 对指定实例设置沉睡，通过统一对抗状态保存，不创建旧临时死机。 */
    public static boolean sleep(GhostModuleHost host, ResourceLocation module, long ticks) {
        var entry=host.getModules().stream().filter(e->e.id().equals(module)).findFirst().orElse(null);
        if(entry==null)return false;
        GhostModuleCombatState.sleep(host,entry,Math.max(1,ticks));return true;
    }
    /** 复苏缓冲以百分比保存，随完整模块转移；对任意可变宿主采用同一上限。 */
    public static int addRevivalReserve(GhostModuleHost host,double amount) {
        if(!Double.isFinite(amount)||amount<=0||!(host instanceof GhostModuleCustomDataModifier modifier))return 0;
        int changed=0;
        for(var entry:host.getModules()) {
            if(!host.carriesModule(entry.id()))continue;
            var data=modifier.getModuleCustomData(entry.id());double old=data.getDouble(REVIVAL_RESERVE);
            double next=Math.clamp(old+amount,0,100);
            if(next!=old){data.putDouble(REVIVAL_RESERVE,next);modifier.setModuleCustomData(entry.id(),data);changed++;}
        }
        return changed;
    }
}
