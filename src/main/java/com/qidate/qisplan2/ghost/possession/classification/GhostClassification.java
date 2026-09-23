package com.qidate.qisplan2.ghost.possession.classification;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class GhostClassification {

    private final Set<GhostTag> tags;

    private GhostClassification(
            Set<GhostTag> tags
    ) {

        this.tags =
                Collections.unmodifiableSet(
                        EnumSet.copyOf(tags)
                );
    }

    public static GhostClassification of(
            GhostTag... tags
    ) {

        EnumSet<GhostTag> set =
                EnumSet.noneOf(
                        GhostTag.class
                );

        Collections.addAll(
                set,
                tags
        );

        return new GhostClassification(
                set
        );
    }

    public static GhostClassification empty() {

        return new GhostClassification(
                EnumSet.noneOf(
                        GhostTag.class
                )
        );
    }

    public boolean has(
            GhostTag tag
    ) {

        return tags.contains(tag);
    }

    public Set<GhostTag> tags() {

        return tags;
    }
}