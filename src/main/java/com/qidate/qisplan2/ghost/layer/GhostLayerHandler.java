package com.qidate.qisplan2.ghost.layer;

import com.qidate.qisplan2.core.ModAttachments;
import net.minecraft.world.entity.Entity;

public final class GhostLayerHandler {

    private GhostLayerHandler() {
    }

    public static int getLayer(
            Entity entity
    ) {
        return entity
                .getData(ModAttachments.GHOST_LAYER)
                .layer();
    }

    public static void setLayer(
            Entity entity,
            int layer
    ) {

        layer = Math.clamp(layer, 0, 6);

        entity.setData(
                ModAttachments.GHOST_LAYER,
                new GhostLayerData(layer)
        );
    }

    public static boolean sameLayer(
            Entity a,
            Entity b
    ) {
        return getLayer(a)
                == getLayer(b);
    }

    public static boolean canSee(Entity viewer, Entity target) {
        if (viewer == target) {
            return true;
        }

        int viewerLayer = getLayer(viewer);
        int targetLayer = getLayer(target);

        /*
         * 现实层：
         * 只能看见现实层实体。
         */
        if (viewerLayer == 0) {
            return targetLayer == 0;
        }

        /*
         * 灵异层：
         * 可以看见现实层实体，
         * 以及与自己处于同一灵异层的实体。
         */
        if (targetLayer == 0) {
            return true;
        }

        /*
         * 灵异层之间只能看见同层实体。
         */
        return viewerLayer == targetLayer;
    }

    public static boolean canInteract(
            Entity a,
            Entity b
    ) {
        return sameLayer(
                a,
                b
        );
    }
}