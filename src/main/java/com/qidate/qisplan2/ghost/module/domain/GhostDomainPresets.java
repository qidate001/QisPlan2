package com.qidate.qisplan2.ghost.module.domain;

import com.qidate.qisplan2.ghost.module.ability.*;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/** 模块可选的自动化预设；框架不会隐式把这些能力塞入每一个模块。 */
public final class GhostDomainPresets {
    private GhostDomainPresets() {}
    /** 为当前模块生成稳定的分层能力 ID。 */
    public static ResourceLocation layerId(ResourceLocation module,int layer) { return module.withPath(module.getPath()+"/automation_layer_"+layer); }
    /** 返回送入各层的下拉目录，能力需要由模块同时加入 getIntrinsicAbilities。 */
    public static List<GhostDomainPanel.Automation> layers(ResourceLocation module,int count) {
        List<GhostDomainPanel.Automation> result=new ArrayList<>();
        for(int layer=1;layer<=count;layer++) result.add(new GhostDomainPanel.Automation(layerId(module,layer),"screen.qisplan2.domain.layer_"+layer));
        return List.copyOf(result);
    }
    /** 按需复用的私有能力；目标由服务器自动化适配器提供，始终经过统一能力执行器。 */
    public static List<GhostAbility> layerAbilities(ResourceLocation module,int count) {
        List<GhostAbility> result=new ArrayList<>();
        for(int layer=1;layer<=count;layer++) {
            final int value=layer;
            result.add(new GhostAbility(){
                /** 稳定能力身份。 */
                public ResourceLocation getId(){return layerId(module,value);}
                /** 自动化不是轮盘中的直接技能。 */
                public boolean isPublicSkill(){return false;}
                /** 仅接受可信目标，目标必须仍在本鬼域有效覆盖内，层数必须真实存在。 */
                public boolean canExecute(GhostAbilityContext c){return c.parameters() instanceof GhostTargetParameters p
                        && c.getModuleContext().world().domain().contains(p.target())
                        && c.getModuleContext().world().domain().layer()>=value;}
                /** 通过通用出口设置目标层数。 */
                public void execute(GhostAbilityContext c){executeWithResult(c);}
                /** 实际层数不变时不写入数据，避免持续刷新网络状态。 */
                public boolean executeWithResult(GhostAbilityContext c){return c.getModuleContext().world().domain().setTargetLayer(((GhostTargetParameters)c.parameters()).target(),value);}
            });
        }
        return List.copyOf(result);
    }
    /** 模块选择通用控制能力后取得其显式映射；不根据厉鬼类型分支。 */
    public static Map<String,ResourceLocation> common(ResourceLocation module,ResourceLocation toggle) {
        return Map.of("toggle",toggle,"domain_up",module.withPath(module.getPath()+"/config/domain_up"),
                "domain_down",module.withPath(module.getPath()+"/config/domain_down"),
                "self_up",module.withPath(module.getPath()+"/config/self_up"),"self_down",module.withPath(module.getPath()+"/config/self_down"));
    }
    /** 通用操作也是模块主动选入的真实能力，遵守模块媒介与执行器约束。 */
    public static List<GhostAbility> commonAbilities(ResourceLocation module) {
        List<GhostAbility> result=new ArrayList<>();
        for(String operation:List.of("domain_up","domain_down","self_up","self_down")) {
            result.add(new DefinedGhostAbility(module.withPath(module.getPath()+"/config/"+operation),
                    ResourceLocation.parse("minecraft:textures/item/ender_eye.png"),
                    c->c.getModuleContext().world().domain().exists(),c->{
                        var domain=c.getModuleContext().world().domain();int delta=operation.endsWith("up")?1:-1;
                        return operation.startsWith("self") ? domain.setOccupiedLayer(domain.occupiedLayer()+delta)
                                : domain.setLayer(domain.layer()+delta);
                    }));
        }
        return result;
    }
}
