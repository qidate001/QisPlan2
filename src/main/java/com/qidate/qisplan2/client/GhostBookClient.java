package com.qidate.qisplan2.client;

import com.qidate.qisplan2.client.screen.GhostBookScreen;
import net.minecraft.client.Minecraft;

public final class GhostBookClient {

    private GhostBookClient() {
    }

    public static void open(
            String apiKey,
            String modelName
    ) {
        Minecraft.getInstance().setScreen(
                new GhostBookScreen(
                        apiKey,
                        modelName
                )
        );
    }
}