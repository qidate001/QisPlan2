package com.qidate.qisplan2.ghost.module.implementation.eye;

import com.qidate.qisplan2.ghost.module.environment.GhostActiveAction;
import com.qidate.qisplan2.ghost.module.ability.WorldActionAbility;
import com.qidate.qisplan2.ghost.bearing.SpiritualPart;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.possession.classification.GhostClassification;
import com.qidate.qisplan2.ghost.possession.classification.GhostTag;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostDomainData;
import java.util.Optional;
import java.util.List;
import com.qidate.qisplan2.ghost.module.GhostMedium;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import net.minecraft.resources.ResourceLocation;

/**
 * 鬼眼模块的最小实现。
 *
 * 声明鬼域开关、层数调节、化虹与重启等独立主动能力，效果由环境出口负责。
 * 不声明语义事件、触发规则或额外袭击能力，后续完善鬼眼规则时再扩展。
 * 鬼域持久状态保存在模块 customData，模块不读取具体载体。
 */
public final class GhostEyeModule implements GhostModule {
    /** 显式声明可主动调用的独立技能，宿主差异由世界端口处理。 */
    @Override public List<GhostAbility> getActiveAbilities() {
        return List.of(
            WorldActionAbility.of(ID, "cast_toggle", "minecraft:textures/item/ender_eye.png", GhostActiveAction.TOGGLE_DOMAIN),
            WorldActionAbility.of(ID, "domain_up", "minecraft:textures/item/glowstone_dust.png", GhostActiveAction.DOMAIN_UP),
            WorldActionAbility.of(ID, "domain_down", "minecraft:textures/item/redstone.png", GhostActiveAction.DOMAIN_DOWN),
            WorldActionAbility.of(ID, "self_up", "minecraft:textures/item/feather.png", GhostActiveAction.SELF_UP),
            WorldActionAbility.of(ID, "self_down", "minecraft:textures/item/flint.png", GhostActiveAction.SELF_DOWN),
            WorldActionAbility.of(ID, "reboot", "minecraft:textures/item/clock_00.png", GhostActiveAction.REBOOT),
            WorldActionAbility.of(ID, "teleport", "minecraft:textures/item/ender_pearl.png", GhostActiveAction.TELEPORT),
            WorldActionAbility.of(ID, "raise_target", "minecraft:textures/item/feather.png", GhostActiveAction.RAISE_TARGET),
            WorldActionAbility.of(ID, "lower_target", "minecraft:textures/item/flint.png", GhostActiveAction.LOWER_TARGET)
        );
    }

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("qisplan2", "ghost_eye");
    public static final ResourceLocation TOGGLE_DOMAIN = ResourceLocation.fromNamespaceAndPath("qisplan2", "ghost_eye/toggle_domain");

    /** 无自动规则；主动调用也必须满足鬼域出口媒介。 */
    @Override public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            public ResourceLocation getId() { return TOGGLE_DOMAIN; }

            /**
             * 校验本媒介要求的环境、事件或输入条件。
             * 返回 false 会阻止本次能力执行，主动使用也不能绕过该检查。
             */
            public boolean allows(GhostAbilityContext context) {
                return context.getModuleContext().world().canToggleDomain();
            }
        });
    }

    /**
     * 返回本厉鬼自身的能力定义。
     * 世界效果由这些能力执行，输入桥和行为适配器只负责发起调用。
     */
    @Override public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(new GhostAbility() {
            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            public ResourceLocation getId() { return TOGGLE_DOMAIN; }

            /**
             * 鬼眼切换能力不要求额外事件参数；可用的鬼域出口由媒介检查。
             */
            public boolean canExecute(GhostAbilityContext context) { return true; }

            /**
             * 执行当前能力的效果。
             * 调用方应使用统一执行器，以确保模块状态和媒介已经检查。
             */
            public void execute(GhostAbilityContext context) { executeWithResult(context); }

            /**
             * 执行能力并返回实际效果是否成功。
             * 返回值供主动交互及成功后结算使用，不能等同于仅接受了调用。
             */
            public boolean executeWithResult(GhostAbilityContext context) {
                return context.getModuleContext().world().toggleDomain();
            }
        });
    }

    /**
     * 获取当前定义的唯一标识。
     * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
     */
    @Override public ResourceLocation getId() { return ID; }

    /**
     * 提供新建模块实例的初始强度和复苏值。
     * 仅用于初始化；已存在实例以保存的模块数据为准。
     */
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(100, 0); }

    private static final GhostCorrosion CORROSION =
            GhostCorrosion.builder()
                    .add(SpiritualPart.EYE, 100)
                    .add(SpiritualPart.GLOBAL, 40)
                    .build();

    /**
     * 鬼域。
     */
    @Override
    public Optional<PossessedGhostDomainData> initialDomainData() {
        return Optional.of(
                PossessedGhostDomainData.create(
                        initialStrength(),
                        80.0D
                )
        );
    }

    /**
     * 返回复苏值为零时可发挥的本质强度比例，供有效强度计算使用。
     */
    @Override
    public double minimumStrengthRatio() {
        return 0.50D;
    }

    /**
     * 返回本厉鬼对各身体部位和全局的侵蚀配置。
     */
    @Override
    public GhostCorrosion corrosion() {
        return CORROSION;
    }

    /**
     * 声明本厉鬼的灵异分类标签，供压制关系匹配使用。
     */
    @Override
    public GhostClassification classification() {

        return GhostClassification.of(
                GhostTag.ENTITY,
                GhostTag.EXISTENCE_FORM,
                GhostTag.ORGAN,
                GhostTag.EYE
        );
    }

    /**
     * 声明能够压制的目标标签；空集合表示没有额外的标签压制对象。
     */
    @Override
    public GhostClassification suppressionTargets() {

        return GhostClassification.of(
                GhostTag.ENTITY
        );
    }

    /**
     * 返回可分配的压制单位数量，供驾驭压制配置使用。
     */
    @Override
    public int suppressionUnits() {
        return 10;
    }

    /**
     * 返回每个压制单位对应的基础灵异强度。
     */
    @Override
    public double suppressionUnitStrength() {
        return 100.0D;
    }


}
