package com.qidate.qisplan2.ghost.reboot;

public record GhostRebootCommit(
        long gameTime,
        GhostRebootSnapshot snapshot,
        GhostRebootDiff diff
) {}