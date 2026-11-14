package com.qidate.qisplan2.ghost.module.ability;

import com.qidate.qisplan2.ghost.module.GhostMedium;
import com.qidate.qisplan2.ghost.module.environment.GhostActiveAction;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * 由模块显式声明的主动能力。名称、图标、效果种类均属于能力定义。
 * 能力只调用抽象世界端口，宿主差异及输入目标解析留在适配层。
 */
public record WorldActionAbility(ResourceLocation getId, ResourceLocation iconTexture,
        GhostActiveAction action) implements GhostAbility {
    /** 主动能力可以被轮盘或未来其他输入渠道选择。 */
    public boolean manuallySelectable() { return true; }

    /** 本能力拥有自己的完整媒介，不沿用同模块中听声或接触规则的事件媒介。 */
    public boolean inheritsModuleMedia() { return false; }

    /** 每次执行都要求适配层确认当前操作条件，而不是信任客户端是否点亮图标。 */
    public List<GhostMedium> media(GhostAbilityContext context) {
        return List.of(new GhostMedium() {
            /** 媒介标识与当前能力对应。 */
            public ResourceLocation getId() { return WorldActionAbility.this.getId; }
            /** 只查询环境，不执行效果、不改变状态。 */
            public boolean allows(GhostAbilityContext call) {
                return call.getModuleContext().world().canActivate(action);
            }
        });
    }

    /** 主动输入不伪造规则语义事件；具体条件已由能力媒介负责。 */
    public boolean canExecute(GhostAbilityContext context) { return true; }

    /** 兼容不需要结果的统一执行入口。 */
    public void execute(GhostAbilityContext context) { executeWithResult(context); }

    /** 返回真实操作结果，失败时不应被网络层当作成功释放。 */
    public boolean executeWithResult(GhostAbilityContext context) {
        return context.getModuleContext().world().activate(action);
    }

    /** 在模块自身命名空间中声明独立技能 ID，图标可复用已有原版或模组纹理。 */
    public static WorldActionAbility of(ResourceLocation module, String name, String icon, GhostActiveAction action) {
        return new WorldActionAbility(ResourceLocation.fromNamespaceAndPath(module.getNamespace(),
                module.getPath() + "/" + name), ResourceLocation.parse(icon), action);
    }
}
