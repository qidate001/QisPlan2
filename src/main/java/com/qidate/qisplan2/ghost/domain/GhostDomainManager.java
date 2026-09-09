package com.qidate.qisplan2.ghost.domain;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.server.level.ServerLevel;

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

    public void add(
            GhostDomain domain
    ) {
        domains.put(
                domain.getId(),
                domain
        );

        QisPlan2.LOGGER.info(
                "[GhostDomain] ADD id={} type={} source={} pos=({}, {}, {})",
                domain.getId(),
                domain.getType(),
                domain.getSourceUUID(),
                domain.getX(),
                domain.getY(),
                domain.getZ()
        );
    }

    public void remove(
            UUID id
    ) {
        GhostDomain domain =
                domains.remove(id);

        if (domain != null) {

            QisPlan2.LOGGER.info(
                    "[GhostDomain] REMOVE id={} type={}",
                    id,
                    domain.getType()
            );
        }
    }

    public void removeBySource(
            UUID sourceUUID
    ) {

        QisPlan2.LOGGER.info(
                "[GhostDomain] removeBySource: source={}, 当前鬼域数量={}",
                sourceUUID,
                domains.size()
        );

        List<UUID> remove =
                new ArrayList<>();

        for (GhostDomain domain : domains.values()) {
            if (sourceUUID.equals(
                    domain.getSourceUUID()
            )) {

                remove.add(
                        domain.getId()
                );
            }
        }

        for (UUID id : remove) {

            domains.remove(id);
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