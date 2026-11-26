package com.qidate.qisplan2.ghost.module.implementation;

import java.util.Set;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.GhostMedium;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.GhostUseEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

/** 完整鬼书：保留既有问答功能，使用事实与主动技能共用同一个阅览能力。 */
public final class GhostBookModule implements GhostModule {
    public static final ResourceLocation ID = ResourceLocation.parse("qisplan2:ghost_book");
    public static final ResourceLocation READ = ResourceLocation.parse("qisplan2:ghost_book/read");
    /** 返回鬼书身份；不附加旧版不存在的杀人规则。 */
    public ResourceLocation getId() { return ID; }
    /** 手动使用视作能力调用，死机不禁止阅读，仍须满足交互媒介。 */
    public Set<Class<? extends GhostEvent>> abilityEventTypes() { return Set.of(GhostUseEvent.class); }

    /** 仅接收已声明订阅的持续能力事实。 */
    public void onAbilityEvent(GhostModuleContext c, GhostEvent event) {
        if (event instanceof GhostUseEvent) c.continueAbility(READ,event);
    }
    /** 获取语义事件或可信输入指定的读者，不读取来源载体。 */
    private static LivingEntity reader(GhostAbilityContext c) {
        return c.event() instanceof GhostUseEvent event ? event.user()
                : c.parameters() instanceof GhostInvocationParameters input ? input.recipient() : null;
    }
    /** 公开阅读技能，界面逻辑与客户端配置留在适配层。 */
    public List<GhostAbility> getIntrinsicAbilities() { return List.of(new GhostAbility() {
        /** 返回能力标识。 */
        public ResourceLocation getId() { return READ; }
        /** 使用原有书本物品图标。 */
        public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/book.png"); }
        /** 阅读必须有能够交互的读者和暴露到世界的来源。 */
        public List<GhostMedium> media(GhostAbilityContext ignored) { return List.of(new GhostMedium() {
            /** 返回交互媒介标识。 */
            public ResourceLocation getId() { return ResourceLocation.parse("qisplan2:ghost_book/readable"); }
            /** 具体交互距离和客户端能力由适配器验证。 */
            public boolean allows(GhostAbilityContext c) { return c.getModuleContext().world().canOpenScreen(ID,reader(c)); }
        }); }
        /** 额外条件已由媒介检查。 */
        public boolean canExecute(GhostAbilityContext c) { return true; }
        /** 兼容无结果入口。 */
        public void execute(GhostAbilityContext c) { executeWithResult(c); }
        /** 只请求授权界面，不接触配置、网络服务或载体类型。 */
        public boolean executeWithResult(GhostAbilityContext c) { return c.getModuleContext().world().openScreen(ID,reader(c)); }
    }); }
}
