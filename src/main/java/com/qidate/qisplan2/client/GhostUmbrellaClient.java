package com.qidate.qisplan2.client;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.client.renderer.GhostUmbrellaRenderer;
import com.qidate.qisplan2.core.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class GhostUmbrellaClient {

    private GhostUmbrellaClient() {
    }

    @SubscribeEvent
    public static void registerAdditionalModels(
            ModelEvent.RegisterAdditional event
    ) {
        event.register(
                ModelResourceLocation.standalone(
                        ResourceLocation.fromNamespaceAndPath(
                                QisPlan2.MODID,
                                "item/ghost_umbrella_closed"
                        )
                )
        );

        event.register(
                ModelResourceLocation.standalone(
                        ResourceLocation.fromNamespaceAndPath(
                                QisPlan2.MODID,
                                "item/ghost_umbrella_open"
                        )
                )
        );
    }

    @SubscribeEvent
    public static void registerClientExtensions(
            RegisterClientExtensionsEvent event
    ) {
        QisPlan2.LOGGER.info(
                "[QisPlan2] 注册鬼雨伞客户端渲染器"
        );

        event.registerItem(
                new IClientItemExtensions() {

                    private final GhostUmbrellaRenderer renderer =
                            new GhostUmbrellaRenderer();

                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return renderer;
                    }
                },
                ModItems.GHOST_UMBRELLA.get()
        );
    }
}