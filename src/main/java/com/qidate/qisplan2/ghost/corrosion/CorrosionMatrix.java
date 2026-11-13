package com.qidate.qisplan2.ghost.corrosion;

import com.qidate.qisplan2.ghost.bearing.HostComposition;

import com.qidate.qisplan2.ghost.bearing.SpiritualPart;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

public class CorrosionMatrix {

    private final Map<SpiritualPart, Map<ResourceLocation, Map<SpiritualPart, Integer>>>
            matrix = new LinkedHashMap<>();


    /** 玩家界面兼容入口；其他宿主应传入自身组成。 */
    public CorrosionMatrix() { this(HostComposition.HUMANOID); }

    /** 预置实际组成和全局统计项，动态部位仍可按需加入贡献。 */
    public CorrosionMatrix(HostComposition composition) {
        matrix.put(SpiritualPart.GLOBAL, new LinkedHashMap<>());
        composition.capacities().keySet().forEach(part -> matrix.put(part, new LinkedHashMap<>()));
    }

    /*
     * ============================================================
     * 增加贡献
     * ============================================================
     */

    /**
     * target：最终作用到哪个部位。
     *
     * source：侵蚀原本来自哪个类型。
     *
     * 例如：
     *
     * HAND <- GLOBAL +20
     * HAND <- HAND +20
     */
    public void add(
            SpiritualPart target,
            ResourceLocation ghost,
            SpiritualPart source,
            int amount
    ) {

        matrix.computeIfAbsent(target, ignored -> new HashMap<>())
                .computeIfAbsent(
                        ghost,
                        g -> new LinkedHashMap<>()
                )
                .merge(
                        source,
                        amount,
                        Integer::sum
                );
    }


    /*
     * ============================================================
     * 查询
     * ============================================================
     */

    /**
     * 获取某部位总侵蚀。
     */
    public int total(
            SpiritualPart target
    ) {

        return matrix.getOrDefault(target, Map.of())
                .values()
                .stream()
                .flatMap(m -> m.values().stream())
                .mapToInt(Integer::intValue)
                .sum();
    }


    /**
     * 获取某只鬼在某部位总贡献。
     */
    public int contribution(
            SpiritualPart target,
            ResourceLocation ghost
    ) {

        Map<SpiritualPart, Integer> map =
                matrix.getOrDefault(target, Map.of()).get(ghost);

        if (map == null) {
            return 0;
        }

        return map.values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();
    }


    /**
     * 获取某只鬼在某部位、某来源的贡献。
     */
    public int contribution(
            SpiritualPart target,
            ResourceLocation ghost,
            SpiritualPart source
    ) {

        Map<SpiritualPart, Integer> map =
                matrix.getOrDefault(target, Map.of()).get(ghost);

        if (map == null) {
            return 0;
        }

        return map.getOrDefault(
                source,
                0
        );
    }


    /**
     * 获取整个矩阵（只读）。
     */
    public Map<SpiritualPart, Map<ResourceLocation, Map<SpiritualPart, Integer>>> matrix() {

        var copy = new LinkedHashMap<SpiritualPart, Map<ResourceLocation, Map<SpiritualPart, Integer>>>();
        matrix.forEach((part, contributions) -> {
            var byGhost = new LinkedHashMap<ResourceLocation, Map<SpiritualPart, Integer>>();
            contributions.forEach((ghost, sources) -> byGhost.put(ghost, Map.copyOf(sources)));
            copy.put(part, Map.copyOf(byGhost));
        });
        return Map.copyOf(copy);
    }

    public Map<ResourceLocation, Integer> contributions(
            SpiritualPart target
    ) {

        Map<ResourceLocation, Integer> result =
                new HashMap<>();

        for (ResourceLocation ghost :
                matrix.getOrDefault(target, Map.of()).keySet()) {

            result.put(
                    ghost,
                    contribution(
                            target,
                            ghost
                    )
            );
        }

        return Map.copyOf(result);
    }
}