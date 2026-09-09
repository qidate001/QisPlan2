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

        QisPlan2.LOGGER.info(
                "[GhostDomain] ADD id={} type={} source={} pos=({}, {}, {})",
                domain.getId(),
                domain.getType(),
                domain.getSourceUUID(),
                domain.getX(),
                domain.getY(),
                domain.getZ()
        );

        ServerPlayer player =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(domain.getSourceUUID());

        if (player != null) {
            QisNetwork.sendGhostDomainAdd(
                    player,
                    domain
            );
        }
    }

    public void remove(UUID id) {

        GhostDomain domain = domains.remove(id);

        if (domain == null) {
            return;
        }

        QisPlan2.LOGGER.info(
                "[GhostDomain] REMOVE id={} type={}",
                domain.getId(),
                domain.getType()
        );

        ServerPlayer player =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(domain.getSourceUUID());

        if (player != null) {
            QisNetwork.sendGhostDomainRemove(
                    player,
                    domain.getId()
            );
        }
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
        // 暂时没有内容
    }
}