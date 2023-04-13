package pt.tecnico.distledger.server.domain.exceptions;

import pt.tecnico.distledger.common.domain.VectorClock;

/**
 * Represents an exception thrown when a request cannot be processed due to a state timestamp being
 * outdated.
 */
public class OutdatedStateException extends RuntimeException {
  /** Creates a new exception with the given request and state timestamps. */
  public OutdatedStateException(VectorClock requestTimeStamp) {
    super(
        "Request has timestamp "
            + requestTimeStamp
            + " which is more recent than the current state");
  }
}
