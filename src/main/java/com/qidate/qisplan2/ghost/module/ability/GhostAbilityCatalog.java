package com.qidate.qisplan2.ghost.module.ability;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** 供轮盘和未来自定义技能栏共用的能力目录，界面不需要识别具体厉鬼。 */
public final class GhostAbilityCatalog {
    public static final int WHEEL_LIMIT = 10;

    /** 单个技能始终同时携带模块 ID 与能力 ID，避免跨模块碰撞。 */
    public record Entry(ResourceLocation moduleId, GhostAbility ability) {}

    /** 工具类无需实例化。 */
    private GhostAbilityCatalog() {}

    /** 按模块保存顺序及能力声明顺序列出主动技能，死机不会隐藏仍可主动调用的能力。 */
    public static List<Entry> all(GhostModuleHost host) {
        var result = new ArrayList<Entry>();
        for (var entry : host.getModules()) {
            var module = GhostModuleRegistry.get(entry.id());
            if (module == null || !host.carriesModule(entry.id())) continue;
            for (var ability : module.getAbilities())
                if (ability.manuallySelectable()) result.add(new Entry(entry.id(), ability));
        }
        return List.copyOf(result);
    }

    /** 当前只取前十项；截取策略单独保留，后续可替换为玩家自定义排序。 */
    public static List<Entry> wheel(GhostModuleHost host) {
        var entries = all(host);
        return List.copyOf(entries.subList(0, Math.min(WHEEL_LIMIT, entries.size())));
    }
}
