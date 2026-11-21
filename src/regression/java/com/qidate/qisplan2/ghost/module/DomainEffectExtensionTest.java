package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.command.WorldCommands;
import com.qidate.qisplan2.ghost.domain.SphereDomainShape;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainClientEffects;
import com.qidate.qisplan2.ghost.domain.client.renderer.ModuleGhostDomainRenderEffect;
import com.qidate.qisplan2.ghost.module.domain.*;
import com.qidate.qisplan2.ghost.module.implementation.umbrella.GhostRainEffect;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** 验证未知的新效果通过开放接口接入，不依赖修改框架字段或已知效果枚举。 */
public final class DomainEffectExtensionTest {
    private record Pulse(float speed) implements GhostDomainEffect {}
    private record Sparks(int count) implements GhostDomainEffect {}
    private DomainEffectExtensionTest() {}
    /** 失败时使构建终止。 */
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    /** 使用无渲染适配器验证组合、参数分发和模块身份，同时检查删除后的命令树。 */
    public static void run() {
        var first=ResourceLocation.parse("qisplan2_test:effect_one");
        var second=ResourceLocation.parse("qisplan2_test:effect_two");
        var shader=ResourceLocation.parse("qisplan2:ghost_mist");
        var source=new ArrayList<GhostDomainEffect>(List.of(new Pulse(2),new Sparks(7)));
        var definition=new GhostDomainDefinition(16,1,false,SphereDomainShape::new,true,false,0,true,true,source);
        source.clear();
        check(definition.effects().size()==2,"声明必须复制效果列表，不能受外部修改影响");
        try { definition.effects().clear(); throw new AssertionError("效果列表必须不可变"); }
        catch(UnsupportedOperationException expected) {}
        GhostModuleRegistry.register(new GhostModule() {
            public ResourceLocation getId(){return first;}
            public Optional<GhostDomainDefinition> domainDefinition(){return Optional.of(definition);}
        });
        GhostModuleRegistry.register(new GhostModule() {
            public ResourceLocation getId(){return second;}
            public Optional<GhostDomainDefinition> domainDefinition(){return Optional.of(new GhostDomainDefinition(
                    8,1,false,SphereDomainShape::new,true,false,0,true,true,List.of(new Pulse(4))));}
        });
        var calls=new ArrayList<String>();
        // 内置声明也必须有绑定；此测试不创建任何窗口或真实 Shader。
        GhostDomainClientEffects.register(GhostDomainShaderEffect.class,(module,effect)->{});
        GhostDomainClientEffects.register(GhostRainEffect.class,(module,effect)->{});
        GhostDomainClientEffects.register(Pulse.class,(module,effect)->calls.add(module+":pulse:"+effect.speed()));
        GhostDomainClientEffects.register(Sparks.class,(module,effect)->calls.add(module+":sparks:"+effect.count()));
        GhostDomainClientEffects.installModules();
        check(calls.size()==3 && calls.contains(first+":pulse:2.0") && calls.contains(first+":sparks:7")
                && calls.contains(second+":pulse:4.0"),"新效果必须携带独立参数并分发给正确模块，不能变成全局开关");
        try {
            GhostDomainClientEffects.register(Pulse.class,(module,effect)->{});
            throw new AssertionError("重复绑定必须拒绝，不能覆盖已有实现");
        } catch(IllegalStateException expected) {}
        var render=new ModuleGhostDomainRenderEffect(first,shader,"GhostMist");
        check(render.domainType().equals(first) && render.shaderId().equals(shader),"复用着色器资源不能改变鬼域模块身份");
        var root=Commands.literal("qisplan2");
        WorldCommands.register(root);
        var node=root.build();
        check(node.getChild("debug_domain")==null,"Debug鬼域命令必须彻底移除");
        check(node.getChild("debug_isolation")!=null && node.getChild("kill")!=null,"删除Debug鬼域不能影响其他命令");
        System.out.println("Open domain effect adapters, parameters and debug command removal checks passed");
    }
}
