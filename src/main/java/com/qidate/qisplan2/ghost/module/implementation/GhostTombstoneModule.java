package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.core.ModTags;
import com.qidate.qisplan2.ghost.module.GhostInscriptionCapability;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.GhostUseEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 鬼墓碑模块。
 *
 * 负责定义鬼墓碑对应的灵异模块身份，
 * 并响应通用灵异使用事件。
 */
public final class GhostTombstoneModule implements GhostModule {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_tombstone"
            );

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    /**
     * 定义鬼墓碑模块的新实例初始属性。
     */
    @Override
    public GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(
                25.0D,
                0.0D,
                0.0D
        );
    }

    @Override
    public boolean supports(GhostEvent event) {
        return event instanceof GhostUseEvent;
    }

    @Override
    public void onEvent(
            GhostModuleContext context,
            GhostEvent event
    ) {
        if (!(event instanceof GhostUseEvent useEvent)) {
            return;
        }

        if (!(context.host() instanceof GhostInscriptionCapability capability)) {
            return;
        }

        if (!(useEvent.user() instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }

        ItemStack stack = player.getMainHandItem();

        if (!stack.is(ModTags.Items.GHOST_TOMBSTONE_INSCRIBABLE)) {
            return;
        }

        capability.requestInscription(useEvent.user());
    }
}