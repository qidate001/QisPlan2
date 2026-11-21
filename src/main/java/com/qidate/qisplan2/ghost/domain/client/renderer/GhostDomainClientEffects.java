package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainEffect;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import java.io.IOException;
import java.util.*;

/** 通用客户端效果适配表，只按声明类型分发，不认识鬼雨、鬼眼或任何厉鬼 ID。 */
public final class GhostDomainClientEffects {
    private static final Map<Class<?>, Binding<?>> BINDINGS = new LinkedHashMap<>();
    private GhostDomainClientEffects() {}

    /** 客户端初始化时注册一种声明的实现；这不是注册鬼域或厉鬼。 */
    public static <E extends GhostDomainEffect> void register(Class<E> type, GhostDomainClientEffect<E> adapter) {
        if(BINDINGS.putIfAbsent(type,new Binding<>(type,Objects.requireNonNull(adapter)))!=null)
            throw new IllegalStateException("Duplicate domain effect adapter: "+type.getName());
    }
    /** 所有模块只注册自身；表现从其效果列表派生，不再维护另一份鬼域清单。 */
    public static void installModules() {
        for(var module:GhostModuleRegistry.getAll()) module.domainDefinition().ifPresent(domain -> {
            for(var effect:domain.effects()) binding(effect).install(module.getId(),effect);
        });
    }
    /** 每次资源重载重新遍历声明与适配器，缺失实现会给出明确错误。 */
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        Set<ResourceLocation> loaded=new HashSet<>();
        for(var module:GhostModuleRegistry.getAll()) {
            var domain=module.domainDefinition().orElse(null);
            if(domain==null) continue;
            for(var effect:domain.effects()) binding(effect).shaders(event,effect,loaded);
        }
    }
    /** 使用精确声明类型，避免多个继承匹配导致同一效果被重复安装。 */
    private static Binding<?> binding(GhostDomainEffect effect) {
        var binding=BINDINGS.get(effect.getClass());
        if(binding==null) throw new IllegalStateException("No client adapter for domain effect: "+effect.getClass().getName());
        return binding;
    }
    /** 在适配边界完成受检类型转换，调用方无需不安全强转。 */
    private record Binding<E extends GhostDomainEffect>(Class<E> type,GhostDomainClientEffect<E> adapter) {
        void install(ResourceLocation module,GhostDomainEffect effect) { adapter.install(module,type.cast(effect)); }
        void shaders(RegisterShadersEvent event,GhostDomainEffect effect,Set<ResourceLocation> loaded) throws IOException {
            adapter.registerShaders(event,type.cast(effect),loaded);
        }
    }
}
