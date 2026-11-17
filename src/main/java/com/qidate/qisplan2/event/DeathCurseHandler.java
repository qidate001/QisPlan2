package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.event.MeleeHitEvent;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.implementation.DeathCurseSwordModule;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.ArrayList;
import java.util.List;

/** 近战命中适配桥。只处理物理输入与旧普通伤害表现，诅咒算法完全交给模块。 */
@EventBusSubscriber(modid=QisPlan2.MODID)
public final class DeathCurseHandler {
    /** 事件处理器无需实例化。 */
    private DeathCurseHandler() {}
    /** 过滤投射物、环境伤害及灵异伤害，防止十层袭击递归生成新的近战命中。 */
    @SubscribeEvent public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide() || !event.getEntity().isAlive()) return;
        var source=event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity()!=attacker) return;
        if (!source.is(DamageTypes.PLAYER_ATTACK) && !source.is(DamageTypes.MOB_ATTACK)
                && !source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)) return;
        List<GhostModuleHost> hosts=new ArrayList<>();
        if (attacker instanceof Player player) hosts.add(new PlayerGhostModuleHost(player));
        else if (attacker instanceof GhostModuleHost modules) hosts.add(modules);
        var stack=attacker.getMainHandItem();
        if (!stack.isEmpty()) hosts.add(new GhostItemModuleHost(stack,attacker));
        boolean active=hosts.stream().anyMatch(host->host.carriesModule(DeathCurseSwordModule.ID)
                && host.getModules().stream().anyMatch(entry->entry.id().equals(DeathCurseSwordModule.ID)
                && GhostModuleCombatState.available(entry)));
        if (active) event.setNewDamage(event.getEntity() instanceof SupernaturalEntity ? 0 : 1);
        GhostModuleRuntime.dispatch(hosts,new MeleeHitEvent(event.getEntity()));
    }
    /** 保留旧规则：死亡重生后不继承共享诅咒层数。 */
    @SubscribeEvent public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getEntity().removeData(ModAttachments.DEATH_CURSE_COUNT.get());
    }
}
