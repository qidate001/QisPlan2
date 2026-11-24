package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.bearing.SpiritualPart;
import com.qidate.qisplan2.ghost.corrosion.CorrosionMatrix;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

/** 所有宿主共用的侵蚀、属性与强度计算。此处不读取任何玩家或实体类型。 */
public final class GhostHostProperties {
    private GhostHostProperties() {}
    /** 只展开真实组成；GLOBAL 不虚构人形部位，万能宿主通过自身部位承载对应影响。 */
    public static CorrosionMatrix corrosion(GhostModuleHost host) {
        var matrix=new CorrosionMatrix();
        for(var entry:host.getModules()) {
            var module=GhostModuleRegistry.get(entry.id());if(module==null||!host.carriesModule(entry.id()))continue;
            module.corrosion().entries().forEach((requested,amount)->{
                if(requested.equals(SpiritualPart.GLOBAL)) {
                    matrix.add(SpiritualPart.GLOBAL,entry.id(),requested,amount);
                    host.composition().capacities().keySet().forEach(part->matrix.add(part,entry.id(),requested,amount));
                } else {
                    var actual=host.composition().resolve(requested);
                    if(actual!=null)matrix.add(actual,entry.id(),requested,amount);
                }
            });
        }
        return matrix;
    }
    /** 人形采用全局、皮肤、血肉和骨骼平均值；其他组成以其实际身体部位计算。 */
    public static double bodyCorrosion(GhostModuleHost host) {
        var matrix=corrosion(host);var parts=new ArrayList<SpiritualPart>();parts.add(SpiritualPart.GLOBAL);
        for(var part:List.of(SpiritualPart.SKIN,SpiritualPart.FLESH,SpiritualPart.BONE))
            if(host.composition().capacities().containsKey(part))parts.add(part);
        if(parts.size()==1)parts.addAll(host.composition().capacities().keySet());
        return parts.stream().mapToDouble(matrix::total).average().orElse(0);
    }
    /** 普通伤害减免使用同一公式与模块声明，不因载体是玩家而采用不同规则。 */
    public static double damageReduction(GhostModuleHost host) {
        double reduction=1-Math.pow(.5,bodyCorrosion(host)/20),cap=.9;
        for(var entry:host.getModules()){
            var module=GhostModuleRegistry.get(entry.id());if(module==null||!host.carriesModule(entry.id()))continue;
            reduction=module.modifyNonSupernaturalDamageReduction(reduction);cap=module.modifyNonSupernaturalDamageReductionCap(cap);
        }
        return Math.clamp(reduction,0,Math.clamp(cap,0,1));
    }
    /** 只计算加成；实际是否有生命属性由物理宿主适配器决定。 */
    public static double maxHealthBonus(GhostModuleHost host) {
        double health=40*(1-Math.pow(.5,bodyCorrosion(host)/20));
        for(var entry:host.getModules()){
            var module=GhostModuleRegistry.get(entry.id());if(module!=null&&host.carriesModule(entry.id()))health=module.modifyMaxHealthBonus(health);
        }
        return Math.max(0,health);
    }
    /** 统一从模块复苏单位换算发挥比例，不经过旧驭鬼 DTO，也不按宿主身份分支。 */
    public static double effectiveStrength(GhostModuleHost host,ResourceLocation id) {
        var module=GhostModuleRegistry.get(id);if(module==null||!host.carriesModule(id))return 0;
        var entry=host.getModules().stream().filter(e->e.id().equals(id)).findFirst().orElse(null);if(entry==null)return 0;
        // 自主复苏的厉鬼完整发挥；只有受控关系使用复苏比例，任何宿主都可声明此关系。
        // 不能把这条承载关系判断写成 Player 的类型判断，也不能意外削弱所有野生厉鬼。
        if (!host.controlsIntrinsicModules()) return entry.intensity();
        double ratio=Math.clamp(module.minimumStrengthRatio(),0,1);
        double revival=Math.clamp(entry.revival()/Math.max(1,module.getMaxRevival()),0,1);
        return entry.intensity()*(ratio+revival*(1-ratio));
    }
}
