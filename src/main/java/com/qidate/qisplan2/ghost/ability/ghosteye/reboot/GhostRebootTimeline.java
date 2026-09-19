package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

import java.util.ArrayDeque;
import java.util.Deque;

public final class GhostRebootTimeline {

    private final Deque<GhostRebootCommit> commits =
            new ArrayDeque<>();

    private long lastCommitGameTime = Long.MIN_VALUE;

    public boolean shouldCommit(
            long gameTime
    ) {

        return lastCommitGameTime == Long.MIN_VALUE
                || gameTime - lastCommitGameTime >= 200;
    }

    public GhostRebootCommit get(
            int index
    ) {
        if (index < 0 || index >= commits.size()) {
            return null;
        }

        return commits.stream()
                .skip(index)
                .findFirst()
                .orElse(null);
    }

    public void push(
            GhostRebootCommit commit
    ) {

        commits.addLast(commit);

        lastCommitGameTime =
                commit.gameTime();
    }

    public GhostRebootCommit latest() {
        return commits.peekLast();
    }

    public GhostRebootCommit popLast() {
        return commits.pollLast();
    }

    public int size() {
        return commits.size();
    }

    public void trim(int max) {

        while (commits.size() > max) {
            commits.removeFirst();
        }
    }

    public void truncateAfter(
            int index
    ) {

        while (commits.size() > index + 1) {
            commits.removeLast();
        }
    }

    public Deque<GhostRebootCommit> commits() {
        return commits;
    }
}