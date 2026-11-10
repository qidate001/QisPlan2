package com.qidate.qisplan2.ghost.module.host.player.binding;
import com.qidate.qisplan2.ghost.module.GhostModule;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
public final class GhostPlayerBindings {
    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
    private GhostPlayerBindings() {}
    private static final GhostPlayerBinding NONE = new GhostPlayerBinding() {};
    private static final Map<ResourceLocation, GhostPlayerBinding> BINDINGS = Map.of(
            ResourceLocation.fromNamespaceAndPath("qisplan2", "night_wanderer"), new NightWandererPlayerBinding(),
            ResourceLocation.fromNamespaceAndPath("qisplan2", "knocking_ghost"), new KnockingGhostPlayerBinding(),
            ResourceLocation.fromNamespaceAndPath("qisplan2", "ghost_eye"), new GhostEyePlayerBinding(),
            ResourceLocation.fromNamespaceAndPath("qisplan2", "ghost_mist"), new GhostMistPlayerBinding());

    /**
     * 按模块标识选择玩家载体适配器；没有注册专用适配时返回默认空实现。
     */
    public static GhostPlayerBinding get(GhostModule module) { return BINDINGS.getOrDefault(module.getId(), NONE); }
}
