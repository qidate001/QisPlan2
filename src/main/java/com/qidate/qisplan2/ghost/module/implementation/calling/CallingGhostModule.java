package com.qidate.qisplan2.ghost.module.implementation.calling;
import com.qidate.qisplan2.ghost.module.environment.GhostBehavior;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 完整喊人鬼模块。
 * 行为选择并跟随目标；媒介校验目标与灵异阻隔；转身事实触发规则并提交裁决；
 * 跟随喊名能力与袭击能力只在统一执行器批准后操作世界。
 */
public final class CallingGhostModule implements GhostModule {
    /**
     * 声明需要的自主行动类型。
     * 具体 Goal 的创建、资源竞争和停止清理由行动适配层负责。
     */
    @Override public GhostBehavior behavior() { return GhostBehavior.FOLLOW_CALL; }
    public static final ResourceLocation ID = id("calling_ghost");

    // 原实现固定使用 30 强度袭击，迁移后作为模块初始值，后续随实例 intensity 变化。
    public static final double INITIAL_INTENSITY = 30.0D;
    public static final ResourceLocation FOLLOW = id("calling_ghost/follow_call");
    public static final ResourceLocation ATTACK = id("calling_ghost/attack");

    /**
     * 构造或返回本定义使用的资源标识，保持注册与调用入口一致。
     */
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MODID, path); }

    /**
     * 获取当前定义的唯一标识。
     * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
     */
    @Override public ResourceLocation getId() { return ID; }

    /**
     * 提供新建模块实例的初始强度和复苏值。
     * 仅用于初始化；已存在实例以保存的模块数据为准。
     */
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(INITIAL_INTENSITY, 0); }

    /**
     * 计算归一化后的水平转角变化，达到 45 度即视为满足转身语义。
     */
    public static boolean isTurnAround(float previous, float current) {
        return Math.abs(Mth.wrapDegrees(current - previous)) >= 45.0F;
    }

    /**
     * 声明能力执行前必须满足的媒介。
     * 主动调用同样需要通过媒介；规则是否触发由规则层单独判断。
     */
    @Override public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            @Override public ResourceLocation getId() { return id("calling_ghost/target_medium"); }

            /**
             * 校验本媒介要求的环境、事件或输入条件。
             * 返回 false 会阻止本次能力执行，主动使用也不能绕过该检查。
             */
            @Override public boolean allows(GhostAbilityContext c) {
                var world = c.getModuleContext().world();
                if (!world.present()) return false;
                var player = c.event() == null && c.source() == GhostAbilityContext.Source.POSSESSION ? world.aimedTarget(32) : world.target();
                if (player == null || !world.canTrack(player)) return false;
                if (c.abilityId().equals(FOLLOW)) return c.source() == GhostAbilityContext.Source.POSSESSION || c.getModuleContext().getCustomBoolean("physicsOwned", false);
                if (c.event() == null) return world.validTarget(player);
                return c.event() instanceof EntityTurnedEvent e && world.isOrigin(e.observer())
                        && e.entity() == player && isTurnAround(e.previousYaw(), e.currentYaw())
                        && world.validTarget(player);
            }
        });
    }

    /**
     * 声明本厉鬼响应的语义事件及对应规则。
     * 规则负责决定何时发起能力或提交影响，不绕过执行器直接完成袭击。
     */
    @Override public List<GhostRule> getRules() {
        return List.of(new GhostRule() {
            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            @Override public ResourceLocation getId() { return id("calling_ghost/turn_rule"); }

            /**
             * 判断当前语义事件是否属于本规则处理的范围。
             * 这里只筛选事件种类或语义，不执行能力和世界操作。
             */
            @Override public boolean supports(GhostEvent e) {
                return e instanceof EntityTurnedEvent || e instanceof ModuleTickEvent;
            }

            /**
             * 检查当前事实和模块上下文是否满足规则触发条件。
             * 返回 false 时跳过本规则，不提交能力或灵异影响。
             */
            @Override public boolean canTrigger(GhostRuleContext c, GhostEvent e) {
                return c.getModuleContext().world().present();
            }

            /**
             * 处理已满足条件的语义事件，发起对应能力或提交灵异影响。
             * 需要裁决的效果仅在获准后执行。
             */
            @Override public void onTrigger(GhostRuleContext c, GhostEvent e) {
                if (e instanceof ModuleTickEvent) {
                    var data = c.getModuleContext().getCustomData();
                    int remaining = Math.max(0, data.getInt("attackTargetCooldown"));
                    data.putInt("attackTargetCooldown", Math.max(0, remaining - 1));
                    if (remaining <= 1) data.remove("recentlyAttacked");
                    c.getModuleContext().setCustomData(data);
                } else if (e instanceof EntityTurnedEvent turn && isTurnAround(turn.previousYaw(), turn.currentYaw())) {
                    c.submitAbilityInfluence(ATTACK, turn, turn.entity().getUUID(),
                            GhostInfluenceType.ATTACK, c.getModuleContext().intensity());
                }
            }
        });
    }

    /**
     * 返回本厉鬼自身的能力定义。
     * 世界效果由这些能力执行，输入桥和行为适配器只负责发起调用。
     */
    @Override public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(new GhostAbility() {
            /** 技能图标由所属模块直接声明。 */
            @Override public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/name_tag.png"); }

            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            @Override public ResourceLocation getId() { return FOLLOW; }

            /**
             * 检查本次能力调用的事件、参数或环境是否可用于执行。
             * 模块可用性与媒介由统一执行器共同校验。
             */
            @Override public boolean canExecute(GhostAbilityContext c) { return c.getModuleContext().world().canMove(); }

            /**
             * 执行当前能力的效果。
             * 调用方应使用统一执行器，以确保模块状态和媒介已经检查。
             */
            @Override public void execute(GhostAbilityContext c) {
                var world = c.getModuleContext().world();
                var player = c.event() == null && c.source() == GhostAbilityContext.Source.POSSESSION ? world.aimedTarget(32) : world.target();
                if (player == null) return;
                Vec3 look = player.getLookAngle();
                Vec3 destination = player.position().subtract(look.x * 2.5D, 0, look.z * 2.5D).add(0, 1, 0);
                Vec3 movement = destination.subtract(world.position());
                double distance = movement.length();
                world.move(distance < 0.1D ? Vec3.ZERO
                        : movement.normalize().scale(Math.min(distance * 0.5D, 0.35D)), player.getYRot());

                var data = c.getModuleContext().getCustomData();
                int cooldown = Math.max(0, data.getInt("callCooldown"));
                if (cooldown > 0) {
                    data.putInt("callCooldown", cooldown - 1);
                    c.getModuleContext().setCustomData(data);
                    return;
                }
                if (data.getInt("callCount") >= 7) {
                    data.remove("target");
                    data.putBoolean("hasLastPlayerRotation", false);
                    c.getModuleContext().setCustomData(data);
                    world.move(Vec3.ZERO, player.getYRot());
                    return;
                }
                boolean hasSound = world.hasCall(player);
                data.putInt("callCooldown", 100);
                if (hasSound) {
                    data.putInt("callCount", Math.max(0, data.getInt("callCount")) + 1);
                    world.playCall(player);
                }
                c.getModuleContext().setCustomData(data);
            }
        }, new GhostAbility() {
            /** 技能图标由所属模块直接声明。 */
            @Override public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/name_tag.png"); }

            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            @Override public ResourceLocation getId() { return ATTACK; }

            /**
             * 检查本次能力调用的事件、参数或环境是否可用于执行。
             * 模块可用性与媒介由统一执行器共同校验。
             */
            @Override public boolean canExecute(GhostAbilityContext c) {
                return c.event() instanceof EntityTurnedEvent e && e.entity() instanceof ServerPlayer
                        || c.event() == null && c.getModuleContext().world().validTarget(c.getModuleContext().world().aimedTarget(32));
            }

            /**
             * 执行当前能力的效果。
             * 调用方应使用统一执行器，以确保模块状态和媒介已经检查。
             */
            @Override public void execute(GhostAbilityContext c) { executeWithResult(c); }

            /**
             * 执行能力并返回实际效果是否成功。
             * 返回值供主动交互及成功后结算使用，不能等同于仅接受了调用。
             */
            @Override public boolean executeWithResult(GhostAbilityContext c) {
                var target = c.event() instanceof EntityTurnedEvent event ? event.entity() : c.getModuleContext().world().aimedTarget(32);
                return c.getModuleContext().world().attack(target,
                        ModDamageTypes.CALLING_GHOST, c.getModuleContext().intensity());
            }
        });
    }
}
