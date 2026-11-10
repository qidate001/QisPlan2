package com.qidate.qisplan2.ghost.module.environment;

import com.qidate.qisplan2.ghost.module.GhostMedium;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import net.minecraft.resources.ResourceLocation;

/** 所有模块共用的“暴露在世界”媒介，不询问暴露是如何实现的。 */
public final class ExposedMedium implements GhostMedium {
    public static final ExposedMedium INSTANCE = new ExposedMedium();

    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
    private ExposedMedium() {}

    /**
     * 获取当前定义的唯一标识。
     * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
     */
    @Override public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath("qisplan2", "exposed"); }

    /**
     * 校验本媒介要求的环境、事件或输入条件。
     * 返回 false 会阻止本次能力执行，主动使用也不能绕过该检查。
     */
    @Override public boolean allows(GhostAbilityContext context) { return context.getModuleContext().environment().exposed(); }
}
