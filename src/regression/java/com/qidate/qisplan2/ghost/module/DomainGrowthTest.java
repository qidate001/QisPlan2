package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainGrowth;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainDefinition;
import com.qidate.qisplan2.ghost.domain.shape.SphereDomainShape;
import com.qidate.qisplan2.ghost.module.host.ModuleGhostDomains;
import com.qidate.qisplan2.ghost.module.implementation.mist.GhostMistModule;
import net.minecraft.nbt.CompoundTag;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

/** 成长门槛、基础强度来源及宿主统一存档入口的回归。 */
public final class DomainGrowthTest {
    private DomainGrowthTest() {}
    /** 失败时保留明确的规则说明。 */
    private static void check(boolean result,String message){if(!result)throw new AssertionError(message);}
    /** 验证门槛前后、无效输入、声明隔离及关闭状态下不能写入越界等级。 */
    public static void run() {
        var growth=GhostDomainGrowth.STANDARD;
        double[] thresholds={50,70,100,145,213,314,466,694,1036,1548};
        for(int i=0;i<thresholds.length;i++) {
            check(growth.requiredIntensity(i+1)==thresholds[i],"总门槛一次向上取整");
            check(growth.gradeFor(Math.nextDown(thresholds[i]))==i,"刚低于门槛不能升级");
            check(growth.gradeFor(thresholds[i])==i+1,"恰好达到门槛即可升级");
        }
        check(growth.gradeFor(Double.MAX_VALUE)==10,"最高十级");
        check(growth.gradeFor(Double.NaN)==0&&growth.gradeFor(Double.POSITIVE_INFINITY)==0,"非法强度不解锁等级");
        check(GhostDomainGrowth.linear(50,20,3).requiredIntensity(3)==90,"倍率为一时使用线性公式");
        double[] custom={50,100,200};var table=GhostDomainGrowth.table(custom);custom[1]=1;
        check(table.requiredIntensity(2)==100,"自定义表不受外部数组修改影响");
        boolean rejected=false;try{GhostDomainGrowth.table(50,50);}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"拒绝不递增的门槛");
        for(var module:GhostModuleRegistry.getAll())module.domainDefinition().ifPresent(definition->{
            check((definition.growth()!=null)==module.getId().equals(GhostMistModule.ID),"仅鬼雾接入成长");
            if(!module.getId().equals(GhostMistModule.ID))check(definition.maxGrade(0)==10,"其他模块保留原有等级机制");
        });
        var definition=GhostModuleRegistry.get(GhostMistModule.ID).domainDefinition().orElseThrow();
        check(definition.maxGrade(1)==1&&definition.maxGrade(5)==1&&definition.maxGrade(0)==1,"鬼雾天生一级不依赖50强度门槛");
        check(definition.maxGrade(69)==1&&definition.maxGrade(70)==2,"70强度解锁成长到二级");
        var six=new GhostDomainDefinition(80,6,true,SphereDomainShape::new,true,false,20,true,true,List.of()).withGrowth(growth);
        check(six.minGrade()==6&&six.maxGrade(5)==6,"默认六级在低强度时仍保留六级");
        check(six.maxGrade(465)==6&&six.maxGrade(466)==7,"默认六级按全局七级门槛466成长");
        check(six.maxGrade(1)==6,"强度降低不得低于默认六级");
        check(!definition.layered(),"等级成长不会隐式开启分层");
        check(definition.withLayering(true).growth()==growth&&definition.withDefaults(definition.defaultSettings()).growth()==growth,"声明链保留成长策略");
        double[] intensity={100};CompoundTag[] data={new CompoundTag()};
        data[0].putDouble("domainStrength",999999);data[0].putInt("domainLayer",1);
        var host=(GhostModuleHost)Proxy.newProxyInstance(DomainGrowthTest.class.getClassLoader(),
                new Class<?>[]{GhostModuleHost.class,GhostModuleCustomDataModifier.class},(proxy,method,args)->switch(method.getName()) {
            case "getModules"->List.of(new GhostModuleData.Entry(GhostMistModule.ID,intensity[0],0,false,false,data[0]));
            case "getModuleCustomData"->data[0].copy();
            case "setModuleCustomData"->{data[0]=((CompoundTag)args[1]).copy();yield true;}
            default->{if(method.isDefault())yield InvocationHandler.invokeDefault(proxy,method,args);throw new AssertionError(method.getName());}
        });
        check(ModuleGhostDomains.maxGrade(host,GhostMistModule.ID)==3,"使用模块基础强度，不使用独立 domainStrength");
        ModuleGhostDomains.saveLayer(host,GhostMistModule.ID,4);
        check(data[0].getInt("domainLayer")==1,"关闭状态也不能保存超上限等级");
        ModuleGhostDomains.saveLayer(host,GhostMistModule.ID,3);
        check(data[0].getInt("domainLayer")==3,"允许保存已解锁等级");
        intensity[0]=70;check(ModuleGhostDomains.maxGrade(host,GhostMistModule.ID)==2,"强度降低即时降低上限");
        intensity[0]=49;check(ModuleGhostDomains.maxGrade(host,GhostMistModule.ID)==1,"不足50仍保留天生一级鬼域");
        ModuleGhostDomains.saveLayer(host,GhostMistModule.ID,1);
        check(data[0].getInt("domainLayer")==1,"低强度仍能保存默认一级");
        System.out.println("Domain growth thresholds, module opt-in, base intensity and persistence checks passed");
    }
}
