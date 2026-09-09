package com.qidate.qisplan2.client.domain;

import com.qidate.qisplan2.ghost.domain.GhostDomain;

import java.util.*;

public final class ClientGhostDomainManager {

    private static final Map<UUID, GhostDomain>
            DOMAINS = new LinkedHashMap<>();

    private ClientGhostDomainManager() {
    }


    public static void add(
            GhostDomain domain
    ) {

        DOMAINS.put(
                domain.getId(),
                domain
        );
    }


    public static void remove(
            UUID id
    ) {

        DOMAINS.remove(id);
    }


    public static void clear() {

        DOMAINS.clear();
    }


    public static Collection<GhostDomain>
    getDomains() {

        return Collections.unmodifiableCollection(
                DOMAINS.values()
        );
    }


    public static List<GhostDomain>
    getDomainsAt(
            double x,
            double y,
            double z
    ) {

        List<GhostDomain> result =
                new ArrayList<>();

        for (GhostDomain domain :
                DOMAINS.values()) {

            if (domain.contains(
                    x,
                    y,
                    z
            )) {

                result.add(domain);
            }
        }

        return result;
    }


    public static boolean isInside(
            double x,
            double y,
            double z
    ) {

        for (GhostDomain domain :
                DOMAINS.values()) {

            if (domain.contains(
                    x,
                    y,
                    z
            )) {

                return true;
            }
        }

        return false;
    }
}