package com.qidate.qisplan2.ghost.module.sound;

import com.qidate.qisplan2.block.entity.AbstractModularGhostBlockEntity;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 模块化方块宿主的声音能力同步器。
 *
 * 负责根据宿主携带的模块声明，自动注册和注销声音源。
 * 声音的传播判定和听众状态由 GhostSoundManager 负责。
 */
public final class GhostModuleSoundManager {

    /**
     * 每个宿主当前登记的模块声音声明。
     *
     * 使用宿主对象身份区分实例，避免不同宿主共享同一份注册记录。
     */
    private static final Map<AbstractModularGhostBlockEntity, Set<SoundRegistration>>
            REGISTRATIONS = new HashMap<>();

    private static final Map<ModularGhostEntity, Set<SoundRegistration>>
            ENTITY_REGISTRATIONS = new HashMap<>();

    private GhostModuleSoundManager() {
    }

    /**
     * 同步宿主当前的全部声音能力。
     *
     * 先注销该宿主的旧注册，再根据当前模块集合重新注册。
     * 模块移除前的 INTERRUPTED 必须由 removeModule() 提前处理；
     * 此处的注销只用于清理配置变更后的旧注册。
     */
    public static void syncHost(
            AbstractModularGhostBlockEntity host
    ) {
        Objects.requireNonNull(host, "host");

        if (!(host.getLevel() instanceof ServerLevel level)
                || host.isRemoved()) {
            return;
        }

        BlockPos pos = host.getBlockPos().immutable();

        unregisterRecordedSources(host, level);

        Set<SoundRegistration> registrations = new HashSet<>();

        for (GhostModuleData.Entry entry : host.getModules()) {
            GhostModule module = GhostModuleRegistry.get(entry.id());

            if (module == null) {
                continue;
            }

            for (GhostSoundDefinition definition : module.getSounds()) {
                if (definition == null) {
                    continue;
                }

                ResourceLocation moduleId = entry.id();

                /*
                 * 注册身份包含模块 ID 与声音类别。
                 * 同一方块宿主可以携带多个声明声音的模块。
                 */
                GhostSoundManager.registerModuleBlockSource(
                        level,
                        pos,
                        host,
                        moduleId,
                        definition.soundType(),
                        definition.soundEvent(),
                        definition.range()
                );

                registrations.add(
                        new SoundRegistration(
                                moduleId,
                                definition.soundType()
                        )
                );
            }
        }

        if (!registrations.isEmpty()) {
            REGISTRATIONS.put(host, registrations);
        }
    }

    /**
     * 同步实体宿主声明的全部模块声音。
     *
     * 已存在的声源只更新位置和配置，不注销再注册。
     * 只有被移除的模块声音才会真正注销。
     */
    public static void syncEntityHost(ModularGhostEntity host) {
        Objects.requireNonNull(host, "host");

        if (!(host.level() instanceof ServerLevel level)
                || host.isRemoved()) {
            return;
        }

        Set<SoundRegistration> currentRegistrations = new HashSet<>();

        Vec3 position = new Vec3(
                host.getX(),
                host.getY() + host.getBbHeight() * 0.5D,
                host.getZ()
        );

        for (GhostModuleData.Entry entry : host.getModules()) {
            GhostModule module = GhostModuleRegistry.get(entry.id());

            if (module == null) {
                continue;
            }

            for (GhostSoundDefinition definition : module.getSounds()) {
                if (definition == null) {
                    continue;
                }

                ResourceLocation moduleId = entry.id();

                SoundRegistration registration = new SoundRegistration(
                        moduleId,
                        definition.soundType()
                );

                currentRegistrations.add(registration);

                GhostSoundManager.registerModuleEntitySource(
                        level,
                        host.getUUID(),
                        host,
                        moduleId,
                        definition.soundType(),
                        definition.soundEvent(),
                        position,
                        definition.range()
                );
            }
        }

        Set<SoundRegistration> previousRegistrations =
                ENTITY_REGISTRATIONS.getOrDefault(host, Set.of());

        // 只注销本轮已经不存在的声音。
        for (SoundRegistration previous : previousRegistrations) {
            if (currentRegistrations.contains(previous)) {
                continue;
            }

            GhostSoundManager.unregisterModuleEntitySource(
                    level,
                    host.getUUID(),
                    previous.moduleId(),
                    previous.soundType()
            );
        }

        if (currentRegistrations.isEmpty()) {
            ENTITY_REGISTRATIONS.remove(host);
        } else {
            ENTITY_REGISTRATIONS.put(host, currentRegistrations);
        }
    }

    /**
     * 宿主移除时清理全部已登记声音。
     *
     * 调用时宿主的模块数据仍然存在。
     */
    public static void unregisterHost(
            AbstractModularGhostBlockEntity host
    ) {
        if (host == null) {
            return;
        }

        if (!(host.getLevel() instanceof ServerLevel level)) {
            REGISTRATIONS.remove(host);
            return;
        }

        unregisterRecordedSources(host, level);
    }

    /**
     * 注销实体宿主注册的全部模块声音。
     */
    public static void unregisterEntityHost(ModularGhostEntity host) {
        if (host == null) {
            return;
        }

        Set<SoundRegistration> registrations =
                ENTITY_REGISTRATIONS.remove(host);

        if (registrations == null) {
            return;
        }

        if (!(host.level() instanceof ServerLevel level)) {
            return;
        }

        for (SoundRegistration registration : registrations) {
            GhostSoundManager.unregisterModuleEntitySource(
                    level,
                    host.getUUID(),
                    registration.moduleId(),
                    registration.soundType()
            );
        }
    }

    private static void unregisterRecordedSources(
            AbstractModularGhostBlockEntity host,
            ServerLevel level
    ) {
        Set<SoundRegistration> previous = REGISTRATIONS.remove(host);

        if (previous == null) {
            return;
        }

        BlockPos pos = host.getBlockPos();

        for (SoundRegistration registration : previous) {
            GhostSoundManager.unregisterModuleBlockSource(
                    level,
                    pos,
                    registration.moduleId(),
                    registration.soundType()
            );
        }
    }

    private record SoundRegistration(
            ResourceLocation moduleId,
            ResourceLocation soundType
    ) {
    }
}