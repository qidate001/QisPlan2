package com.qidate.qisplan2.ghost.module.implementation;


import com.qidate.qisplan2.ghost.module.ability.GhostAbilityStimulation;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.module.environment.GhostWorldAccess;
import net.minecraft.world.phys.Vec3;
import com.qidate.qisplan2.ghost.bearing.SpiritualPart;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.DoorChangedEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.ghost.possession.classification.GhostClassification;
import com.qidate.qisplan2.ghost.possession.classification.GhostTag;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 开门鬼的完整模块定义。
 *
 * 该模块自身代表一只完整的开门鬼：规则识别开门事件，媒介
 * 限制袭击必须由有效门事件触发，能力负责瞬移和裁决后的袭击。
 */
public final class OpeningGhostModule implements GhostModule {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MODID, "opening_ghost");
    public static final double INITIAL_INTENSITY = 5.0D;
    private static final double TRIGGER_RADIUS = 100.0D;
    private static final ResourceLocation MEDIUM_ID = ResourceLocation.fromNamespaceAndPath(MODID, "opening_ghost/door");
    private static final ResourceLocation RULE_ID = ResourceLocation.fromNamespaceAndPath(MODID, "opening_ghost/open_door_rule");
    private static final ResourceLocation ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MODID, "opening_ghost/door_attack");
    private static final List<GhostMedium> MEDIA = List.of(new OpenDoorMedium());
    private static final List<GhostRule> RULES = List.of(new OpenDoorRule());
    private static final List<GhostAbility> ABILITIES = List.of(new DoorAttackAbility());

    /**
     * 获取当前定义的唯一标识。
     * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
     */
    @Override public ResourceLocation getId() { return ID; }

    /**
     * 提供新建模块实例的初始强度和复苏值。
     * 仅用于初始化；已存在实例以保存的模块数据为准。
     */
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(INITIAL_INTENSITY, 0.0D); }

    /**
     * 返回灵异防御值，供伤害结算使用。
     */
    @Override public double getSupernaturalDefense() { return 6.0D; }

    /**
     * 声明能力执行前必须满足的媒介。
     * 主动调用同样需要通过媒介；规则是否触发由规则层单独判断。
     */
    @Override public List<GhostMedium> getMedia() { return MEDIA; }

    /**
     * 声明本厉鬼响应的语义事件及对应规则。
     * 规则负责决定何时发起能力或提交影响，不绕过执行器直接完成袭击。
     */
    @Override public List<GhostRule> getRules() { return RULES; }

    /**
     * 返回本厉鬼自身的能力定义。
     * 世界效果由这些能力执行，输入桥和行为适配器只负责发起调用。
     */
    @Override public List<GhostAbility> getIntrinsicAbilities() { return ABILITIES; }

    /** 只接受门从关闭变为打开的事件，避免其他方块变化触发开门鬼规则。 */
    private static boolean isOpenEvent(GhostEvent event) {
        return event instanceof DoorChangedEvent doorEvent && doorEvent.opening();
    }

    /**
     * 校验门事件的来源与方块状态。规则只在服务端处理仍存活的触发者，
     * 并确认事件确实对应带朝向属性的门方块，避免无效事件进入裁决流程。
     */
    private static boolean isValidDoorEvent(DoorChangedEvent event) {
        return event.source().isAlive()
                && event.source().level() instanceof ServerLevel
                && event.doorState().getBlock() instanceof DoorBlock
                && event.doorState().hasProperty(DoorBlock.FACING);
    }

    /** 检查环境是否有效、与门同维度，且处于门事件的触发范围内。 */
    private static boolean isInRange(GhostModuleContext context, DoorChangedEvent event) {
        var world = context.world();
        return world.present() && world.sameWorld(event.source().level())
                && world.position().distanceToSqr(Vec3.atCenterOf(event.doorPos())) <= TRIGGER_RADIUS * TRIGGER_RADIUS;
    }

    private static final class OpenDoorMedium implements GhostMedium {
        /**
         * 获取当前定义的唯一标识。
         * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
         */
        @Override public ResourceLocation getId() { return MEDIUM_ID; }

        /**
         * 校验本媒介要求的环境、事件或输入条件。
         * 返回 false 会阻止本次能力执行，主动使用也不能绕过该检查。
         */
        @Override public boolean allows(GhostAbilityContext context) {
            if (context.event() == null) return context.getModuleContext().world().doorMarkedTarget(true) != null;
            // 媒介是能力执行前的硬性门槛：只有活着的玩家在服务端打开有效门，
            // 且来源暴露在世界并处于触发范围内时，能力才可执行。
            return context.event() instanceof DoorChangedEvent event
                    && isOpenEvent(event) && isValidDoorEvent(event)
                    && isInRange(context.getModuleContext(), event);
        }
    }

    private static final class OpenDoorRule implements GhostRule {
        /**
         * 获取当前定义的唯一标识。
         * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
         */
        @Override public ResourceLocation getId() { return RULE_ID; }

        /**
         * 判断当前语义事件是否属于本规则处理的范围。
         * 这里只筛选事件种类或语义，不执行能力和世界操作。
         */
        @Override public boolean supports(GhostEvent event) { return isOpenEvent(event); }

        /**
         * 检查当前事实和模块上下文是否满足规则触发条件。
         * 返回 false 时跳过本规则，不提交能力或灵异影响。
         */
        @Override public boolean canTrigger(GhostRuleContext context, GhostEvent event) {
            return event instanceof DoorChangedEvent doorEvent && isOpenEvent(event)
                    && isValidDoorEvent(doorEvent) && isInRange(context.getModuleContext(), doorEvent);
        }

        /**
         * 处理已满足条件的语义事件，发起对应能力或提交灵异影响。
         * 需要裁决的效果仅在获准后执行。
         */
        @Override public void onTrigger(GhostRuleContext context, GhostEvent event) {
            if (!(event instanceof DoorChangedEvent doorEvent)) return;
            // 规则只提交袭击影响，不直接杀死触发者。裁决系统会汇总同一事件内
            // 所有模块的影响并统一计算结果；只有该能力通过裁决后才会瞬移并执行袭击。
            context.submitAbilityInfluence(ABILITY_ID, event, doorEvent.source().getUUID(),
                    GhostInfluenceType.ATTACK, context.getModuleContext().intensity());
        }
    }

    private static final class DoorAttackAbility implements GhostAbility {
        /** 技能图标由所属模块直接声明。 */
        @Override public ResourceLocation iconTexture() { return ResourceLocation.parse("qisplan2:textures/item/ghost_door.png"); }

        /**
         * 获取当前定义的唯一标识。
         * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
         */
        @Override public ResourceLocation getId() { return ABILITY_ID; }

        /** 主动追袭保留五点复苏代价，规则触发不重复结算。 */
        @Override public GhostAbilityStimulation getStimulation(GhostAbilityContext context) {
            return context.source() == GhostAbilityContext.Source.POSSESSION
                    ? new GhostAbilityStimulation(5, GhostAbilityStimulation.Timing.ON_ACCEPTED)
                    : GhostAbilityStimulation.none();
        }

        /**
         * 检查本次能力调用的事件、参数或环境是否可用于执行。
         * 模块可用性与媒介由统一执行器共同校验。
         */
        @Override public boolean canExecute(GhostAbilityContext context) {
            if (context.event() == null) return context.getModuleContext().world().doorMarkedTarget(true) != null;
            return context.event() instanceof DoorChangedEvent event && isOpenEvent(event)
                    && isValidDoorEvent(event) && isInRange(context.getModuleContext(), event);
        }

        /**
         * 执行当前能力的效果。
         * 调用方应使用统一执行器，以确保模块状态和媒介已经检查。
         */
        @Override public void execute(GhostAbilityContext context) { executeWithResult(context); }

        /**
         * 执行能力并返回实际效果是否成功。
         * 返回值供主动交互及成功后结算使用，不能等同于仅接受了调用。
         */
        @Override public boolean executeWithResult(GhostAbilityContext context) {
            if (context.event() == null) {
                var module = context.getModuleContext();
                var world = module.world();
                var target = world.doorMarkedTarget(true);
                if (target == null || !world.approach(target)) return false;
                boolean result = world.attack(target, ModDamageTypes.OPENING_GHOST, module.effectiveIntensity());
                world.clearDoorMarks(target);
                return result;
            }

            if (!context.isModuleAvailable()
                    || !(context.event() instanceof DoorChangedEvent event) || !canExecute(context)) return false;
            // 通过媒介与裁决后才执行世界操作：先移动到触发门附近，再尝试灵异秒杀。
            teleportToDoorSide(context.getModuleContext().world(), event);
            LivingEntity source = event.source();
            return context.getModuleContext().world()
                    .attack(source, ModDamageTypes.OPENING_GHOST, context.getModuleContext().intensity());
        }
    }

    /**
     * 根据门朝向和触发者相对位置选择门的一侧，并通过世界接口执行瞬移。
     * 停止旧导航和调整朝向等物理细节由世界适配器处理。
     */
    private static void teleportToDoorSide(GhostWorldAccess world, DoorChangedEvent event) {
        // 使用门的朝向和触发者相对门中心的位置判断门的哪一侧，
        // 将厉鬼放到该侧门边；随后停止寻路与原有攻击目标，并转向触发者，
        // 避免瞬移完成后继续执行旧目标的移动或攻击行为。
        BlockState doorState = event.doorState();
        Direction facing = doorState.getValue(DoorBlock.FACING);
        double dx = event.source().getX() - (event.doorPos().getX() + 0.5D);
        double dz = event.source().getZ() - (event.doorPos().getZ() + 0.5D);
        int sideSign = dx * facing.getStepX() + dz * facing.getStepZ() >= 0.0D ? 1 : -1;
        world.teleport(event.doorPos().getX() + 0.5D + facing.getStepX() * sideSign * 0.8D,
                Math.max(event.doorPos().getY(), event.source().getY()),
                event.doorPos().getZ() + 0.5D + facing.getStepZ() * sideSign * 0.8D, event.source());

    }

    private static final GhostCorrosion CORROSION =
            GhostCorrosion.builder()
                    .add(SpiritualPart.GLOBAL, 10)
                    .add(SpiritualPart.HAND, 20)
                    .build();

    /**
     * 返回复苏值为零时可发挥的本质强度比例，供有效强度计算使用。
     */
    @Override
    public double minimumStrengthRatio() {
        return 1.0D / 3.0D;
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
                GhostTag.DOOR
        );
    }

    /**
     * 声明能够压制的目标标签；空集合表示没有额外的标签压制对象。
     */
    @Override
    public GhostClassification suppressionTargets() {
        return GhostClassification.empty();
    }

    /**
     * 返回可分配的压制单位数量，供驾驭压制配置使用。
     */
    @Override
    public int suppressionUnits() {
        return 1;
    }

    /**
     * 返回每个压制单位对应的基础灵异强度。
     */
    @Override
    public double suppressionUnitStrength() {
        return 20.0D;
    }

}
