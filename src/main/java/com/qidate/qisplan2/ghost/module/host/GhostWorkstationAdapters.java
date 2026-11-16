package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.environment.GhostWorkstationAccess;
import com.qidate.qisplan2.ghost.module.implementation.GhostStoveModule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import java.util.Map;
import java.util.HashMap;
import java.util.function.BiFunction;

/** 工作台类型到服务的注册适配；以后新增工作台无需在厉鬼或输入网络里判断载体类别。 */
public final class GhostWorkstationAdapters {
    /** 工作台服务提供菜单和制作生命周期，库存实现可以按类型选择。 */
    public interface Session extends MenuProvider {
        /** 实时校验交互者与存储是否仍有效。 */
        boolean canUse(ServerPlayer player);
        /** 推进一轮制作，内部去重同 tick 的维护调用。 */
        void tick();
    }
    private static final Map<ResourceLocation, BiFunction<GhostModuleHost, ResourceLocation, Session>> TYPES = new HashMap<>();
    static { register(GhostStoveModule.ID, GhostStoveWorkstation::obtain); }
    /** 工具类无需实例化。 */
    private GhostWorkstationAdapters() {}
    /** 注册工作台服务，拒绝重复覆盖既有类型。 */
    public static void register(ResourceLocation type, BiFunction<GhostModuleHost,ResourceLocation,Session> factory) {
        if (TYPES.putIfAbsent(type,factory) != null) throw new IllegalArgumentException("Duplicate workstation: " + type);
    }
    /** 将工作台操作绑定到当前模块实例，不把载体对象传回模块。 */
    public static GhostWorkstationAccess bind(GhostModuleHost host, ResourceLocation module) {
        return new GhostWorkstationAccess() {
            /** 无模块或未暴露时不提供服务，避免背包中的物品产生世界效果。 */
            private Session session(ResourceLocation type) {
                var factory = TYPES.get(type);
                return factory != null && host.carriesModule(module) && GhostEnvironmentAdapters.bind(host,module).exposed()
                        ? factory.apply(host,module) : null;
            }
            /** 菜单只向合法的服务端交互者开放。 */
            public boolean canOpen(ResourceLocation type, LivingEntity user) {
                var service = session(type); return service != null && user instanceof ServerPlayer player && service.canUse(player);
            }
            /** 再次验证后由标准菜单协议同步槽位和进度。 */
            public boolean open(ResourceLocation type, LivingEntity user) {
                var service = session(type);
                return service != null && user instanceof ServerPlayer player && service.canUse(player) && player.openMenu(service).isPresent();
            }
            /** 所有载体通过同一个服务推进配方。 */
            public void tick(ResourceLocation type) { var service = session(type); if (service != null) service.tick(); }
        };
    }
}
