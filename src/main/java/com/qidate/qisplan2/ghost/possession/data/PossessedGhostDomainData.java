package com.qidate.qisplan2.ghost.possession.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * ========================================
 * 驾驭厉鬼的鬼域持久数据
 * ========================================
 *
 * <p>
 * 描述一只被玩家驾驭的厉鬼自身所拥有的鬼域状态。
 * </p>
 *
 * <p>
 * 注意：
 *
 * 这不是 GhostDomain。
 *
 * PossessedGhostDomainData：
 *     持久化数据
 *
 * GhostDomain：
 *     世界运行时对象
 * </p>
 *
 * <p>
 * 并不是所有厉鬼都拥有鬼域。
 * 是否存在这个数据对象，
 * 由 PossessedGhostData 中的 Optional 决定。
 * </p>
 */
public record PossessedGhostDomainData(
        boolean open,
        double strength,
        double radius,
        int layer
) {

    /**
     * 最小鬼域范围。
     */
    public static final double MIN_RADIUS = 0.0D;

    /**
     * 层数数据范围。
     */
    public static final int MIN_LAYER = 1;
    public static final int MAX_LAYER = 10;

    /**
     * 创建鬼域数据。
     */
    public PossessedGhostDomainData {
        strength = Math.max(0.0D, strength);
        radius = Math.max(MIN_RADIUS, radius);
        layer = Math.clamp(layer, MIN_LAYER, MAX_LAYER);
    }

    /*
     * ============================================================
     * Codec
     * ============================================================
     */

    public static final Codec<PossessedGhostDomainData> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.BOOL.optionalFieldOf("open", false)
                                    .forGetter(PossessedGhostDomainData::open),

                            Codec.DOUBLE.optionalFieldOf("strength", 0.0D)
                                    .forGetter(PossessedGhostDomainData::strength),

                            Codec.DOUBLE.optionalFieldOf("radius", 0.0D)
                                    .forGetter(PossessedGhostDomainData::radius),

                            Codec.INT.optionalFieldOf("layer", 1)
                                    .forGetter(PossessedGhostDomainData::layer)

                    ).apply(instance, PossessedGhostDomainData::new)
            );

    /*
     * ============================================================
     * 网络同步
     * ============================================================
     */

    public static final StreamCodec<RegistryFriendlyByteBuf, PossessedGhostDomainData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    PossessedGhostDomainData::open,

                    ByteBufCodecs.DOUBLE,
                    PossessedGhostDomainData::strength,

                    ByteBufCodecs.DOUBLE,
                    PossessedGhostDomainData::radius,

                    ByteBufCodecs.INT,
                    PossessedGhostDomainData::layer,

                    PossessedGhostDomainData::new
            );

    /*
     * ============================================================
     * 工厂
     * ============================================================
     */

    /**
     * 创建一个默认关闭的鬼域数据。
     */
    public static PossessedGhostDomainData create(
            double strength,
            double radius
    ) {
        return new PossessedGhostDomainData(
                false,
                strength,
                radius,
                1
        );
    }

    /**
     * 创建一个已经开启的鬼域数据。
     */
    public static PossessedGhostDomainData createOpen(
            double strength,
            double radius
    ) {
        return new PossessedGhostDomainData(
                true,
                strength,
                radius,
                1
        );
    }

    /*
     * ============================================================
     * 修改方法
     * ============================================================
     */

    public PossessedGhostDomainData withOpen(
            boolean open
    ) {
        return new PossessedGhostDomainData(
                open,
                strength,
                radius,
                layer
        );
    }

    public PossessedGhostDomainData withStrength(
            double strength
    ) {
        return new PossessedGhostDomainData(
                open,
                strength,
                radius,
                layer
        );
    }

    public PossessedGhostDomainData withRadius(
            double radius
    ) {
        return new PossessedGhostDomainData(
                open,
                strength,
                radius,
                layer
        );
    }

    public PossessedGhostDomainData withLayer(int layer) {
        return new PossessedGhostDomainData(
                open,
                strength,
                radius,
                layer
        );
    }
}