package com.qidate.qisplan2.ghost.module.event;
import java.util.UUID;

/** 已形成的外来压制事实；不是发起能力的请求。 */
public record SupernaturalSuppressionEvent(UUID target, double strength) implements GhostEvent {}
