package com.qidate.qisplan2.entity;

import net.minecraft.resources.ResourceLocation;

public record GhostPaintingVariant(
        ResourceLocation texture,
        int width,
        int height
) { }