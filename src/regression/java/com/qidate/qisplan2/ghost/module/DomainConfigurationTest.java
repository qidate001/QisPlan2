package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.domain.GhostDomainBoundary;
import com.qidate.qisplan2.ghost.domain.shape.SphereDomainShape;
import com.qidate.qisplan2.ghost.domain.shape.CylinderDomainShape;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.domain.*;
import com.qidate.qisplan2.ghost.module.environment.*;
import com.qidate.qisplan2.ghost.module.implementation.eye.GhostEyeModule;
import com.qidate.qisplan2.ghost.module.implementation.eye.GhostEyeDomainControls;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppressionState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.*;
import java.util.*;

/** 无需启动客户端的权限、存档、几何穿透与真实能力执行链回归。 */
public final class DomainConfigurationTest {
    private DomainConfigurationTest(){}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    /** 验证相互独立的进出策略、名单优先级、模块声明和十眼状态变化。 */
    public static void run(){
        for(int group=0;group<4;group++)check(!GhostDomainSettings.Access.CLOSED.allows(group,"Alex","minecraft:pig"),"默认拒绝四类实体跨界");
        var access=new GhostDomainSettings.Access(15,"Alex,minecraft:pig","Alex");
        check(!access.allows(0,"Alex","minecraft:player"),"黑名单优先于白名单和分类开关");
        check(new GhostDomainSettings.Access(0," Alex , minecraft:pig ","").allows(3,"","minecraft:pig"),"实体ID白名单可以覆盖分类关闭");
        check(!new GhostDomainSettings.Access(0,"Alex","").allows(0,"Alexander","minecraft:player"),"姓名整项匹配而非子串");
        check(GhostDomainSettings.matches("#ordinary, 凡人",3,"","minecraft:cow",true),"稳定分类目标可识别");
        check(GhostDomainSettings.matches("#ordinary, 凡人",0,"Steve","minecraft:player",true),"中文分类别名可识别");
        var config=new GhostDomainSettings(access,GhostDomainSettings.Access.CLOSED,List.of(new GhostDomainSettings.Rule(GhostDomainPresets.layerId(GhostEyeModule.ID,1),"#ordinary,#mortal",false)));
        CompoundTag original=new CompoundTag();original.putInt("eyeMask",513);original.putString("other","preserved");
        check(GhostDomainSettings.read(config.write(original)).equals(config),"进出设置、自动化启停和目标存档往返");
        check(config.write(original).getInt("eyeMask")==513&&!original.contains(GhostDomainSettings.KEY),"配置保存保留模块其他字段且不原地修改");
        boolean rejected=false;try{new GhostDomainSettings.Access(0,"x".repeat(513),"");}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"拒绝超长选择器");
        check(GhostDomainBoundary.denied(false,true,false,false,true),"禁止进入");
        check(GhostDomainBoundary.denied(true,false,false,true,false),"禁止离开");
        check(!GhostDomainBoundary.denied(true,true,false,false,false),"关闭进出不阻止域内移动");
        check(GhostDomainBoundary.denied(false,false,true,true,false),"穿越整域必须同时允许离开");
        check(!GhostDomainBoundary.denied(false,false,true,true,true),"两侧允许才能穿越");
        var sphere=new SphereDomainShape(1);var cylinder=new CylinderDomainShape(1);
        check(sphere.intersectsSegment(0,0,0,new Vec3(-100,0,0),new Vec3(100,0,0)),"高速移动两端都在外仍识别穿域");
        check(!sphere.intersectsSegment(0,0,0,new Vec3(-100,2,0),new Vec3(100,2,0)),"近旁经过不误判");
        check(cylinder.intersectsSegment(0,0,0,new Vec3(-100,300,0),new Vec3(100,300,0)),"圆柱路径判定与无限高度形状一致");
        for(var module:GhostModuleRegistry.getAll())if(module.domainDefinition().isPresent()){
            var definitions=module.getAbilities();var panel=module.domainPanel();
            var refs=new ArrayList<ResourceLocation>(panel.common().values());refs.addAll(panel.buttons());panel.controls().forEach(c->refs.add(c.ability()));
            for(var ref:refs)check(definitions.stream().anyMatch(a->a.getId().equals(ref)&&a.isPublicSkill()),"面板不能引用未声明或私有技能: "+ref);
            for(var automation:panel.automations())check(definitions.stream().anyMatch(a->a.getId().equals(automation.ability())),"自动化必须有真实模块能力");
        }
        exerciseEyes();
        System.out.println("Domain access, persistence, segment crossing, panel declarations and ten-eye ability checks passed");
    }
    /** 用宿主接口桩执行真实鬼眼能力；桩不模拟世界，只记录能力发出的接口请求。 */
    private static void exerciseEyes(){
        CompoundTag[] data={new CompoundTag()};GhostSuppressionState[] suppression={GhostSuppressionState.NONE};
        GhostDomainAccess domain=new GhostDomainAccess(){
            public boolean exists(){return data[0].getBoolean("domainOpen");}
            public int layer(){return data[0].getInt("domainLayer");}
            public boolean setOpen(boolean open){data[0].putBoolean("domainOpen",open);return true;}
            public boolean setLayer(int layer){data[0].putInt("domainLayer",layer);return true;}
        };
        var world=(GhostWorldAccess)Proxy.newProxyInstance(DomainConfigurationTest.class.getClassLoader(),new Class<?>[]{GhostWorldAccess.class},(p,m,a)->switch(m.getName()){
            case "domain"->domain;case "canToggleDomain","present"->true;default->throw new AssertionError(m.getName());
        });
        var host=(GhostModuleHost)Proxy.newProxyInstance(DomainConfigurationTest.class.getClassLoader(),new Class<?>[]{GhostModuleHost.class,GhostModuleCustomDataModifier.class},(p,m,a)->switch(m.getName()){
            case "getModules"->List.of(new GhostModuleData.Entry(GhostEyeModule.ID,100,0,false,true,data[0]));
            case "getModuleCustomData"->data[0].copy();
            case "setModuleCustomData"->{data[0]=((CompoundTag)a[1]).copy();yield true;}
            case "moduleWorld"->world;case "suppression"->suppression[0];case "controlsIntrinsicModules"->true;
            default->{if(m.isDefault())yield InvocationHandler.invokeDefault(p,m,a);throw new AssertionError(m.getName());}
        });
        for(int i:new int[]{0,9})check(toggle(host,i),"死机状态仍可主动开眼");
        check(data[0].getInt("domainLayer")==2&&data[0].getInt("eyeMask")==513,"任意两只眼睛对应两层，而不是最高眼睛编号");
        check(toggle(host,0)&&data[0].getInt("domainLayer")==1,"闭一眼减一层");
        suppression[0]=GhostSuppressionState.EXTERNAL_COMPLETE;
        check(!toggle(host,9)&&data[0].getBoolean("domainOpen"),"完全压制不能借面板绕过能力执行器");
        suppression[0]=GhostSuppressionState.NONE;
        check(toggle(host,9)&&!data[0].getBoolean("domainOpen"),"全部闭眼关闭鬼域");
        check(!toggle(host,10),"越界眼睛编号拒绝");
    }
    /** 通过真实统一执行器提交一次点眼操作。 */
    private static boolean toggle(GhostModuleHost host,int eye){return GhostAbilityExecutor.tryExecuteWithResult(new GhostAbilityContext(host,GhostEyeModule.ID,GhostEyeDomainControls.TOGGLE_EYE,
            GhostAbilityContext.Source.POSSESSION,null,()->{},new GhostInvocationParameters(null,eye)));}
}
