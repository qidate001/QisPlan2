package com.qidate.qisplan2.ghost.module.ability;

import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/** 通用能力定义容器；条件与效果由模块传入，本类不认识任何具体厉鬼或技能种类。 */
public record DefinedGhostAbility(ResourceLocation getId, ResourceLocation iconTexture,
        Predicate<GhostAbilityContext> requirement, Predicate<GhostAbilityContext> effect) implements GhostAbility {
    /** 模块给出的条件就是本技能自身的执行前提。 */
    public boolean canExecute(GhostAbilityContext context) { return requirement.test(context); }
    /** 兼容无需返回效果结果的调用方。 */
    public void execute(GhostAbilityContext context) { executeWithResult(context); }
    /** 直接调用模块定义的效果，不再交给动作枚举分发。 */
    public boolean executeWithResult(GhostAbilityContext context) { return effect.test(context); }
}
