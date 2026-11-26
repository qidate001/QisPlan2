package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.host.GhostConflictDiagnostics;
import net.minecraft.network.chat.Component;
import com.qidate.qisplan2.ghost.module.sound.GhostSoundTransition;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppression;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppressionLedger;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppressionState;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/** 验证稳定关系缓存、来源诊断及声音中断聚合，不依赖具体宿主类。 */
public final class SuppressionLedgerTest {
    private static final ResourceLocation SOURCE = ResourceLocation.parse("qisplan2_test:sound_source");
    private static final ResourceLocation TARGET = ResourceLocation.parse("qisplan2_test:target");
    /** 断言失败使构建失败。 */
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    /** 使用正式关系账本验证多来源合并、撤销、缓存和来源信息。 */
    public static void run() {
        var first = UUID.randomUUID(); var second = UUID.randomUUID();
        var ledger = new GhostSuppressionLedger();
        var relation = new GhostSuppressionLedger.Relation(new GhostSuppression(GhostSuppression.Kind.NORMAL, 15), false, null);
        ledger.put(first, relation);
        check(ledger.state(TARGET, 15).external() == GhostSuppression.Level.NONE, "等强不压制");
        check(ledger.state(TARGET, 14).external() == GhostSuppression.Level.BASIC, "15对14基础压制");
        check(ledger.state(TARGET, 10).external() == GhostSuppression.Level.COMPLETE, "15对10恰好进入完全压制");
        check(ledger.state(TARGET, 9).external() == GhostSuppression.Level.COMPLETE, "15对9完全压制");
        var full=ledger.state(TARGET,1);
        check(full.external()==GhostSuppression.Level.COMPLETE&&!full.canAct()&&!full.canRunRules()&&!full.canUseAbility(true),
                "15强度外来声音对1强度模块：无抵挡时三项权限必须全部禁止");
        for(var owner:List.of(ResourceLocation.parse("qisplan2:ghost_eye"),ResourceLocation.parse("qisplan2:ghost_mist"))){
            var provenance=new GhostSuppressionLedger.Source("domain",owner,Component.literal("实体来源"));
            var declared=new GhostSuppressionLedger.Relation(new GhostSuppression(GhostSuppression.Kind.NORMAL, 15),false,null,provenance);
            var domainId=UUID.randomUUID();ledger.put(domainId,declared);
            var entry=new GhostModuleData.Entry(TARGET,1);
            var diagnostic=GhostConflictDiagnostics.persistentRelation(domainId,ledger.snapshot().get(domainId),()->List.of(entry),entry);
            check(diagnostic.sourceModule().equals(owner)&&diagnostic.kind().equals("domain")&&diagnostic.sourceId().equals(domainId.toString()),
                    "鬼域实例与所属模块必须同时保留，不能显示未知来源");
            check(diagnostic.source().getString().equals("实体来源"),"鬼域来源描述不能丢失");
            ledger.remove(domainId);
        }
        long version = ledger.revision(); var state = ledger.state(TARGET, 9);
        for (int i = 0; i < 1000; i++) ledger.put(first, relation);
        check(ledger.revision() == version && ledger.state(TARGET, 9) == state, "稳定听觉关系不得反复重算状态");
        ledger.put(second, new GhostSuppressionLedger.Relation(new GhostSuppression(GhostSuppression.Kind.NORMAL, 30), false, null));
        check(ledger.state(TARGET, 20).external() == GhostSuppression.Level.COMPLETE, "多来源取最强且不简单相加");
        ledger.remove(second);
        check(ledger.state(TARGET, 20).external() == GhostSuppression.Level.NONE && !ledger.isEmpty(), "停一台仍保留另一台关系");
        ledger.put(first, new GhostSuppressionLedger.Relation(new GhostSuppression(GhostSuppression.Kind.NORMAL, 60), false, null));
        check(ledger.state(TARGET, 20).external() == GhostSuppression.Level.COMPLETE, "仍在范围内也必须更新改变的强度");
        check(ledger.state(TARGET, 100).external() == GhostSuppression.Level.NONE, "目标变强必须重算");
        var snapshot = ledger.snapshot();
        long beforeRead = ledger.revision();
        check(snapshot.get(first).suppression().strength() == 60 && ledger.revision() == beforeRead, "诊断读取实际关系且不改变版本");
        try { snapshot.clear(); throw new AssertionError("诊断快照必须只读"); }
        catch (UnsupportedOperationException expected) {}
        ledger.remove(first); check(snapshot.size() == 1, "旧快照不能被后续关系撤销修改");
        check(ledger.isEmpty(), "离开/隔音/停止撤销最后关系");
        ledger.put(first, new GhostSuppressionLedger.Relation(new GhostSuppression(GhostSuppression.Kind.NORMAL, 60), true, SOURCE));
        check(ledger.state(SOURCE, 1).canAct(), "不能压制发声模块自身");
        check(ledger.state(TARGET, 1).canUseAbility(true) && !ledger.state(TARGET, 1).canRunRules(), "体内压制保留主动调用豁免");
        check(!ledger.state(TARGET, 1).merge(GhostSuppressionState.EXTERNAL_COMPLETE).canUseAbility(true), "音乐结束或体内豁免不能解除棺材钉");

        var before = Map.of(first, SOURCE, second, SOURCE);
        var oneLeft = GhostSoundTransition.between(before, Map.of(second, SOURCE));
        check(oneLeft.left().size() == 1 && oneLeft.interrupted().isEmpty(), "仍听到同类音乐不能触发中断袭击");
        var silent = GhostSoundTransition.between(before, Map.<UUID,ResourceLocation>of());
        check(silent.left().size() == 2 && silent.interrupted().size() == 1, "两台同时消失只产生一次同类中断");
        var stable = GhostSoundTransition.between(before, before);
        check(stable.entered().isEmpty() && stable.left().isEmpty() && stable.interrupted().isEmpty(), "持续听见不得重复派发");
        var blockedAtStart = GhostSoundTransition.between(Map.<UUID,ResourceLocation>of(), Map.<UUID,ResourceLocation>of());
        check(blockedAtStart.interrupted().isEmpty(), "一开始就被白烛屏蔽或没有听力不能凭空中断");
        var resumed = GhostSoundTransition.between(Map.<UUID,ResourceLocation>of(), Map.of(first, SOURCE));
        check(resumed.entered().size() == 1 && resumed.interrupted().isEmpty(), "重新听见只建立关系");
        System.out.println("Suppression ledger, source lifecycle and hearing transitions passed");
    }
}
