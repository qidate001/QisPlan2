
package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostData;
import com.qidate.qisplan2.ghost.module.implementation.candle.GhostCandleModule;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** 无需启动游戏世界的回归检查：死机规则、主动能力、媒介和延迟调用。 */
public final class PossessionInvocationTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("qisplan2_test", "invocation");
    private static boolean medium = true;
    private static int effects;
    private static int rules;
    private static final class Host implements GhostModuleHost {
        List<GhostModuleData.Entry> entries;

        /**
         * 读取当前载体保存的模块实例列表。
         */
        public List<GhostModuleData.Entry> getModules() { return entries; }
    }

    /**
     * 校验回归条件；不满足时抛出断言错误并给出失败原因。
     */
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    /**
     * 运行回归用例，覆盖死机、主动能力、媒介限制以及延迟任务失效等边界。
     */
    public static void main(String[] args) {
        HostNeutralCandleTest.run();
        GhostCarpetContactTest.run();
        GhostGrassContactTest.run();
        BlockSpreadTest.run();
        GhostShroudTest.run();
        GhostModuleRegistry.register(new GhostModule() {
            /**
             * 获取当前定义的唯一标识。
             * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
             */
            public ResourceLocation getId() { return ID; }

            /**
             * 返回本定义对外提供的能力列表，供统一执行器按标识查找。
             */
            public List<GhostAbility> getAbilities() { return List.of(new GhostAbility() {
                /**
                 * 获取当前定义的唯一标识。
                 * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
                 */
                public ResourceLocation getId() { return ID; }

                /**
                 * 检查本次能力调用的事件、参数或环境是否可用于执行。
                 * 模块可用性与媒介由统一执行器共同校验。
                 */
                public boolean canExecute(GhostAbilityContext c) { return true; }

                /**
                 * 执行当前能力的效果。
                 * 调用方应使用统一执行器，以确保模块状态和媒介已经检查。
                 */
                public void execute(GhostAbilityContext c) { effects++; }
            }); }

            /**
             * 声明能力执行前必须满足的媒介。
             * 主动调用同样需要通过媒介；规则是否触发由规则层单独判断。
             */
            public List<GhostMedium> getMedia() { return List.of(new GhostMedium() {
                /**
                 * 获取当前定义的唯一标识。
                 * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
                 */
                public ResourceLocation getId() { return ID; }

                /**
                 * 校验本媒介要求的环境、事件或输入条件。
                 * 返回 false 会阻止本次能力执行，主动使用也不能绕过该检查。
                 */
                public boolean allows(GhostAbilityContext c) { return medium; }
            }); }

            /**
             * 声明本厉鬼响应的语义事件及对应规则。
             * 规则负责决定何时发起能力或提交影响，不绕过执行器直接完成袭击。
             */
            public List<GhostRule> getRules() { return List.of(new GhostRule() {
                /**
                 * 获取当前定义的唯一标识。
                 * 注册、能力查找和规则分发使用此标识，不代表某个运行时实例。
                 */
                public ResourceLocation getId() { return ID; }

                /**
                 * 判断当前语义事件是否属于本规则处理的范围。
                 * 这里只筛选事件种类或语义，不执行能力和世界操作。
                 */
                public boolean supports(GhostEvent e) { return true; }

                /**
                 * 检查当前事实和模块上下文是否满足规则触发条件。
                 * 返回 false 时跳过本规则，不提交能力或灵异影响。
                 */
                public boolean canTrigger(GhostRuleContext c, GhostEvent e) { return true; }

                /**
                 * 处理已满足条件的语义事件，发起对应能力或提交灵异影响。
                 * 需要裁决的效果仅在获准后执行。
                 */
                public void onTrigger(GhostRuleContext c, GhostEvent e) { rules++; }
            }); }
        });
        Host host = new Host();
        CompoundTag timed = new CompoundTag();
        CompoundTag combat = new CompoundTag();
        combat.putInt("stunTicks", 200);
        timed.put("qisplan2:combat", combat);
        CompoundTag permanent = timed.copy();
        permanent.getCompound("qisplan2:combat").putBoolean("permanentStun", true);
        for (var entry : List.of(
                new GhostModuleData.Entry(ID, 10, 0, false, true, new CompoundTag()),
                new GhostModuleData.Entry(ID, 10, 0, false, false, timed),
                new GhostModuleData.Entry(ID, 10, 0, false, false, permanent))) {
            host.entries = List.of(entry);
            int before = effects;
            var active = new GhostAbilityContext(host, ID, ID, GhostAbilityContext.Source.POSSESSION, null);
            check(GhostAbilityExecutor.tryExecuteWithResult(active), "死机不应阻止主动能力");
            check(effects == before + 1, "主动能力必须执行一次");
            var automatic = new GhostAbilityContext(host, ID, ID, GhostAbilityContext.Source.RULE, null);
            check(!GhostAbilityExecutor.tryExecute(automatic), "死机必须阻止规则能力");
            GhostModuleRuntime.dispatch(host, new ModuleTickEvent());
            check(rules == 0, "死机必须阻止规则触发");
            medium = false;
            check(!GhostAbilityExecutor.tryExecute(active), "主动调用不能绕过媒介");
            medium = true;
            Runnable deferred = GhostAbilityExecutor.prepareDeferred(active);
            host.entries = List.of();
            deferred.run();
            check(effects == before + 1, "卸载模块必须取消延迟能力");
        }
        host.entries = List.of(new GhostModuleData.Entry(ID, 10));
        GhostModuleRuntime.dispatch(host, new ModuleTickEvent());
        check(rules == 1, "正常模块应触发规则");
        var deadlocked = new GhostModuleData.Entry(ID, 10, 0, false, true, new CompoundTag());
        check(PlayerGhostData.view(deadlocked).state().isAnyStun(), "界面必须显示模块死机");
        CompoundTag lit = new CompoundTag();
        lit.putBoolean("lit", true);
        check(GhostCandleModule.isLit(new GhostModuleData.Entry(ID, 5, 0, false, false, lit)),
                "点燃且可用的鬼烛必须有效");
        check(!GhostCandleModule.isLit(new GhostModuleData.Entry(ID, 5)),
                "未点燃鬼烛不能提供效果");
        check(!GhostCandleModule.isLit(new GhostModuleData.Entry(ID, 5, 0, false, true, lit)),
                "死机鬼烛不能提供效果");
        check(!GhostCandleModule.isLit(new GhostModuleData.Entry(ID, 5, 0, true, false, lit)),
                "沉睡鬼烛不能提供效果");
        lit.put("qisplan2:combat", combat);
        check(!GhostCandleModule.isLit(new GhostModuleData.Entry(ID, 5, 0, false, false, lit)),
                "限时死机鬼烛不能提供效果");
        check(GhostCandleModule.soundBurnPerTick(0) == 0, "没有声音压力时不额外燃烧");
        check(GhostCandleModule.soundBurnPerTick(10) * 20 == 1, "10强度应额外消耗1耐久每秒");
        check(GhostCandleModule.soundBurnPerTick(50) * 20 == 5, "声音压力应随强度线性增加");
        check(GhostCandleModule.soundBurnPerTick(-1) == 0, "无效负强度不能返还耐久");
        System.out.println("Possession and candle state regression checks passed");
    }
}
