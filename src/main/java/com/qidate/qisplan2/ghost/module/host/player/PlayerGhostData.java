package com.qidate.qisplan2.ghost.module.host.player;

import com.qidate.qisplan2.ghost.bearing.HostComposition;
import com.qidate.qisplan2.ghost.bearing.GhostBearing;

import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.possession.data.*;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家模块的唯一持久化入口。
 * 旧 UI、压制和鬼域接口所需的 DTO 均由模块即时投影，不再保存另一份厉鬼状态。
 * 复苏的模块单位为 0..maxRevival，旧视图为 0..1，只在这个边界转换。
 */
public final class PlayerGhostData {
    private static final String MIGRATED = "QisPlan2PlayerModulesMigrated";
    private static final String PLAYER_STATE = "qisplan2:player";
    private static final String COMBAT = "qisplan2:combat";
    private PlayerGhostData() {}
    public static List<GhostModuleData.Entry> getModules(Player player) {
        if (!player.level().isClientSide && !player.getPersistentData().getBoolean(MIGRATED)) {
            var existing = player.getData(ModAttachments.PLAYER_GHOST_MODULES);
            var old = player.getData(ModAttachments.POSSESSED_GHOSTS);
            if (existing.isEmpty() && !old.isEmpty()) {
                List<GhostModuleData.Entry> migrated = new ArrayList<>();
                old.forEach((id, data) -> migrated.add(fromView(id, data, null)));
                player.setData(ModAttachments.PLAYER_GHOST_MODULES, List.copyOf(migrated));
            }
            player.getPersistentData().putBoolean(MIGRATED, true);
            if (!old.isEmpty()) player.setData(ModAttachments.POSSESSED_GHOSTS, new HashMap<>());
        }
        return List.copyOf(player.getData(ModAttachments.PLAYER_GHOST_MODULES));
    }
    public static void setModules(Player player, List<GhostModuleData.Entry> entries) {
        if (player.level().isClientSide) throw new IllegalStateException("Player module mutation requires server");
        GhostBearing.validateChange(HostComposition.HUMANOID, getModules(player), entries);
        player.getPersistentData().putBoolean(MIGRATED, true);
        player.setData(ModAttachments.PLAYER_GHOST_MODULES, List.copyOf(entries));
    }
    /** 旧界面与快照所需的只读状态投影，不另建一套厉鬼状态。 */
    public static PossessedGhostState state(Player player, ResourceLocation id) {
        var view=views(player).get(id);return view==null?null:view.state();
    }
    /** 旧快照使用的状态映射，实际存档仍是模块列表。 */
    public static Map<ResourceLocation,PossessedGhostState> states(Player player) {
        Map<ResourceLocation,PossessedGhostState> result=new LinkedHashMap<>();
        views(player).forEach((id,data)->result.put(id,data.state()));return result;
    }
    /** 旧数据写回边界只合并字段，不处理能力、减伤、复苏或宿主生命周期。 */
    public static void writeState(Player player,ResourceLocation id,PossessedGhostState state) {
        var data=new LinkedHashMap<>(views(player));var old=data.get(id);
        if(old==null||state==null)return;data.put(id,old.withState(state));writeViews(player,data);
    }

    private static double maximum(ResourceLocation id) {
        var module = GhostModuleRegistry.get(id);
        return module == null ? 100 : Math.max(1, module.getMaxRevival());
    }
    public static Map<ResourceLocation, PossessedGhostData> views(Player player) {
        Map<ResourceLocation, PossessedGhostData> result = new LinkedHashMap<>();
        for (var entry : getModules(player)) result.put(entry.id(), view(entry));
        return result;
    }
    public static PossessedGhostData view(GhostModuleData.Entry entry) {
        var custom = entry.customData();
        var player = custom.getCompound(PLAYER_STATE);
        var state = new PossessedGhostState(entry.revival() / maximum(entry.id()), entry.intensity(),
                custom.contains(GhostModuleOperations.REVIVAL_RESERVE) ? custom.getDouble(GhostModuleOperations.REVIVAL_RESERVE) : player.getDouble("shallowStun"), GhostModuleCombatState.sleepTicks(entry),
                entry.deadlocked(), player.getLong("lastAbilityUseTick"));
        Optional<PossessedGhostDomainData> domain = Optional.empty();
        if (custom.contains("domainStrength")) {
            domain = Optional.of(new PossessedGhostDomainData(custom.getBoolean("domainOpen"),
                    custom.getDouble("domainStrength"), custom.getDouble("domainRadius"),
                    Math.clamp(custom.getInt("domainLayer"), 1, 10)));
        } else {
            var module = GhostModuleRegistry.get(entry.id());
            if (module != null) domain = module.domainDefinition().map(definition ->
                    new PossessedGhostDomainData(false, entry.intensity(),
                            definition.radius(), definition.initialLayer()));
        }
        return new PossessedGhostData(state, domain);
    }
    /** 写入旧接口修改的字段时，以最新模块为基础，保留其全部专属数据及沉睡/裁决死机状态。 */
    public static void writeViews(Player player, Map<ResourceLocation, PossessedGhostData> views) {
        Map<ResourceLocation, GhostModuleData.Entry> current = new HashMap<>();
        getModules(player).forEach(e -> current.put(e.id(), e));
        List<GhostModuleData.Entry> entries = new ArrayList<>();
        views.forEach((id, value) -> entries.add(fromView(id, value, current.get(id))));
        setModules(player, entries);
    }
    private static GhostModuleData.Entry fromView(ResourceLocation id, PossessedGhostData value, GhostModuleData.Entry previous) {
        CompoundTag custom = previous == null ? new CompoundTag() : previous.customData().copy();
        var state = value.state();
        CompoundTag player = custom.getCompound(PLAYER_STATE).copy();
        custom.putDouble(GhostModuleOperations.REVIVAL_RESERVE, state.shallowStun());
        player.remove("shallowStun");
        player.putLong("lastAbilityUseTick", state.lastAbilityUseTick());
        custom.put(PLAYER_STATE, player);
        if (previous == null) {
            // 仅旧存档迁移读取旧死机字段；运行时死机始终由模块接口修改。
            CompoundTag combat = new CompoundTag();
            combat.putInt("stunTicks", (int)Math.clamp(state.sleepTicks(), 0, Integer.MAX_VALUE));
            custom.put(COMBAT, combat);
        }
        value.domain().ifPresent(domain -> {
            custom.putBoolean("domainOpen", domain.open());
            custom.putDouble("domainStrength", domain.strength());
            custom.putDouble("domainRadius", domain.radius());
            custom.putInt("domainLayer", domain.layer());
        });
        return new GhostModuleData.Entry(id, state.strength(), state.revival() * maximum(id),
                previous != null && previous.sleeping(),
                previous == null ? state.permanentStun() : previous.deadlocked(), custom);
    }
}
