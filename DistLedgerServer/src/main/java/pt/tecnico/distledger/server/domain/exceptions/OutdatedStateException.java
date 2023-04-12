package pt.tecnico.distledger.server.domain.exceptions;

import pt.tecnico.distledger.common.domain.VectorClock;

/**
 * Represents an exception thrown when a request has a timestamp that doesn't happen before the
 * value timestamp.
 */
public class OutdatedStateException extends RuntimeException {
  /** Creates a new exception with the given request and state timestamps. */
  public OutdatedStateException(VectorClock requestTimestamp, VectorClock stateTimestamp) {
    super(
        "Request has timestamp "
            + requestTimestamp
            + ", but the state timestamp is still "
            + stateTimestamp);
  }
}
