package com.qidate.qisplan2.ghost.module.ability;

import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostMedium;
import com.qidate.qisplan2.ghost.module.host.player.PlayerModuleAbility;
import com.qidate.qisplan2.ghost.module.host.player.PlayerAbilityParameters;
import java.util.ArrayList;
import java.util.List;

/** 宿主调用桥在执行边界装配，厉鬼模块只提供自身能力。 */
public final class GhostAbilityDefinitions {
    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
    private GhostAbilityDefinitions() {}

    /**
     * 合并模块自身能力与通用玩家输入桥，供执行边界统一查找。
     * 输入桥的装配不要求模块定义引用玩家载体。
     */
    public static List<GhostAbility> abilities(GhostModule module) {
        var result = new ArrayList<>(module.getAbilities());
        result.add(new PlayerModuleAbility(module));
        return result;
    }

    /**
     * 为调用入口选择媒介集合。
     * 玩家输入桥先检查输入有效性；桥接到具体模块能力时仍执行该能力的媒介。
     */
    public static List<GhostMedium> media(GhostModule module, GhostAbilityContext context) {
        return context.parameters() instanceof PlayerAbilityParameters
                ? List.of(PlayerModuleAbility.MEDIUM) : module.getMedia(context);
    }
}
