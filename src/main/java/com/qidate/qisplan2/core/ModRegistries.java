package com.qidate.qisplan2.core;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import static com.qidate.qisplan2.QisPlan2.MODID;

public class ModRegistries {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(
                    MODID
            );

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    MODID
            );

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    MODID
            );

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.BLOCK_ENTITY_TYPE,
                    MODID
            );

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(
                    BuiltInRegistries.ARMOR_MATERIAL,
                    MODID
            );

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(
                    BuiltInRegistries.SOUND_EVENT,
                    MODID
            );

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    BuiltInRegistries.MENU,
                    MODID
            );

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(
                    Registries.RECIPE_TYPE,
                    MODID
            );

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(
                    Registries.RECIPE_SERIALIZER,
                    MODID
            );

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    MODID
            );

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(
                    BuiltInRegistries.PARTICLE_TYPE,
                    MODID
            );

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(
                    NeoForgeRegistries.ATTACHMENT_TYPES,
                    MODID
            );

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(
                    NeoForgeRegistries.FLUID_TYPES,
                    MODID
            );

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(
                    BuiltInRegistries.FLUID,
                    MODID
            );
}
