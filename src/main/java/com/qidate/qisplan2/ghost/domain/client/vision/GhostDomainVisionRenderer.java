package com.qidate.qisplan2.ghost.domain.client.vision;

import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainVisionSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * 鬼域视觉描边渲染器。
 *
 * <p>
 * 负责在鬼域主人客户端中，
 * 为鬼域范围内的实体绘制特殊透视描边。
 *
 * <p>
 * 本类与 {@code ghost.domain.client.renderer}
 * 是两码事。
 *
 * <p>
 * {@code renderer} 包负责鬼域本身的画面后处理；
 * 本类负责鬼域赋予实体的视觉效果。
 */
public final class GhostDomainVisionRenderer {

    private GhostDomainVisionRenderer() {
    }

    /**
     * 判断实体当前是否应该进行鬼域视觉描边。
     *
     * @param entity 实体
     * @return 是否需要描边
     */
    public static boolean shouldOutline(
            Entity entity
    ) {

        if (entity == null) {
            return false;
        }

        return ClientGhostDomainVisionSystem
                .shouldOutline(
                        entity.getUUID()
                );
    }

    /**
     * 获取实体当前应该使用的描边颜色。
     *
     * @param entity 实体
     * @return RGB 颜色；没有视觉效果时返回 null
     */
    public static Integer getOutlineColor(
            Entity entity
    ) {

        if (!shouldOutline(entity)) {
            return null;
        }

        return ClientGhostDomainVisionSystem
                .getColor(
                        entity.getUUID()
                );
    }

    /**
     * 获取当前客户端。
     *
     * <p>
     * 当前暂时只作为渲染入口的统一访问点，
     * 实际描边绘制将在后续步骤接入 Minecraft
     * 的实体渲染阶段。
     */
    public static Minecraft getMinecraft() {

        return Minecraft.getInstance();
    }
}