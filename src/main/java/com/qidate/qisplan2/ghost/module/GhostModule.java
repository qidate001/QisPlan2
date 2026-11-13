package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.bearing.GhostModuleForm;

import com.qidate.qisplan2.ghost.bearing.SpiritualPart;
import com.qidate.qisplan2.ghost.module.environment.GhostEnvironmentalEffect;
import com.qidate.qisplan2.ghost.module.environment.GhostBehavior;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.ghost.module.sound.GhostSoundDefinition;
import com.qidate.qisplan2.ghost.possession.classification.GhostClassification;
import com.qidate.qisplan2.ghost.possession.classification.GhostTag;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostDomainData;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 灵异模块定义。
 *
 * 模块负责声明自身的身份、初始属性、
 * 自然复苏规则、规则定义以及能力定义。
 *
 * 初始属性只用于创建新的模块实例，
 * 不代表已经存在的实例当前保存的属性。
 *
 * 自然复苏速度与复苏值上限属于模块定义，
 * 不属于模块实例的当前状态。
 */
public interface GhostModule {
    /** 厉鬼默认独立存在；替换式定义覆写此声明，模块不需要知道宿主是什么。 */
    default GhostModuleForm form() { return GhostModuleForm.independent(); }

    /**
     * 声明需要的自主行动类型。
     * 具体 Goal 的创建、资源竞争和停止清理由行动适配层负责。
     */
    default GhostBehavior behavior() { return GhostBehavior.NONE; }

    /** 可被点燃是模块性质，点燃操作由输入适配器提供。 */
    default boolean supportsIgnition() { return false; }

    /**
     * 声明能力输出的环境效果类型，供世界系统统一查询。
     */
    default GhostEnvironmentalEffect environmentalEffect() {
        return GhostEnvironmentalEffect.NONE;
    }

    /**
     * 获取模块的唯一标识。
     */
    ResourceLocation getId();

    /**
     * 获取模块定义提供的初始属性。
     */
    default GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(
                1.0D,
                0.0D
        );
    }

    /**
     * 获取该厉鬼模块声明的灵异防御基础值。
     *
     * 运行时根据当前安装的模块重新计算防御；
     * 多模块宿主采用各模块声明值中的最高值，避免安装顺序改变结果。
     * 默认值与通用模块化宿主的基础防御保持一致。
     */
    default double getSupernaturalDefense() {
        return 4.0D;
    }

    /**
     * 获取自然复苏速度。
     *
     * 单位：复苏值/秒。
     *
     * 默认每秒增加 1 点复苏值。
     * 返回 0 表示默认不进行自然复苏。
     *
     * 具体的自然复苏条件由状态运行机制决定。
     */
    default double getNaturalRevivalRate() {
        return 1.0D;
    }

    /**
     * 获取复苏值上限。
     *
     * 默认上限为 100。
     *
     * 状态运行机制负责校验该定义并限制实例的复苏值。
     */
    default double getMaxRevival() {
        return 100.0D;
    }

    /**
     * 获取此模块定义的规则列表。
     *
     * 默认没有规则，保证现有模块兼容。
     */
    default List<GhostRule> getRules() {
        return List.of();
    }

    /**
     * 获取此模块定义的能力列表。
     *
     * 默认没有能力，保证现有模块兼容。
     */
    default List<GhostAbility> getAbilities() {
        return List.copyOf(getIntrinsicAbilities());
    }

    /**
     * 返回本厉鬼自身的能力定义。
     * 世界效果由这些能力执行，输入桥和行为适配器只负责发起调用。
     */
    default List<GhostAbility> getIntrinsicAbilities() {
        return List.of();
    }

    /**
     * 获取此模块定义的媒介列表。
     *
     * 默认没有媒介，保证现有模块兼容。
     */
    default List<GhostMedium> getMedia() {
        return List.of();
    }

    /**
     * 获取该厉鬼模块的复苏刺激结算策略。
     *
     * 复苏策略负责判断一次能力刺激是否允许结算。
     *
     * 它与宿主的可变性限制不同：
     * - 宿主限制决定数据能不能被修改。
     * - 复苏策略决定当前这次刺激能不能结算。
     *
     * 默认允许结算，以保持现有模块的行为不变。
     * 后续接入死机系统后，可以由具体模块提供自己的策略。
     */
    default GhostRevivalPolicy getRevivalPolicy() {
        return GhostRevivalPolicy.allowAll();
    }

    /**
     * 获取该模块声明的声音能力。
     *
     * 默认没有声音能力，保证现有模块兼容。
     */
    default List<GhostSoundDefinition> getSounds() {
        return List.of();
    }

    /**
     * 这个能力对应的鬼。
     */
    default ResourceLocation id() { return getId(); }

    /**
     * 新获得这只鬼时的初始强度。
     *
     * 只在创建新实例时使用，已有实例的强度由宿主保存的模块数据提供。
     */
    default double initialStrength() {
        return getInitialData().intensity();
    }

    /**
     * 复苏值为 0 时，
     * 这只鬼能够发挥自身本质强度的多少。
     *
     * 默认：
     *
     * 1 / 3
     */
    default double minimumStrengthRatio() {

        return 1.0D / 3.0D;
    }

    /**
     * 新获得这只鬼时的初始鬼域数据。
     *
     * <p>
     * 并不是所有厉鬼都拥有鬼域。
     * </p>
     *
     * <p>
     * 返回 Optional.empty() 表示：
     *
     *     这只鬼没有鬼域。
     *
     * 返回具体数据则表示：
     *
     *     这只鬼拥有鬼域，
     *     并使用返回的数据作为初始鬼域状态。
     * </p>
     *
     * <p>
     * 鬼域强度与厉鬼本体强度是两个独立的数据。
     * </p>
     */
    default Optional<PossessedGhostDomainData> initialDomainData() {
        return Optional.empty();
    }

    /**
     * 玩家看到的鬼名称。
     *
     * 默认使用语言文件。
     */
    default Component displayName() {

        return Component.translatable(
                "module."
                        + id().getNamespace()
                        + "."
                        + id().getPath()
        );
    }

    /**
     * 灵异卡片的大图标。
     *
     * 返回 null 时使用默认占位图标。
     */
    default ResourceLocation iconTexture() {
        return null;
    }

    /**
     * 压制额度的小图标。
     *
     * 返回 null 时使用默认占位图标。
     */
    default ResourceLocation suppressionIconTexture() {
        return null;
    }

    /** 主动能力对目标产生的影响类型，未声明的操作不自动当作袭击。 */
    default GhostInfluenceType activeInfluence() { return null; }

    /**
     * 修改驭鬼者的非灵异伤害减免。
     *
     * 默认不修改。
     */
    default double modifyNonSupernaturalDamageReduction(
            double currentReduction
    ) {

        return currentReduction;
    }

    /**
     * 修改驭鬼者的非灵异伤害减免上限。
     *
     * 默认上限为90%。
     */
    default double modifyNonSupernaturalDamageReductionCap(
            double currentCap
    ) {

        return currentCap;
    }

    /**
     * 修改驭鬼带来的生命上限加成。
     */
    default double modifyMaxHealthBonus(
            double currentBonus
    ) {

        return currentBonus;
    }

    /**
     * 灵异力量侵蚀值
     */
    default GhostCorrosion corrosion() {
        return GhostCorrosion.builder()
                .add(SpiritualPart.GLOBAL, 10)
                .build();
    }

    /**
     * 这个鬼的灵异分类。
     *
     * <p>
     * 用于描述鬼的本质、存在形态以及具体灵异特征。
     * </p>
     */
    default GhostClassification classification() {

        return GhostClassification.empty();
    }

    /**
     * 这个鬼的灵异能够压制的目标分类。
     *
     * <p>
     * 用于描述这个鬼的灵异规则可以针对哪些类型的存在。
     * </p>
     */
    default GhostClassification suppressionTargets() {

        return GhostClassification.empty();
    }

    /**
     * 判断这个鬼的灵异是否能够压制目标鬼。
     *
     * <p>
     * 只要自身的压制目标 Tag 与目标鬼的分类 Tag
     * 存在至少一个交集，就认为可以进行压制。
     * </p>
     */
    default boolean canSuppress(
            GhostModule target
    ) {

        if (target == null) {
            return false;
        }

        GhostClassification targets =
                suppressionTargets();

        GhostClassification targetClassification =
                target.classification();

        for (GhostTag tag : targets.tags()) {

            if (targetClassification.has(tag)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 可分配压制单位数量。
     *
     * 默认没有压制资源。
     */
    default int suppressionUnits() {
        return 0;
    }

    /**
     * 每单位压制资源对应多少基础灵异强度。
     *
     * 默认100。
     */
    default double suppressionUnitStrength() {
        return 100.0D;
    }

    /**
     * 自身压制目标时的效率。
     *
     * 默认 90%。
     */
    default double suppressionEfficiency(
            GhostModule target
    ) {
        return 0.9D;
    }

    /**
     * 被目标反向限制时的效率。
     *
     * 默认是正向的一半。
     */
    default double reverseSuppressionEfficiency(
            GhostModule target
    ) {
        return suppressionEfficiency(target) * 0.5D;
    }

    /** 根据能力上下文选择媒介；媒介只读取语义参数、环境能力和模块状态。 */
    default List<GhostMedium> getMedia(GhostAbilityContext context) {
        return getMedia();
    }
}
