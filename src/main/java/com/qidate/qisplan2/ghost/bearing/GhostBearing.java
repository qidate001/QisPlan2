package com.qidate.qisplan2.ghost.bearing;

import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** 通用承载结算：只依赖组成和模块定义，不区分实体、玩家、物品或方块。 */
public final class GhostBearing {
    /** 工具类无需实例化。 */
    private GhostBearing() {}

    /** 分配结果同时保留有效模块、占位列表和无法承载的模块，便于界面或存档诊断。 */
    public record Allocation(Set<ResourceLocation> accepted,
            Map<SpiritualPart, List<ResourceLocation>> occupants, Set<ResourceLocation> rejected) {
        /** 对结果做深层只读快照，调用者不能改写实际占位。 */
        public Allocation {
            accepted = Set.copyOf(accepted);
            rejected = Set.copyOf(rejected);
            var copy = new LinkedHashMap<SpiritualPart, List<ResourceLocation>>();
            occupants.forEach((part, modules) -> copy.put(part, List.copyOf(modules)));
            occupants = Collections.unmodifiableMap(copy);
        }
    }

    /**
     * 按保存顺序分配部位。死机或沉睡不会释放部位，只有移除模块才会释放。
     * 未注册模块保留数据但不参与运行；旧数据冲突时保留所有实例，仅使后续冲突实例无效。
     */
    public static Allocation allocate(HostComposition composition, List<GhostModuleData.Entry> entries) {
        var accepted = new LinkedHashSet<ResourceLocation>();
        var rejected = new LinkedHashSet<ResourceLocation>();
        var occupants = new LinkedHashMap<SpiritualPart, List<ResourceLocation>>();
        composition.capacities().keySet().forEach(part -> occupants.put(part, new ArrayList<>()));
        for (var entry : entries) {
            if (accepted.contains(entry.id())) continue;
            var module = GhostModuleRegistry.get(entry.id());
            if (module == null) { rejected.add(entry.id()); continue; }
            var form = module.form();
            if (form.kind() == GhostModuleForm.Kind.INDEPENDENT) { accepted.add(entry.id()); continue; }
            var part = composition.resolve(form.part());
            if (part == null || occupants.get(part).size() >= composition.capacities().get(part)) {
                rejected.add(entry.id()); continue;
            }
            occupants.get(part).add(entry.id());
            accepted.add(entry.id());
        }
        return new Allocation(accepted, occupants, rejected);
    }

    /** 安装前验证新模块不会失配或挤掉任何已有效的模块，已有同 ID 更新不重新占位。 */
    public static boolean canInstall(HostComposition composition, List<GhostModuleData.Entry> entries,
            ResourceLocation id) {
        if (entries.stream().anyMatch(entry -> entry.id().equals(id))) return true;
        var before = allocate(composition, entries);
        var next = new ArrayList<>(entries);
        next.add(new GhostModuleData.Entry(id, 0));
        var after = allocate(composition, next);
        return after.accepted().contains(id) && after.accepted().containsAll(before.accepted());
    }

    /** 写入新列表前禁止新增承载冲突；原有不可承载数据允许保留、更新或移除。 */
    public static void validateChange(HostComposition composition, List<GhostModuleData.Entry> previous,
            List<GhostModuleData.Entry> next) {
        var before = allocate(composition, previous);
        var after = allocate(composition, next);
        for (var id : after.rejected())
            if (!before.rejected().contains(id))
                throw new IllegalArgumentException("Module cannot be carried: " + id);
    }
}
