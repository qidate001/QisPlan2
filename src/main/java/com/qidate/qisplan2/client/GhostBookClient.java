package com.qidate.qisplan2.client;

import net.minecraft.client.Minecraft;

public final class GhostBookClient {

    private GhostBookClient() {
    }

    public static void open() {
        Minecraft.getInstance().setScreen(
                new GhostBookScreen()
        );
    }
}