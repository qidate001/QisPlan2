package com.qidate.qisplan2.ghost.curse.type.ghosttombstone;

import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * 鬼墓碑诅咒。
 *
 * 当前阶段只负责存在于诅咒系统中。
 * 后续再逐步增加具体诅咒效果。
 */
public class GhostTombstoneCurse implements Curse {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "ghost_tombstone"
            );

    private final UUID id;

    private final UUID target;

    private final GhostTombstoneCurseSource source;

    public GhostTombstoneCurse(
            UUID target,
            GhostTombstoneCurseSource source
    ) {
        this.id = UUID.randomUUID();
        this.target = target;
        this.source = source;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public ResourceLocation getType() {
        return ID;
    }

    @Override
    public UUID getTarget() {
        return target;
    }

    @Override
    public CurseSource getSource() {
        return source;
    }

    @Override
    public void tick(
            MinecraftServer server
    ) {
        // 当前阶段暂时没有诅咒效果。
    }

    @Override
    public boolean isValid(
            MinecraftServer server
    ) {
        // 当前阶段暂时永久有效。
        return true;
    }
}