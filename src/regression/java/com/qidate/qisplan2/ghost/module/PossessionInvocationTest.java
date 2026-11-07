
package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostData;
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
        public List<GhostModuleData.Entry> getModules() { return entries; }
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        GhostModuleRegistry.register(new GhostModule() {
            public ResourceLocation getId() { return ID; }
            public List<GhostAbility> getAbilities() { return List.of(new GhostAbility() {
                public ResourceLocation getId() { return ID; }
                public boolean canExecute(GhostAbilityContext c) { return true; }
                public void execute(GhostAbilityContext c) { effects++; }
            }); }
            public List<GhostMedium> getMedia() { return List.of(new GhostMedium() {
                public ResourceLocation getId() { return ID; }
                public boolean allows(GhostAbilityContext c) { return medium; }
            }); }
            public List<GhostRule> getRules() { return List.of(new GhostRule() {
                public ResourceLocation getId() { return ID; }
                public boolean supports(GhostEvent e) { return true; }
                public boolean canTrigger(GhostRuleContext c, GhostEvent e) { return true; }
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
        System.out.println("Possession invocation regression checks passed");
    }
}
