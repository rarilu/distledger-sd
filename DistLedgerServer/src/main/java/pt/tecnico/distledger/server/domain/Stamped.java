package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.common.domain.VectorClock;

/** Represents a read result with a timestamp. */
public record Stamped<T>(T value, VectorClock timeStamp) {}
