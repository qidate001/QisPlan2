package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.domain.GhostDomainManager;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import net.minecraft.world.entity.player.Player;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.host.player.PlayerAbilityParameters;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleContext;
import com.qidate.qisplan2.ghost.module.host.player.PlayerModuleAbility;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.ghost.module.sound.GhostSoundDefinition;
import com.qidate.qisplan2.ghost.possession.classification.GhostClassification;
import com.qidate.qisplan2.ghost.possession.classification.GhostTag;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostDomainData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;

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
    /** 物品宿主的服务端生命周期；用于燃烧等持续状态，不发布虚构的语义事件。 */
    default void onItemTick(GhostItemModuleHost host,
            Player holder) {}


    /**
     * 获取模块的唯一标识。
     */
    ResourceLocation getId();
    /** 每个服务端实体 tick 调用，包括休眠、死机状态，用于管理模块外部资源。 */
    default void onEntityTick(ModularGhostEntity host) {}

    /** 模块卸载或宿主离开世界时释放运行时资源；不得清空持久化成长数据。 */
    default void onEntityDetached(ModularGhostEntity host) {}


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
     * 为实体宿主创建独立的行为实例。宿主安装模块时挂载，卸载时停止并移除。
     * 列表顺序决定行为优先级；没有实体行为的模块返回空列表。
     */
    default List<Goal> createEntityGoals(
            ModularGhostEntity host) {
        return List.of();
    }

    /**
     * 获取该厉鬼模块声明的灵异防御基础值。
     *
     * ModularGhostEntity 会根据宿主当前安装的模块重新计算防御；
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
        ArrayList<GhostAbility> abilities = new ArrayList<>(getIntrinsicAbilities());
        abilities.add(new PlayerModuleAbility(this));
        return List.copyOf(abilities);
    }

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
     * 这个能力对应的实体类型。
     *
     * 默认情况下不直接要求每个 Ability
     * 重复声明，破体系统会通过 Registry
     * 根据 id() 查找。
     *
     * 如果以后出现：
     *
     * Ability ID ≠ Entity ID
     *
     * 可以在具体 Ability 中覆盖这个方法。
     */


    /*
     * ============================================================
     * 厉鬼强度
     * ============================================================
     */

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

    /*
     * ============================================================
     * 鬼域
     * ============================================================
     */

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


    /*
     * ============================================================
     * 基础信息
     * ============================================================
     */

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


    /*
     * ============================================================
     * 驾驭生命周期
     * ============================================================
     */

    /**
     * 玩家刚刚驾驭这只鬼。
     */
    default void onPossess(
            PlayerGhostModuleContext context
    ) {
    }


    /**
     * 玩家宿主离线或模块暂时不可用时，释放鬼域等运行时资源。
     */
    default void onPlayerSuspended(PlayerGhostModuleContext context) {
        GhostDomainManager.get(context.player().serverLevel()).removeBySourceAndType(context.player().getUUID(), getId());
    }

    /** 主动能力对目标产生的影响类型，未声明的操作不自动当作袭击。 */
    default GhostInfluenceType playerActionInfluence() { return null; }

    /** 玩家宿主上的持续效果；不为玩家创建第二份模块状态。 */
    default void onPlayerTick(
            PlayerGhostModuleContext context
    ) {
    }


    /*
     * ============================================================
     * 主动能力
     * ============================================================
     */

    /**
     * 使用主动能力。
     */
    default boolean use(
            PlayerGhostModuleContext context
    ) {

        return false;
    }


    /**
     * 对方块使用主动能力。
     */
    default boolean useOnBlock(
            PlayerGhostModuleContext context,
            BlockPos pos
    ) {

        return false;
    }


    /*
     * ============================================================
     * 解除驾驭
     * ============================================================
     */

    /**
     * 玩家解除驾驭时调用。
     */
    default void onRelease(
            PlayerGhostModuleContext context
    ) { }


    /*
     * ============================================================
     * 非灵异伤害减免
     * ============================================================
     */

    /**
     * 修改驭鬼者的非灵异伤害减免。
     *
     * 默认不修改。
     */
    default double modifyNonSupernaturalDamageReduction(
            ServerPlayer player,
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
            ServerPlayer player,
            double currentCap
    ) {

        return currentCap;
    }

    /**
     * 修改驭鬼带来的生命上限加成。
     */
    default double modifyMaxHealthBonus(
            ServerPlayer player,
            double currentBonus
    ) {

        return currentBonus;
    }

    /**
     * 灵异力量侵蚀值
     */
    default GhostCorrosion corrosion() {
        return GhostCorrosion.builder()
                .add(CorrosionType.GLOBAL, 10)
                .build();
    }

    /*
     * ============================================================
     * 压制Tag
     * ============================================================
     */

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

    /*
     * ============================================================
     * 压制资源
     * ============================================================
     */

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

    /*
     * ============================================================
     * 压制效率
     * ============================================================
     */

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

    /** 根据调用宿主选择媒介；玩家主动调用使用模块的玩家目标检查。 */
    default List<GhostMedium> getMedia(GhostAbilityContext context) {
        if (context.parameters() instanceof PlayerAbilityParameters)
            return List.of(PlayerModuleAbility.MEDIUM);
        return getMedia();
    }
}
