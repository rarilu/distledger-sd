package pt.tecnico.distledger.server.domain.exceptions;

import pt.tecnico.distledger.common.domain.VectorClock;

/**
 * Represents an exception thrown when a request cannot be processed due to a state timestamp being
 * outdated.
 */
public class OutdatedStateException extends RuntimeException {
  /** Creates a new exception with the given request and state timestamps. */
  public OutdatedStateException(VectorClock requestTimeStamp, VectorClock stateTimeStamp) {
    super(
        "Request has timestamp "
            + requestTimeStamp
            + ", but the state timestamp is still "
            + stateTimeStamp);
  }
}
