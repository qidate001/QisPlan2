package com.qidate.qisplan2.ghost.module.implementation.candle;

import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.GhostUseEvent;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.item.candle.GhostCandleSystem;
import com.qidate.qisplan2.ghost.module.sound.GhostSoundManager;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/** 鬼烛的共用机制；红、白鬼烛分别注册为完整模块。点燃状态属于模块 customData。 */
public abstract class GhostCandleModule implements GhostModule {
    private final ResourceLocation id;
    protected GhostCandleModule(String path) { id = ResourceLocation.fromNamespaceAndPath("qisplan2", path); }
    @Override public ResourceLocation getId() { return id; }
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(5, 0); }
    private ResourceLocation toggleId() { return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "/toggle"); }

    public static boolean isLit(ItemStack stack, ResourceLocation id) {
        if (stack.isEmpty()) return false;
        return new GhostItemModuleHost(stack).getModules().stream().anyMatch(e -> e.id().equals(id)
                && isLit(e));
    }

    /** 点燃状态与模块运行状态共同决定鬼烛能否提供效果。 */
    public static boolean isLit(GhostModuleData.Entry entry) {
        return GhostModuleCombatState.available(entry) && entry.customData().getBoolean("lit");
    }

    @Override public List<GhostRule> getRules() {
        return List.of(new GhostRule() {
            public ResourceLocation getId() { return toggleId(); }
            public boolean supports(GhostEvent e) { return e instanceof GhostUseEvent; }
            public boolean canTrigger(GhostRuleContext c, GhostEvent e) { return c.host() instanceof GhostItemModuleHost; }
            public void onTrigger(GhostRuleContext c, GhostEvent e) { c.tryExecuteAbility(toggleId(), e); }
        });
    }
    @Override public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            public ResourceLocation getId() { return toggleId(); }
            public boolean allows(GhostAbilityContext c) {
                return c.host() instanceof GhostItemModuleHost host && !host.stack().isEmpty()
                        && host.stack().isDamageableItem();
            }
        });
    }
    @Override public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(new GhostAbility() {
            public ResourceLocation getId() { return toggleId(); }
            public boolean canExecute(GhostAbilityContext c) { return c.host() instanceof GhostItemModuleHost; }
            public void execute(GhostAbilityContext c) {
                var host = (GhostItemModuleHost)c.host();
                var data = host.getModuleCustomData(id);
                boolean lit = data.contains("lit") ? data.getBoolean("lit")
                        : host.stack().has(DataComponents.CUSTOM_MODEL_DATA);
                data.putBoolean("lit", !lit);
                host.setModuleCustomData(id, data);
                syncModel(host.stack());
            }
        });
    }
    /** 模型只是显示结果，不作为是否点燃的权威数据。 */
    private static void syncModel(ItemStack stack) {
        if (isLit(stack, WhiteGhostCandleModule.ID) || isLit(stack, RedGhostCandleModule.ID))
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        else stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }
    /** 每 10 点被屏蔽的灵异强度，额外消耗 1 点耐久/秒。 */
    public static double soundBurnPerTick(double intensity) {
        return Double.isFinite(intensity) ? Math.max(0, intensity) / 200.0D : 0;
    }

    @Override public void onItemTick(GhostItemModuleHost host, Player player) {
        ItemStack stack = host.stack();
        var data = host.getModuleCustomData(id);
        if (!data.contains("lit")) {
            // 旧物品以模型组件保存点燃状态；首次读取后迁移到模块。
            data.putBoolean("lit", stack.has(DataComponents.CUSTOM_MODEL_DATA));
            host.setModuleCustomData(id, data);
        }
        syncModel(stack);
        if (!isLit(stack, id)) return;
        double burn = GhostCandleSystem.getBurnDamagePerTick(player);
        if (id.equals(WhiteGhostCandleModule.ID) && player instanceof ServerPlayer serverPlayer
                && (player.getMainHandItem() == stack || player.getOffhandItem() == stack)) {
            // 声音屏蔽是额外负担，与原有附近厉鬼燃烧压力相加。
            // 多个声源只取最强者；背包中的鬼烛不提供屏蔽，也不承担声音消耗。
            burn += soundBurnPerTick(GhostSoundManager.getBlockedSoundIntensity(serverPlayer));
        }
        int damage = GhostCandleSystem.rollBurnDamage(player, burn);
        if (damage <= 0) return;
        // 必须加上当前损伤值，否则鬼烛永远无法正常燃尽。
        if ((long)stack.getDamageValue() + damage >= stack.getMaxDamage()) stack.shrink(1);
        else stack.setDamageValue(stack.getDamageValue() + damage);
    }
}
