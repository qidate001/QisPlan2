package com.qidate.qisplan2.ghost.module.implementation;

import java.util.Set;
import com.qidate.qisplan2.ghost.module.GhostMedium;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.ability.DefinedGhostAbility;
import com.qidate.qisplan2.ghost.module.event.EntityUsedEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppression;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** 完整棺材钉模块：插入能力建立附着关系，插入媒介持续成立便产生无解压制。 */
public final class CoffinNailModule implements GhostModule {
    public static final ResourceLocation ID = ResourceLocation.parse("qisplan2:coffin_nail");
    public static final ResourceLocation EMBED = ResourceLocation.parse("qisplan2:coffin_nail/embed");
    /** 物理交互由通用输入桥投递，不依赖棺材钉物品类。 */
    @Override public boolean supportsEntityUse() { return true; }
    /** 立体外观独立于背包物品模型。 */
    @Override public ResourceLocation attachmentModel() { return ResourceLocation.parse("qisplan2:item/coffin_nail_embedded"); }
    /** 返回模块类型，不表示具体插入来源的身份。 */
    @Override public ResourceLocation getId() { return ID; }
    /** 交互是手动调用，死机不阻止手动使用，但压制及媒介仍由执行器检查。 */
    @Override public Set<Class<? extends GhostEvent>> abilityEventTypes() { return Set.of(EntityUsedEvent.class); }

    /** 仅接收已声明订阅的持续能力事实。 */
    public void onAbilityEvent(GhostModuleContext module, GhostEvent event) {
        if (event instanceof EntityUsedEvent) module.continueAbility(EMBED, event);
    }
    /** 附着关系变化时读取一次，不按 tick 重放压制或刷新倒计时。 */
    @Override public List<GhostSuppression> getSuppressions(GhostModuleContext module) {
        return module.world().attachments().embedded() ? List.of(GhostSuppression.absolute()) : List.of();
    }
    /** 插入的硬性媒介由抽象端口解释，所有调用渠道统一校验。 */
    @Override public List<GhostMedium> getMedia(GhostAbilityContext ignored) {
        return List.of(new GhostMedium() {
            /** 媒介标识稳定，不绑定具体物品类。 */
            public ResourceLocation getId() { return ResourceLocation.parse("qisplan2:insertable_surface"); }
            /** 拒绝不可达、无模块或没有插入能力的情况。 */
            public boolean allows(GhostAbilityContext context) {
                return context.getModuleContext().world().attachments().canEmbed(target(context));
            }
        });
    }
    /** 插入能力与具体物品无关，能否实施由世界端口决定。 */
    @Override public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(new DefinedGhostAbility(EMBED, ResourceLocation.parse("qisplan2:textures/item/coffin_nail.png"),
                c -> c.getModuleContext().world().attachments().canEmbed(target(c)),
                c -> c.getModuleContext().world().attachments().embed(target(c))));
    }
    /** 交互目标优先，否则使用通用瞄准接口；目标不是模块自己的宿主。 */
    private static LivingEntity target(GhostAbilityContext context) {
        return context.event() instanceof EntityUsedEvent event ? event.target()
                : context.getModuleContext().world().aimedTarget(3);
    }
}
