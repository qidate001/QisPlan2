package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.domain.GhostDomainConflict;
import com.qidate.qisplan2.ghost.domain.GhostDomainInfluenceScope;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainSettings;
import com.qidate.qisplan2.ghost.module.implementation.eye.GhostEyeModule;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.UUID;

/** 覆盖真实空间求解，不用模拟算法副本作为预期值。 */
public final class DomainConflictTest {
    private DomainConflictTest() {}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static GhostDomainConflict.Node node(int id,double x,double radius,double strength,int grade){
        return new GhostDomainConflict.Node(new UUID(0,id),new Vec3(x,0,0),radius,strength,grade,1);
    }
    /** 检查声明默认值、嵌套挖空、交界分摊、可恢复压制和顺序无关性。 */
    public static void run() {
        for(int group=0;group<4;group++){
            check(GhostDomainSettings.DEFAULT.enter().allows(group,"Alex","minecraft:pig"),"新实例默认允许进入");
            check(!GhostDomainSettings.DEFAULT.leave().allows(group,"Alex","minecraft:pig"),"新实例默认禁止离开");
        }
        for(var module:GhostModuleRegistry.getAll())module.domainDefinition().ifPresent(definition ->
            check(definition.layered()==module.getId().equals(GhostEyeModule.ID),"只有鬼眼显式分层"));
        var outer=node(1,0,200,1000,3);var inner=node(2,80,64,100,2);
        var nested=GhostDomainConflict.solve(List.of(outer,inner));
        check(nested.radius(inner.id())==3,"用户示例 64/2*10% 向下取整为3");
        check(nested.owns(inner.id(),inner.center())&&!nested.owns(outer.id(),inner.center()),"内域领地排除外域");
        check(nested.crossings(outer.id(),inner.center(),new Vec3(84,0,0))==0,"内域边缘不触发外域禁出");
        check(nested.crossings(outer.id(),new Vec3(-250,0,0),new Vec3(250,0,0))==3,"高速穿过完整外边界检测双向");
        var suppressed=GhostDomainConflict.solve(List.of(node(1,0,200,1000,5),inner));
        check(suppressed.radius(inner.id())==0,"差三级且源头进入时完全压制");
        check(GhostDomainConflict.solve(List.of(inner)).radius(inner.id())==64,"压制解除恢复原始范围");
        var high=node(2,80,64,100,4);
        check(GhostDomainConflict.solve(List.of(outer,high)).radius(high.id())==64,"高等级内域不收缩");
        var a=node(1,0,10,100,2);var b=node(2,15,10,300,2);
        var shared=GhostDomainConflict.solve(List.of(a,b));Vec3 intersection=new Vec3(7.5,0,0);
        check(shared.weight(a.id(),intersection)==.25&&shared.weight(b.id(),intersection)==.75,"同级交界双方按强度分摊");
        check(shared.weight(a.id(),Vec3.ZERO)==1,"非交界强度保持完整");
        double slight=GhostDomainConflict.edgeRadius(64,100,1000,1,.01);
        double half=GhostDomainConflict.edgeRadius(64,100,1000,1,.5);
        double almost=GhostDomainConflict.edgeRadius(64,100,1000,1,.99);
        check(Math.abs(slight-60.48)<1e-9,"1%覆盖时保留99%的计算范围");
        check(slight>half&&half>almost&&almost>0,"覆盖面积越大压制越高，剩余范围单调减少");
        check(GhostDomainConflict.edgeRadius(64,100,1000,1,1)==0,"完全覆盖时剩余范围为零");
        var higher=node(2,15,10,300,3);var edge=GhostDomainConflict.solve(List.of(a,higher));
        check(edge.radius(a.id())>0&&edge.radius(a.id())<10,"外部低等级范围缩小");
        var reversed=GhostDomainConflict.solve(List.of(higher,a));
        check(edge.radius(a.id())==reversed.radius(a.id()),"交换输入顺序不改变结果");
        var gap=GhostDomainConflict.solve(List.of(a,node(2,15,10,300,5)));
        check(gap.radius(a.id())==10&&!gap.owns(a.id(),intersection)&&gap.owns(a.id(),Vec3.ZERO),"外部三级差仅覆盖交界");
        var separate=GhostDomainConflict.solve(List.of(a,node(2,30,10,300,3)));
        check(separate.radius(a.id())==10,"无交集保留原始范围");
        var top=node(3,80,10,1000,8);var mid=node(2,80,100,300,5);var low=node(1,0,10,100,2);
        check(GhostDomainConflict.solve(List.of(top,mid,low)).radius(low.id())==10,"完全压制的中间域不能压制第三域");
        Runnable[] deferred={null};GhostDomainInfluenceScope.run(.25,()->deferred[0]=GhostDomainInfluenceScope.capture(
            ()->check(GhostDomainInfluenceScope.weight()==.25,"延迟事实保持本次倍率")));
        deferred[0].run();check(GhostDomainInfluenceScope.weight()==1,"作用域退出恢复倍率");
        System.out.println("Domain conflict geometry, overlap reduction, defaults, layering and deferred weight checks passed");
    }
}
