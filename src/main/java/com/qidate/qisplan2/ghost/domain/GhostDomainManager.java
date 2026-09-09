package com.qidate.qisplan2.ghost.domain;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.network.QisNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class GhostDomainManager {

    private static final Map<ServerLevel, GhostDomainManager>
            MANAGERS = new WeakHashMap<>();

    private final ServerLevel level;

    private final Map<UUID, GhostDomain> domains =
            new LinkedHashMap<>();

    private GhostDomainManager(
            ServerLevel level
    ) {
        this.level = level;
    }

    public static GhostDomainManager get(
            ServerLevel level
    ) {
        return MANAGERS.computeIfAbsent(
                level,
                GhostDomainManager::new
        );
    }

    public void add(GhostDomain domain) {

        domains.put(domain.getId(), domain);

        domain.getBehavior().onCreate(
                level,
                domain
        );

        QisNetwork.sendGhostDomainAdd(
                level,
                domain
        );
    }

    public void syncToPlayer(ServerPlayer player) {

        for (GhostDomain domain : domains.values()) {

            QisNetwork.sendGhostDomainAdd(
                    player,
                    domain
            );
        }

        QisPlan2.LOGGER.info(
                "[GhostDomain] SYNC {} domains to player {}",
                domains.size(),
                player.getGameProfile().getName()
        );
    }

    public void remove(UUID id) {

        GhostDomain domain = domains.remove(id);

        if (domain == null) {
            return;
        }

        domain.getBehavior().onRemove(
                level,
                domain
        );

        QisNetwork.sendGhostDomainRemove(
                level,
                domain.getId()
        );
    }

    public void removeBySource(UUID sourceUUID) {

        List<UUID> remove = new ArrayList<>();

        for (GhostDomain domain : domains.values()) {
            if (sourceUUID.equals(domain.getSourceUUID())) {
                remove.add(domain.getId());
            }
        }

        for (UUID id : remove) {
            remove(id);
        }
    }

    public void updatePosition(
            UUID domainId,
            double x,
            double y,
            double z
    ) {
        GhostDomain domain = domains.get(domainId);

        if (domain == null) {
            return;
        }

        if (domain.getUpdateMode() == GhostDomainUpdateMode.MANUAL) {

            domain.setPosition(x, y, z);

            QisNetwork.sendGhostDomainUpdate(
                    level,
                    domain
            );

            return;
        }

        double dx = x - domain.getX();
        double dy = y - domain.getY();
        double dz = z - domain.getZ();

        double distanceSquared =
                dx * dx + dy * dy + dz * dz;

        double threshold =
                domain.getUpdateDistance();

        if (distanceSquared < threshold * threshold) {
            return;
        }

        domain.setPosition(x, y, z);

        QisNetwork.sendGhostDomainUpdate(
                level,
                domain
        );
    }

    public GhostDomain get(
            UUID id
    ) {
        return domains.get(id);
    }

    public GhostDomain getBySource(
            UUID sourceUUID
    ) {

        for (GhostDomain domain : domains.values()) {

            if (sourceUUID.equals(
                    domain.getSourceUUID()
            )) {
                return domain;
            }
        }

        return null;
    }

    public Collection<GhostDomain> getDomains() {
        return Collections.unmodifiableCollection(
                domains.values()
        );
    }

    public void tick() {

        for (GhostDomain domain : domains.values()) {

            domain.getBehavior().tick(
                    level,
                    domain
            );
        }
    }
}