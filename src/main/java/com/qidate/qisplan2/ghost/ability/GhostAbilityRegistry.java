package com.qidate.qisplan2.ghost.ability;

import com.qidate.qisplan2.ghost.ability.knockingghost.KnockingGhostAbility;
import com.qidate.qisplan2.ghost.ability.nightwanderer.NightWandererAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class GhostAbilityRegistry {

    private static final Map<
            ResourceLocation,
            PossessedGhostAbility
            > ABILITIES =
            new HashMap<>();

    private static boolean initialized = false;

    private GhostAbilityRegistry() {
    }

    public static void register(
            PossessedGhostAbility ability
    ) {
        ABILITIES.put(
                ability.id(),
                ability
        );
    }

    public static PossessedGhostAbility get(
            ResourceLocation id
    ) {
        return ABILITIES.get(id);
    }

    /**
     * 注册所有鬼的驾驭能力。
     */
    public static void bootstrap() {

        if (initialized) {
            return;
        }

        initialized = true;

        // 夜游鬼
        register(
                new NightWandererAbility()
        );

        // 敲门鬼
        register(
                new KnockingGhostAbility()
        );
    }
}