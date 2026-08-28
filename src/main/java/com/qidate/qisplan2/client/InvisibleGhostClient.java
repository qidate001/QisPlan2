package com.qidate.qisplan2.client;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.client.renderer.InvisibleGhostRenderer;
import com.qidate.qisplan2.core.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class InvisibleGhostClient {

    private InvisibleGhostClient() {
    }

    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                ModEntities.INVISIBLE_GHOST.get(),
                InvisibleGhostRenderer::new
        );
    }
}