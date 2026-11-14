package com.qidate.qisplan2.ghost.module.ability;

import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostMedium;
import java.util.ArrayList;
import java.util.List;

/** 统一读取模块声明的能力与媒介，不再偷偷追加玩家专用能力。 */
public final class GhostAbilityDefinitions {
    /** 工具类不实例化。 */
    private GhostAbilityDefinitions() {}

    /** 所有调用渠道使用模块声明的真实能力列表。 */
    public static List<GhostAbility> abilities(GhostModule module) { return List.copyOf(module.getAbilities()); }

    /** 合并当前能力选择继承的模块媒介与能力独有媒介，主动调用不能跳过检查。 */
    public static List<GhostMedium> media(GhostModule module, GhostAbilityContext context) {
        var ability = context.getAbility();
        var result = new ArrayList<GhostMedium>();
        if (ability.inheritsModuleMedia()) result.addAll(module.getMedia(context));
        result.addAll(ability.media(context));
        return List.copyOf(result);
    }
}
