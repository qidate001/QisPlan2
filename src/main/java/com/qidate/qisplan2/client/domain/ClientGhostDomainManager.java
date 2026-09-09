package com.qidate.qisplan2.client.domain;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientGhostDomainManager {

    private static final Map<UUID, ClientGhostDomain> DOMAINS =
            new LinkedHashMap<>();

    private ClientGhostDomainManager() {
    }

    public static void add(
            UUID id,
            UUID sourceUUID,
            ResourceLocation domainType,
            ResourceLocation dimension,
            double x,
            double y,
            double z,
            double radius
    ) {

        ClientGhostDomain domain =
                new ClientGhostDomain(
                        id,
                        sourceUUID,
                        domainType,
                        dimension,
                        x,
                        y,
                        z,
                        radius
                );

        DOMAINS.put(id, domain);
    }

    public static void remove(UUID id) {
        DOMAINS.remove(id);
    }

    public static ClientGhostDomain get(UUID id) {
        return DOMAINS.get(id);
    }

    public static Collection<ClientGhostDomain> getDomains() {
        return Collections.unmodifiableCollection(
                DOMAINS.values()
        );
    }

    public static boolean isPositionInsideDomain(
            double x,
            double y,
            double z
    ) {

        ResourceLocation currentDimension =
                getCurrentDimension();

        if (currentDimension == null) {
            return false;
        }

        for (ClientGhostDomain domain : DOMAINS.values()) {

            if (!domain.getDimension().equals(currentDimension)) {
                continue;
            }

            if (domain.contains(x, y, z)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isInsideDomain() {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return false;
        }

        return isPositionInsideDomain(
                minecraft.player.getX(),
                minecraft.player.getY(),
                minecraft.player.getZ()
        );
    }

    public static void clear() {
        DOMAINS.clear();
    }

    private static ResourceLocation getCurrentDimension() {

        Minecraft minecraft =
                Minecraft.getInstance();

        ClientLevel level =
                minecraft.level;

        if (level == null) {
            return null;
        }

        return level.dimension().location();
    }
}