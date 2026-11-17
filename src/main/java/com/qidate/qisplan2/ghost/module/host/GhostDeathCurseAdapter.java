package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.environment.GhostDeathCurseAccess;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentSync;
import java.util.concurrent.ThreadLocalRandom;

/** 旧共享诅咒计数与不同载体的实现边界；保留 HUD 和已有目标层数。 */
public final class GhostDeathCurseAdapter implements GhostDeathCurseAccess {
    private final GhostModuleHost host;
    private final ResourceLocation module;
    /** 绑定当前模块实例。 */
    public GhostDeathCurseAdapter(GhostModuleHost host,ResourceLocation module) { this.host=host; this.module=module; }
    /** 校验近距离施咒目标，禁止跨世界、收起物品后远程施咒。 */
    public Subject target(LivingEntity target) {
        var world=host.moduleWorld(module);
        return world.validTarget(target) && target.getBoundingBox().distanceToSqr(world.position())<=9 ? wrap(target) : null;
    }
    /** 反噬作用于当前生命载体或物品携带者，但不把该实体返回给模块。 */
    public Subject backlash() { return wrap(GhostEnvironmentAdapters.bearer(host)); }
    /** 抽样与概率分离，模块拥有反噬概率定义。 */
    public double random() { return ThreadLocalRandom.current().nextDouble(); }
    /** 将生命对象转换为不透明计数与袭击出口。 */
    private Subject wrap(LivingEntity target) {
        if (target==null || !target.isAlive() || !(target.level() instanceof ServerLevel)) return null;
        return new Subject() {
            /** 使用旧附件保证多把剑共享层数，也保留旧 HUD。 */
            public int stacks() { return target.getData(ModAttachments.DEATH_CURSE_COUNT.get()); }
            /** 清零时移除附件，所有改变都同步客户端。 */
            public void stacks(int count) {
                if (count<=0) target.removeData(ModAttachments.DEATH_CURSE_COUNT.get());
                else target.setData(ModAttachments.DEATH_CURSE_COUNT.get(),count);
                AttachmentSync.syncEntityUpdate(target,ModAttachments.DEATH_CURSE_COUNT.get());
            }
            /** 十层袭击参加统一灵异裁决，保护和死机不再被旧处理器绕开。 */
            public void strike(GhostAbilityContext context,double intensity,boolean backlash) {
                var actor=GhostEnvironmentAdapters.bearer(host);
                if (actor instanceof Player player) player.sendSystemMessage(Component.translatable(backlash
                        ? "qisplan2.death_curse.reflection" : host instanceof GhostItemModuleHost
                        ? "qisplan2.death_curse.triggered" : "qisplan2.death_curse.activated"));
                var influence=GhostAbilityInfluence.of(target,GhostInfluenceType.ATTACK,intensity);
                if (influence!=null) influence.execute(context,()->host.moduleWorld(module).attack(target,ModDamageTypes.DEATH_CURSE,intensity));
            }
        };
    }
    /** 物品形态沿用破碎表现；其他载体没有要破碎的物品，不额外删除厉鬼模块。 */
    public void exhaust() {
        if (host instanceof GhostItemModuleHost item) {
            var carrier=item.carrier(); var stack=item.stack();
            if (carrier!=null && carrier.level() instanceof ServerLevel level && !stack.isEmpty()) {
                level.playSound(null,carrier.blockPosition(),SoundEvents.ITEM_BREAK,SoundSource.PLAYERS,1,1);
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,stack.copy()),carrier.getX(),carrier.getY()+1,carrier.getZ(),10,.2,.3,.2,.1);
            }
            stack.shrink(1);
        }
    }
}
