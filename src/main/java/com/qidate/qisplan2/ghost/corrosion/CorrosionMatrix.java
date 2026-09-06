package com.qidate.qisplan2.ghost.corrosion;

import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class CorrosionMatrix {

    private final EnumMap<
            CorrosionType,
            Map<
                    ResourceLocation,
                    EnumMap<CorrosionType, Integer>
                    >
            > matrix =
            new EnumMap<>(CorrosionType.class);


    public CorrosionMatrix() {

        for (CorrosionType type :
                CorrosionType.values()) {

            matrix.put(
                    type,
                    new HashMap<>()
            );
        }
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
            CorrosionType target,
            ResourceLocation ghost,
            CorrosionType source,
            int amount
    ) {

        matrix.get(target)
                .computeIfAbsent(
                        ghost,
                        g -> new EnumMap<>(CorrosionType.class)
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
            CorrosionType target
    ) {

        return matrix.get(target)
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
            CorrosionType target,
            ResourceLocation ghost
    ) {

        EnumMap<CorrosionType, Integer> map =
                matrix.get(target).get(ghost);

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
     *
     * 例如：
     *
     * HAND
     * 来源 GLOBAL
     */
    public int contribution(
            CorrosionType target,
            ResourceLocation ghost,
            CorrosionType source
    ) {

        EnumMap<CorrosionType, Integer> map =
                matrix.get(target).get(ghost);

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
    public Map<
            CorrosionType,
            Map<
                    ResourceLocation,
                    EnumMap<CorrosionType, Integer>
                    >
            > matrix() {

        return Map.copyOf(matrix);
    }

    public Map<ResourceLocation, Integer> contributions(
            CorrosionType target
    ) {

        Map<ResourceLocation, Integer> result =
                new HashMap<>();

        for (ResourceLocation ghost :
                matrix.get(target).keySet()) {

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