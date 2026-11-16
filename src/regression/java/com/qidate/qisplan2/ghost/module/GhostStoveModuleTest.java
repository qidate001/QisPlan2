package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.data.GhostWorkstationData;
import com.qidate.qisplan2.ghost.module.environment.GhostWorkstationAccess;
import com.qidate.qisplan2.ghost.module.environment.GhostWorldAccess;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.implementation.GhostStoveModule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.util.List;

/** 不创建具体载体，验证范围查询、死机边界、独立工作台能力及旧库存迁移。 */
public final class GhostStoveModuleTest {
    private static final class Host implements GhostModuleHost {
        boolean deadlocked, sleeping, removed;
        boolean exposed = true;
        boolean usable = true;
        int queries, maintenance, openings;
        /** 用普通条目模拟模块状态，不提供玩家或方块身份。 */
        public List<GhostModuleData.Entry> getModules() {
            return removed ? List.of() : List.of(new GhostModuleData.Entry(GhostStoveModule.ID,1,0,sleeping,deadlocked,new CompoundTag()));
        }
        /** 捕获真实模块对端口的访问，任何意外访问直接失败。 */
        public GhostWorldAccess moduleWorld(ResourceLocation id) {
            var workstation = new GhostWorkstationAccess() {
                /** 适配器决定是否满足交互媒介。 */
                public boolean canOpen(ResourceLocation type, LivingEntity user) { return usable && exposed && !removed; }
                /** 记录公共能力是否实际打开工作台。 */
                public boolean open(ResourceLocation type, LivingEntity user) {
                    check(type.equals(GhostStoveModule.ID), "工作台类型声明错误");
                    if (!canOpen(type,user)) return false; openings++; return true;
                }
                /** 持续能力维护应与自动袭击规则解耦。 */
                public void tick(ResourceLocation type) { maintenance++; }
            };
            return (GhostWorldAccess) Proxy.newProxyInstance(GhostWorldAccess.class.getClassLoader(),
                    new Class<?>[]{GhostWorldAccess.class}, (proxy,method,args) -> switch (method.getName()) {
                        case "present" -> exposed;
                        case "workstations" -> workstation;
                        case "livingEntities" -> {
                            check(args[0].equals(GhostStoveModule.AREA), "规则应声明同一个相对 AABB");
                            queries++; yield List.of();
                        }
                        case "aimedTarget" -> null;
                        default -> {
                            if (method.isDefault()) yield InvocationHandler.invokeDefault(proxy,method,args);
                            throw new AssertionError("Unexpected port: " + method.getName());
                        }
                    });
        }
    }
    /** 断言失败时提供具体回归原因。 */
    private static void check(boolean condition,String message) { if (!condition) throw new AssertionError(message); }
    /** 走真实的主动能力执行器，而非直接调用效果。 */
    private static boolean invoke(Host host,ResourceLocation ability) {
        return GhostAbilityExecutor.tryExecuteWithResult(new GhostAbilityContext(host,GhostStoveModule.ID,ability,
                GhostAbilityContext.Source.POSSESSION,null,() -> {},new GhostInvocationParameters(null)));
    }
    /** 验证一轮查询、能力分离、存档复制与编解码。 */
    public static void run() {
        if (!GhostModuleRegistry.contains(GhostStoveModule.ID)) GhostModuleRegistry.register(new GhostStoveModule());
        var host=new Host();
        GhostModuleRuntime.dispatch(host,new ModuleTickEvent());
        check(host.queries == 1 && host.maintenance == 1, "每次周期仅一次局部实体查询，并维护工作台");
        host.deadlocked=true; GhostModuleRuntime.dispatch(host,new ModuleTickEvent());
        check(host.queries == 1 && host.maintenance == 2, "死机停止袭击查询，但不停止工作台能力维护");
        check(invoke(host,GhostStoveModule.WORKSTATION) && host.openings == 1, "死机仍允许媒介有效的工作台技能");
        check(!invoke(host,GhostStoveModule.ATTACK), "无携食目标不能因主动释放而绕过媒介");
        host.usable=false; check(!invoke(host,GhostStoveModule.WORKSTATION), "适配器拒绝交互时不能打开菜单");
        host.deadlocked=false; host.sleeping=true; GhostModuleRuntime.dispatch(host,new ModuleTickEvent());
        check(host.queries == 1, "沉睡同样不得自动扫描目标");
        host.sleeping=false; host.exposed=false; GhostModuleRuntime.dispatch(host,new ModuleTickEvent());
        check(host.queries == 1, "未暴露到世界的来源不能扫描实体");
        int before=host.maintenance; host.removed=true; GhostModuleRuntime.dispatch(host,new ModuleTickEvent());
        check(host.maintenance == before, "模块移除后不能继续维护工作台");
        var another=new Host(); check(GhostAbilityCatalog.all(another).size() == 2, "独立公开袭击与工作台能力");
        var translated=GhostStoveModule.AREA.move(10,20,30);
        check(translated.intersects(new AABB(14.9,20,30,15.1,21,31)), "范围按目标碰撞箱相交判断边界");
        check(!translated.intersects(new AABB(15.1,20,30,16,21,31)), "范围外目标不能命中");
        var old=new CompoundTag(); var items=new ListTag();
        for (int i=0;i<6;i++) {
            var slot=new CompoundTag(); slot.putByte("Slot",(byte)i); slot.putString("id","minecraft:bread"); slot.putInt("count",i+1); items.add(slot);
        }
        old.put("Items",items); old.putInt("CookTime",73); old.putInt("CookTimeTotal",200); old.putInt("x",999);
        var custom=GhostWorkstationData.fromLegacy(old);
        var state=custom.getCompound(GhostWorkstationData.KEY);
        check(state.getList("Items",10).size() == 6 && state.getInt("CookTime") == 73
                && state.getInt("CookTimeTotal") == 200 && !state.contains("x"), "旧六格库存与进度完整迁移，载体坐标不迁入");
        items.clear(); check(state.getList("Items",10).size() == 6, "迁移数据必须独立复制，不能共享旧 NBT 引用");
        var entries=List.of(new GhostModuleData.Entry(GhostStoveModule.ID,1,0,false,true,custom));
        var encoded=GhostModuleData.CODEC.encodeStart(NbtOps.INSTANCE,entries).getOrThrow();
        var decoded=GhostModuleData.CODEC.parse(NbtOps.INSTANCE,encoded).getOrThrow().getFirst();
        check(decoded.deadlocked() && decoded.customData().equals(custom), "模块存档与转移必须保留工作台数据及死机状态");
        System.out.println("Ghost stove spatial query, workstation and migration regression checks passed");
    }
}
