package com.qidate.qisplan2.ghost.ability;

import com.qidate.qisplan2.ghost.ability.doorghost.ClosingGhostAbility;
import com.qidate.qisplan2.ghost.ability.doorghost.OpeningGhostAbility;
import com.qidate.qisplan2.ghost.ability.knockingghost.KnockingGhostAbility;
import com.qidate.qisplan2.ghost.ability.nightwanderer.NightWandererAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    public static boolean contains(
            ResourceLocation id
    ) {
        return ABILITIES.containsKey(id);
    }

    public static Set<ResourceLocation> ids() {
        return Collections.unmodifiableSet(
                ABILITIES.keySet()
        );
    }

    /**
     * 注册所有鬼的驾驭能力。
     */
    public static void bootstrap() {

        if (initialized) {
            return;
        }

        initialized = true;

        register(
                new NightWandererAbility()
        );

        register(
                new KnockingGhostAbility()
        );

        register(
                new OpeningGhostAbility()
        );

        register(
                new ClosingGhostAbility()
        );
    }
}