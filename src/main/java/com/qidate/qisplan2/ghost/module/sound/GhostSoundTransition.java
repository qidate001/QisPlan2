package com.qidate.qisplan2.ghost.module.sound;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** 传播快照的纯差异计算；不区分玩家/生物，也不执行攻击或客户端播放。 */
public record GhostSoundTransition<K>(Set<K> entered, Set<K> left, Set<ResourceLocation> interrupted) {
    /** 同类别最后一个声源消失才中断，来源替换或强度变化不会伪造中断。 */
    public static <K> GhostSoundTransition<K> between(Map<K, ResourceLocation> previous, Map<K, ResourceLocation> current) {
        var entered = new HashSet<>(current.keySet()); entered.removeAll(previous.keySet());
        var left = new HashSet<>(previous.keySet()); left.removeAll(current.keySet());
        var interrupted = new HashSet<>(previous.values()); interrupted.removeAll(current.values());
        return new GhostSoundTransition<>(Set.copyOf(entered), Set.copyOf(left), Set.copyOf(interrupted));
    }
}
