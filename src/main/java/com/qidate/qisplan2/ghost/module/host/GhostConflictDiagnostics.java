package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleCombatState;
import com.qidate.qisplan2.ghost.module.host.attachment.GhostAttachments;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.sound.GhostSoundManager;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppression;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppressionLedger;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 宿主适配层的只读对抗诊断。查询实际关系，不执行规则、能力或历史裁决回放。 */
public final class GhostConflictDiagnostics {
    /** 每行对应一条来源到目标模块的关系；剩余 -1 表示由持续媒介维持。 */
    public record Relation(boolean outgoing, String kind, String sourceId, Component source,
            ResourceLocation sourceModule, Component target, ResourceLocation targetModule,
            GhostSuppression suppression, double targetStrength, boolean internal, long remainingTicks) {
        /** 展示当前单条关系的强度等级，不把其他来源的最终结果误算到该关系上。 */
        public GhostSuppression.Level level() { return suppression.level(targetStrength); }
    }
    private GhostConflictDiagnostics() {}

    /** 仅实际身体宿主有身体压制；手持物品不能冒充其携带者。 */
    public static LivingEntity body(GhostModuleHost host) {
        return host instanceof PlayerGhostModuleHost player ? player.player()
                : host instanceof LivingEntity living ? living : null;
    }
    /** 临时适配对象不具有稳定对象身份，分别按真实物品栈或身体匹配。 */
    public static boolean sameHost(GhostModuleHost first, GhostModuleHost second) {
        if (first == second) return true;
        if (first instanceof GhostItemModuleHost a && second instanceof GhostItemModuleHost c) return a.stack() == c.stack();
        var a = body(first); var c = body(second);
        return a != null && a == c;
    }
    /** 来源描述由适配器生成，始终保留语言组件；不把宿主信息交给模块。 */
    public static Component describe(GhostModuleHost host) {
        if (host instanceof GhostItemModuleHost item) return item.stack().getHoverName();
        var living = body(host);
        if (living != null) return Component.translatable("command.qisplan2.module.conflict.entity", living.getDisplayName(), living.getUUID().toString());
        if (host instanceof BlockEntity block) return Component.translatable("command.qisplan2.module.conflict.block",
                block.getBlockPos().toShortString(), block.getLevel() == null ? "?" : block.getLevel().dimension().location().toString());
        return Component.translatable("command.qisplan2.module.conflict.unknown");
    }
    /** 保留来源子系统、完整模块 ID 和实际宿主描述，不能把鬼域当成无主影响。 */
    public static Relation persistentRelation(UUID id,GhostSuppressionLedger.Relation relation,
            GhostModuleHost target,GhostModuleData.Entry entry){
        var source=relation.source();
        return new Relation(false,source==null?"persistent":source.kind(),id.toString(),
                source==null?Component.translatable("command.qisplan2.module.conflict.unknown"):source.description(),
                source==null?null:source.module(),describe(target),entry.id(),relation.suppression(),entry.intensity(),relation.internal(),-1);
    }
    /** 汇集所有已成立的入向/出向声音关系、外来附着关系和未到期短时袭击。 */
    public static List<Relation> snapshot(GhostModuleHost host) {
        var result = new ArrayList<>(GhostSoundManager.conflictRelations(host));
        var target = body(host);
        if (target != null) for (var source : GhostAttachments.suppressionSources(target)) {
            for (var entry : host.getModules()) result.add(new Relation(false, "attachment", source.instance().toString(),
                    source.item(), source.module(), describe(host), entry.id(), source.suppression(), entry.intensity(), false, -1));
        }
        if(target!=null)for(var source:GhostPersistentSuppression.relations(target).entrySet()){
            for(var entry:host.getModules())result.add(persistentRelation(source.getKey(),source.getValue(),host,entry));
        }
        for (var entry : host.getModules()) {
            var data = entry.customData().getCompound(GhostModuleCombatState.KEY);
            if (!data.contains("attackUntil")) continue;
            long remaining = data.getLong("attackUntil") - host.moduleWorld(entry.id()).gameTime();
            double strength = data.getDouble("attackStrength");
            if (remaining <= 0 || !Double.isFinite(strength) || strength <= 0) continue;
            result.add(new Relation(false, "attack", "-", Component.translatable("command.qisplan2.module.conflict.unknown"),
                    null, describe(host), entry.id(), new GhostSuppression(GhostSuppression.Kind.NORMAL, strength), entry.intensity(), false, remaining));
        }
        result.sort(Comparator.comparing(Relation::outgoing).thenComparing(Relation::kind)
                .thenComparing(Relation::sourceId).thenComparing(r -> r.target().getString()).thenComparing(r -> r.targetModule().toString()));
        return List.copyOf(result);
    }
}
