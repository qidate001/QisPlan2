package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

public record GhostRebootCommit(
        long gameTime,
        GhostRebootSnapshot snapshot,
        GhostRebootDiff diff
) {}