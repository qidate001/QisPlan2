package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.module.domain.GhostDomainSettings;
import com.qidate.qisplan2.ghost.domain.*;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainDefinition;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;

/** 鬼域的统一宿主适配器。模块只声明能力，实体、物品和方块的位置差异封闭在这里。 */
public final class ModuleGhostDomains {
    // 弱键和弱值都不延长宿主或世界生命周期；实际鬼域仍由世界管理器持有。
    private static final Map<Object, Map<ResourceLocation, WeakReference<GhostDomain>>> INDEX = new WeakHashMap<>();
    // 没有实体 UUID 的静态宿主也必须共用来源身份，不能为每个模块各造一个来源。
    private static final Map<Object,UUID> STATIC_SOURCES=new WeakHashMap<>();
    private ModuleGhostDomains() {}

    /** 物理来源已有 UUID 时直接复用；静态来源按实际宿主分配生命周期内稳定的身份。 */
    private static UUID sourceId(GhostModuleHost host) {
        var bearer=GhostEnvironmentAdapters.bearer(host);
        return bearer==null?STATIC_SOURCES.computeIfAbsent(key(host),ignored->UUID.randomUUID()):bearer.getUUID();
    }

    /** 临时玩家适配器共享实际玩家身份，物品则按物品栈身份独立。 */
    private static Object key(GhostModuleHost host) {
        if(host instanceof GhostItemModuleHost item)return item.stack();
        var bearer = GhostEnvironmentAdapters.bearer(host);
        return bearer == null ? host : bearer;
    }

    /** 精确释放被删除的域，旧维度的清理不能撤销新维度已经重建的域。 */
    public static void forget(GhostModuleHost host,ResourceLocation id,GhostDomain domain) {
        var entries=INDEX.get(key(host));
        if(entries==null)return;
        var reference=entries.get(id);
        if(reference!=null && reference.get()==domain)entries.remove(id);
        if(entries.isEmpty())INDEX.remove(key(host));
    }

    /** 解析真实服务端世界；没有世界的纯数据宿主不能展开鬼域。 */
    public static ServerLevel level(GhostModuleHost host) {
        var bearer = GhostEnvironmentAdapters.bearer(host);
        if (bearer != null && bearer.level() instanceof ServerLevel server) return server;
        return host instanceof BlockEntity block && block.getLevel() instanceof ServerLevel server ? server : null;
    }

    /** 根据物品栈身份区分同一个携带者手中的多个同种鬼域。 */
    public static boolean same(GhostModuleHost a, GhostModuleHost b) {
        if (a instanceof GhostItemModuleHost x && b instanceof GhostItemModuleHost y) return x.stack() == y.stack() && x.carrier() == y.carrier();
        return a.equals(b);
    }

    /** 查询模块自己的运行时鬼域，不会借用携带者或另一物品上的同名模块。 */
    public static GhostDomain current(GhostModuleHost host, ResourceLocation id) {
        var level = level(host);
        if (level == null) return null;
        var entries=INDEX.get(key(host));
        var reference=entries==null ? null : entries.get(id);
        var domain=reference==null ? null : reference.get();
        return domain!=null && domain.getDimension().equals(level.dimension())
                && domain.getBehavior() instanceof ModuleGhostDomainBehavior behavior && behavior.belongsTo(host) ? domain : null;
    }

    /** 默认等级作为成长下限，未声明成长的特殊鬼域维持原行为。 */
    public static int minGrade(GhostModuleHost host, ResourceLocation id) {
        var module=GhostModuleRegistry.get(id);
        return module==null?1:module.domainDefinition().map(GhostDomainDefinition::minGrade).orElse(1);
    }

    /** 只读取模块基础强度，不能用存档 domainStrength 或局部对抗强度计算成长。 */
    public static int maxGrade(GhostModuleHost host, ResourceLocation id) {
        var module=GhostModuleRegistry.get(id);
        if(module==null||module.domainDefinition().isEmpty())return 0;
        var entry=host.getModules().stream().filter(e->e.id().equals(id)).findFirst().orElse(null);
        return entry==null?0:module.domainDefinition().orElseThrow().maxGrade(entry.intensity());
    }

    /** 有效展开必须满足承载、压制、沉睡和物理暴露条件。死机只关闭规则，不关闭已启动能力。 */
    public static boolean enabled(GhostModuleHost host, ResourceLocation id) {
        var module = GhostModuleRegistry.get(id);
        if (module == null || module.domainDefinition().isEmpty() || !host.carriesModule(id) || maxGrade(host,id)<1) return false;
        var entry = host.getModules().stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
        if (entry == null || (entry.sleeping() && !host.controlsIntrinsicModules())
                || !host.suppression(id).canUseAbility(host.controlsIntrinsicModules())
                || !host.moduleEnvironment(id).exposed()) return false;
        var data = entry.customData();
        return data.contains("domainOpen") ? data.getBoolean("domainOpen")
                : module.domainDefinition().orElseThrow().automatic() && host.allowsAutomaticAbilities() && !entry.deadlocked();
    }

    /** 读取并补齐通用持久状态；沿用旧 customData 键，保留已有鬼域范围和成长数据。 */
    private static CompoundTag state(GhostModuleHost host, ResourceLocation id, GhostDomainDefinition definition) {
        var data = ((GhostModuleCustomDataModifier)host).getModuleCustomData(id);
        var entry = host.getModules().stream().filter(e -> e.id().equals(id)).findFirst().orElseThrow();
        if (!data.contains("domainStrength")) data.putDouble("domainStrength", entry.intensity());
        if (!data.contains("domainRadius")) data.putDouble("domainRadius", definition.radius());
        if (!data.contains(GhostDomainSettings.KEY)) data = definition.defaultSettings().write(data);
        if (!data.contains("domainLayer")) data.putInt("domainLayer", definition.initialLayer());
        data.putInt("domainLayer",Math.clamp(data.getInt("domainLayer"),definition.minGrade(),definition.maxGrade(entry.intensity())));
        return data;
    }

    /** 在模块 tick 维护所有已声明鬼域；不维护按厉鬼名字分支的生命周期表。 */
    public static void sync(GhostModuleHost host) {
        for (var entry : host.getModules()) {
            var module = GhostModuleRegistry.get(entry.id());
            if (module != null && module.domainDefinition().isPresent()) sync(host, entry.id());
        }
    }

    /** 创建或收回模块身体；已存在时不重复创建，也不覆盖其运行时层数。 */
    public static void sync(GhostModuleHost host, ResourceLocation id) {
        var level = level(host);
        if (level == null || !(host instanceof GhostModuleCustomDataModifier modifier)) return;
        var existing = current(host,id);
        if (!enabled(host,id)) { closeRuntime(host,id); return; }
        if (existing != null) {
            int maximum=maxGrade(host,id);
            int corrected=Math.clamp(existing.getLayer(),minGrade(host,id),maximum);
            if(existing.getLayer()!=corrected)GhostDomainManager.get(level).updateLayer(existing.getId(),corrected);
            return;
        }
        var definition = GhostModuleRegistry.get(id).domainDefinition().orElseThrow();
        var data = state(host,id,definition);
        modifier.setModuleCustomData(id,data);
        var position = host.moduleWorld(id).position();
        var radius = finite(data.getDouble("domainRadius"),definition.radius());
        var domain = new GhostDomain(UUID.randomUUID(), sourceId(host), id,
                finite(data.getDouble("domainStrength"),0), Math.clamp(data.getInt("domainLayer"),1,10),
                radius, level.dimension(),position.x,position.y,position.z, definition.shape().apply(radius),
                GhostDomainUpdateMode.DISTANCE, 0, new ModuleGhostDomainBehavior(host,id,definition));
        INDEX.computeIfAbsent(key(host),ignored -> new HashMap<>()).put(id,new WeakReference<>(domain));
        GhostDomainManager.get(level).add(domain);
    }

    /** 过滤存档中的非有限值，避免污染空间索引。 */
    private static double finite(double value,double fallback) { return Double.isFinite(value) && value >= 0 ? value : fallback; }

    /** 开关只修改当前模块的 customData，不依赖驭鬼专用状态。 */
    public static boolean toggle(GhostModuleHost host, ResourceLocation id) {
        if (!(host instanceof GhostModuleCustomDataModifier modifier) || level(host) == null) return false;
        var module = GhostModuleRegistry.get(id);
        if (module == null || module.domainDefinition().isEmpty() || !host.hasModule(id)) return false;
        var data = state(host,id,module.domainDefinition().orElseThrow());
        boolean open=data.contains("domainOpen")?data.getBoolean("domainOpen"):enabled(host,id);
        if(!open && maxGrade(host,id)<1)return false;
        data.putBoolean("domainOpen", !open);
        modifier.setModuleCustomData(id,data);
        sync(host,id);
        return true;
    }

    /** 清理运行时资源，不删除保存的开关、层数和范围。 */
    public static void closeRuntime(GhostModuleHost host, ResourceLocation id) {
        var domain = current(host,id);
        if (domain != null) GhostDomainManager.get(level(host)).remove(domain.getId());
    }

    /** 框架修改层数后同步到模块数据，使换宿主和重载仍保留层数。 */
    public static void saveLayer(GhostModuleHost host, ResourceLocation id, int layer) {
        if (host instanceof GhostModuleCustomDataModifier modifier && host.hasModule(id) && layer>=minGrade(host,id) && layer<=maxGrade(host,id)) {
            var data = modifier.getModuleCustomData(id);data.putInt("domainLayer",layer);modifier.setModuleCustomData(id,data);
        }
    }
}