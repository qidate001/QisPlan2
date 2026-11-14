package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.ghost.module.ability.GhostInvocationParameters;
import net.minecraft.world.entity.LivingEntity;
import com.qidate.qisplan2.core.ModTags;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.GhostUseEvent;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 鬼墓碑模块。
 *
 * 响应灵异使用事件，并请求进行题字。
 */
public final class GhostTombstoneModule implements GhostModule {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_tombstone"
            );

    private static final ResourceLocation USE_RULE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_tombstone/use_rule"
            );

    private static final ResourceLocation INSCRIPTION_ABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_tombstone/inscription"
            );

    private static final List<GhostRule> RULES =
            List.of(new TombstoneUseRule());

    private static final List<GhostAbility> ABILITIES =
            List.of(new InscriptionAbility());

    /**
     * 获取当前定义的唯一标识。
     * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
     */
    @Override
    public ResourceLocation getId() {
        return ID;
    }

    /**
     * 提供新建模块实例的初始强度和复苏值。
     * 仅用于初始化；已存在实例以保存的模块数据为准。
     */
    @Override
    public GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(
                25.0D,
                0.0D
        );
    }

    /**
     * 声明本厉鬼响应的语义事件及对应规则。
     * 规则负责决定何时发起能力或提交影响，不绕过执行器直接完成袭击。
     */
    @Override
    public List<GhostRule> getRules() {
        return RULES;
    }

    /**
     * 返回本厉鬼自身的能力定义。
     * 世界效果由这些能力执行，输入桥和行为适配器只负责发起调用。
     */
    @Override
    public List<GhostAbility> getIntrinsicAbilities() {
        return ABILITIES;
    }

    private static final class TombstoneUseRule
            implements GhostRule {

        /**
         * 获取当前定义的唯一标识。
         * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
         */
        @Override
        public ResourceLocation getId() {
            return USE_RULE_ID;
        }

        /**
         * 判断当前语义事件是否属于本规则处理的范围。
         * 这里只筛选事件种类或语义，不执行能力和世界操作。
         */
        @Override
        public boolean supports(GhostEvent event) {
            return event instanceof GhostUseEvent;
        }

        /**
         * 检查当前事实和模块上下文是否满足规则触发条件。
         * 返回 false 时跳过本规则，不提交能力或灵异影响。
         */
        @Override
        public boolean canTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            return event instanceof GhostUseEvent
                    && context.getModuleContext().world().canInscribe();
        }

        /**
         * 处理已满足条件的语义事件，发起对应能力或提交灵异影响。
         * 需要裁决的效果仅在获准后执行。
         */
        @Override
        public void onTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            context.tryExecuteAbility(
                    INSCRIPTION_ABILITY_ID,
                    event
            );
        }
    }

    private static final class InscriptionAbility
            implements GhostAbility {
            /** 技能图标由所属模块直接声明。 */
            @Override public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/feather.png"); }


        /**
         * 获取当前定义的唯一标识。
         * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
         */
        @Override
        public ResourceLocation getId() {
            return INSCRIPTION_ABILITY_ID;
        }

        /**
         * 检查本次能力调用的事件、参数或环境是否可用于执行。
         * 模块可用性与媒介由统一执行器共同校验。
         */
        @Override
        public boolean canExecute(GhostAbilityContext context) {
            return recipient(context) instanceof Player player
                    && context.getModuleContext().world().canInscribe()
                    && isInscribable(player.getMainHandItem());
        }

        /**
         * 执行当前能力的效果。
         * 调用方应使用统一执行器，以确保模块状态和媒介已经检查。
         */
        @Override
        public void execute(GhostAbilityContext context) {
            if (!context.isModuleAvailable()) {
                return;
            }


            if (!context.getModuleContext().world().canInscribe()) {
                return;
            }

            if (!(recipient(context) instanceof Player player)) {
                return;
            }

            ItemStack stack = player.getMainHandItem();

            if (!isInscribable(stack)) {
                return;
            }

            context.getModuleContext().world().inscribe(recipient(context));
        }

        /**
         * 检查物品是否带有鬼墓碑题字标签，作为题字操作的输入限制。
         */
        private static boolean isInscribable(ItemStack stack) {
            return stack.is(ModTags.Items.GHOST_TOMBSTONE_INSCRIBABLE);
        }
    }    /** 自动交互与主动输入共用同一个题字对象，不读取模块宿主。 */
    private static LivingEntity recipient(GhostAbilityContext context) {
        if (context.event() instanceof GhostUseEvent event) return event.user();
        return context.parameters() instanceof GhostInvocationParameters input ? input.recipient() : null;
    }

}
