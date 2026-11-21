package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.environment.*;
import com.qidate.qisplan2.ghost.module.event.TouchEvent;
import com.qidate.qisplan2.ghost.module.host.GhostContactEvents;
import com.qidate.qisplan2.ghost.module.host.ModuleGhostDomains;
import com.qidate.qisplan2.ghost.module.implementation.GhostUmbrellaModule;
import com.qidate.qisplan2.ghost.module.implementation.umbrella.GhostRainEffect;
import com.qidate.qisplan2.ghost.module.implementation.eye.GhostEyeModule;
import com.qidate.qisplan2.ghost.module.implementation.mist.GhostMistModule;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppressionState;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.nbt.CompoundTag;

/** 无具体载体的鬼域声明、接触计时与普通伤害入口回归。 */
public final class ModuleDomainTest {
    /** 断言失败使构建失败。 */
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    /** 使用真实能力执行器验证伤害类型、媒介、死机与压制，禁止走灵异 attack 出口。 */
    public static void run(){
        var mist=new GhostMistModule();var eye=new GhostEyeModule();var umbrella=new GhostUmbrellaModule();
        for(var module:List.of(mist,eye,umbrella))if(GhostModuleRegistry.get(module.getId())==null)GhostModuleRegistry.register(module);
        check(eye.domainDefinition().orElseThrow().sight() && !mist.domainDefinition().orElseThrow().sight(),"只有声明了看见的鬼域产生看见事实");
        check(mist.domainDefinition().orElseThrow().exposureInterval()==20,"鬼雾每20 tick产生一次周期曝光");
        check(mist.initialDomainData().orElseThrow().radius()==80 && umbrella.initialDomainData().orElseThrow().layer()==3,"旧UI视图由模块鬼域声明派生");
        check(umbrella.domainDefinition().orElseThrow().effects().contains(GhostRainEffect.INSTANCE) && !mist.domainDefinition().orElseThrow().effects().contains(GhostRainEffect.INSTANCE),"天气表现从模块声明选择，不写死鬼伞类型");
        var samples=new GhostContactEvents.Samples();var target=UUID.randomUUID();
        check(samples.sample(target,1)==TouchEvent.Phase.BEGIN,"首次覆盖是开始接触");
        check(samples.sample(target,1)==null,"多个鬼域或本体同tick不能重复计时");
        check(samples.sample(target,2)==TouchEvent.Phase.CONTINUE,"连续覆盖是持续接触");
        check(samples.sample(target,5)==TouchEvent.Phase.BEGIN,"离开后再次进入重建接触，不补算间隔");
        check(samples.sample(UUID.randomUUID(),5)==TouchEvent.Phase.BEGIN,"目标独立计时");
        boolean[] inside={true},controlled={true},automatic={false},exposed={true};
        int[] hurts={0};
        GhostSuppressionState[] suppression={GhostSuppressionState.NONE};
        CompoundTag[] custom={new CompoundTag()};
        boolean[] deadlocked={true},sleeping={false};
        var world=(GhostWorldAccess)Proxy.newProxyInstance(ModuleDomainTest.class.getClassLoader(),new Class<?>[]{GhostWorldAccess.class},(proxy,method,args)->switch(method.getName()){
            case "present" -> true;
            case "aimedTarget" -> null;
            case "domain" -> new GhostDomainAccess(){public boolean contains(net.minecraft.world.entity.LivingEntity target){return inside[0];}};
            case "hurt" -> {check(args[1].equals(ModDamageTypes.GHOST_MIST) && ((Float)args[2])==0.1F,"鬼雾必须为GHOST_MIST的0.1点普通伤害");hurts[0]++;yield true;}
            default -> throw new AssertionError("鬼雾不应调用此出口: "+method.getName());
        });
        var environment=(GhostEnvironment)Proxy.newProxyInstance(ModuleDomainTest.class.getClassLoader(),new Class<?>[]{GhostEnvironment.class},(proxy,method,args)->{
            if(method.getName().equals("exposed"))return exposed[0];throw new AssertionError(method.getName());
        });
        var host=(GhostModuleHost)Proxy.newProxyInstance(ModuleDomainTest.class.getClassLoader(),new Class<?>[]{GhostModuleHost.class},(proxy,method,args)->switch(method.getName()){
            case "getModules" -> List.of(new GhostModuleData.Entry(mist.getId(),5,0,sleeping[0],deadlocked[0],custom[0]));
            case "controlsIntrinsicModules" -> controlled[0];
            case "allowsAutomaticAbilities" -> automatic[0];
            case "moduleWorld" -> world;
            case "moduleEnvironment" -> environment;
            case "suppression" -> suppression[0];
            default -> {if(method.isDefault())yield InvocationHandler.invokeDefault(proxy,method,args);throw new AssertionError(method.getName());}
        });
        var active=new GhostAbilityContext(host,mist.getId(),GhostMistModule.ATTACK,GhostAbilityContext.Source.POSSESSION,null);
        check(GhostAbilityExecutor.tryExecuteWithResult(active) && hurts[0]==1,"死机模块的主动侵蚀仍可造成普通伤害");
        inside[0]=false;check(!GhostAbilityExecutor.tryExecuteWithResult(active) && hurts[0]==1,"离开有效鬼域不能主动侵蚀");
        inside[0]=true;suppression[0]=GhostSuppressionState.EXTERNAL_COMPLETE;
        check(!GhostAbilityExecutor.tryExecuteWithResult(active) && hurts[0]==1,"完全压制禁止侵蚀能力");
        suppression[0]=GhostSuppressionState.NONE;
        check(!ModuleGhostDomains.enabled(host,mist.getId()),"未开启的受控鬼域不能自动展开");
        custom[0].putBoolean("domainOpen",true);
        check(ModuleGhostDomains.enabled(host,mist.getId()),"主动开关存于模块，死机不取消主动持续能力");
        exposed[0]=false;check(!ModuleGhostDomains.enabled(host,mist.getId()),"收起或卸载载体必须停止鬼域");
        exposed[0]=true;suppression[0]=GhostSuppressionState.EXTERNAL_COMPLETE;
        check(!ModuleGhostDomains.enabled(host,mist.getId()),"外部完全压制必须收回鬼域");
        suppression[0]=GhostSuppressionState.NONE;controlled[0]=false;automatic[0]=true;custom[0]=new CompoundTag();deadlocked[0]=false;
        check(ModuleGhostDomains.enabled(host,mist.getId()),"未受控的复苏模块按声明自动展开");
        sleeping[0]=true;check(!ModuleGhostDomains.enabled(host,mist.getId()),"未驾驭的沉睡模块不维持主动鬼域");
        System.out.println("Module domain declarations, contact aggregation and vanilla mist damage checks passed");
    }
}
