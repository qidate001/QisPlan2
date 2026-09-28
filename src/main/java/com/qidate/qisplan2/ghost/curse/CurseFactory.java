package com.qidate.qisplan2.ghost.curse;

import java.util.UUID;

@FunctionalInterface
public interface CurseFactory {

    Curse create(
            UUID target
    );
}