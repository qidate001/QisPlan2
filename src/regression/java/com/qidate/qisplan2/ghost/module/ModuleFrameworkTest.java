package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.bearing.HostComposition;
import com.qidate.qisplan2.ghost.bearing.SpiritualPart;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.environment.GhostBehavior;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** 同一完整模块在不同实现的宿主上必须采用同一套数据、属性与生命周期语义。 */
public final class ModuleFrameworkTest {
    private static final ResourceLocation ID=ResourceLocation.parse("qisplan2_test:whole_ghost");
    private static int factories;
    private ModuleFrameworkTest() {}
    /** 有持久写能力的假宿主，不创建任何玩家或 Minecraft 实体。 */
    private static class Host implements GhostModuleDataModifier,GhostModuleCustomDataModifier {
        GhostModuleData.Entry entry=new GhostModuleData.Entry(ID,30,0);
        boolean controlled;
        int lost;
        public List<GhostModuleData.Entry> getModules(){return entry==null?List.of():List.of(entry);}
        public HostComposition composition(){return HostComposition.HUMANOID;}
        public boolean controlsIntrinsicModules(){return controlled;}
        public void onControlLost(ResourceLocation id){lost++;entry=null;}
        public boolean addModule(ResourceLocation id,double strength){entry=new GhostModuleData.Entry(id,strength,0);return true;}
        public boolean removeModule(ResourceLocation id){entry=null;return true;}
        public boolean setIntensity(ResourceLocation id,double strength){entry=new GhostModuleData.Entry(id,strength,entry.revival(),entry.sleeping(),entry.deadlocked(),entry.customData());return true;}
        public boolean setRevival(ResourceLocation id,double value){entry=new GhostModuleData.Entry(id,entry.intensity(),value,entry.sleeping(),entry.deadlocked(),entry.customData());return true;}
        public boolean setSleeping(ResourceLocation id,boolean value){entry=entry.withSleeping(value);return true;}
        public boolean setDeadlocked(ResourceLocation id,boolean value){entry=new GhostModuleData.Entry(id,entry.intensity(),entry.revival(),entry.sleeping(),value,entry.customData());return true;}
        public CompoundTag getModuleCustomData(ResourceLocation id){return entry.customData();}
        public boolean setModuleCustomData(ResourceLocation id,CompoundTag data){entry=entry.withCustomData(data);return true;}
    }
    /** 不同类型同样提供人形组成，不能因此得到另一套减伤公式。 */
    private static final class OtherHost extends Host {}
    /** 比较有限数值并报告差异。 */
    private static void equal(double actual,double expected,String message){if(Math.abs(actual-expected)>1e-8)throw new AssertionError(message+": "+actual+" != "+expected);}
    /** 注册一个新模块便具有自主行为，无需编辑宿主分发表。 */
    public static void run(){
        GhostModule definition=new GhostModule(){
            public ResourceLocation getId(){return ID;}
            public GhostCorrosion corrosion(){return GhostCorrosion.builder().add(SpiritualPart.GLOBAL,20).build();}
            public GhostBehavior behavior(){return context->{factories++;return List.of();};}
        };
        GhostModuleRegistry.register(definition);
        Host first=new Host(),second=new OtherHost();
        equal(GhostHostProperties.damageReduction(first),.5,"通用减伤公式");
        equal(GhostHostProperties.damageReduction(first),GhostHostProperties.damageReduction(second),"宿主身份不能改变减伤");
        equal(GhostHostProperties.maxHealthBonus(first),GhostHostProperties.maxHealthBonus(second),"宿主身份不能改变生命加成");
        equal(GhostHostProperties.effectiveStrength(first,ID),GhostHostProperties.effectiveStrength(second,ID),"宿主身份不能改变强度");
        equal(GhostHostProperties.effectiveStrength(first,ID),30,"自主模块保留完整强度");
        first.controlled=true;second.controlled=true;
        equal(GhostHostProperties.effectiveStrength(first,ID),GhostHostProperties.effectiveStrength(second,ID),"受控关系也必须跨宿主一致");
        for(var host:List.of(first,second)){
            definition.behavior().create(new GhostModuleContext(host,ID));
            GhostModuleOperations.addRevivalReserve(host,10);
            new GhostModuleContext(host,ID).addRevival(15);
            equal(host.entry.revival(),5,"缓冲在所有宿主统一抵消复苏");
            host.controlled=true;host.setRevival(ID,100);
            GhostModuleRuntime.dispatch(host,new ModuleTickEvent());
            if(host.lost!=1||!host.getModules().isEmpty())throw new AssertionError("受控宿主必须收到统一失控回调");
        }
        if(factories!=2)throw new AssertionError("行为工厂必须按实例调用");
        CompoundTag legacy=new CompoundTag(),state=new CompoundTag();state.putDouble("shallowStun",23);legacy.put("qisplan2:player",state);legacy.putString("customSentinel","keep");
        var migrated=new GhostModuleData.Entry(ID,30,0,false,true,legacy);
        equal(migrated.customData().getDouble("revivalReserve"),23,"旧缓冲必须随模块迁移");
        if(!migrated.deadlocked()||!migrated.customData().getString("customSentinel").equals("keep")||!legacy.getCompound("qisplan2:player").contains("shallowStun"))
            throw new AssertionError("迁移必须保留死机和独有数据，且不能修改输入标签");
        System.out.println("Shared host properties, module factories, revival reserve and control lifecycle checks passed");
    }
}
