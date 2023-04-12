package pt.tecnico.distledger.server.domain.exceptions;

import pt.tecnico.distledger.common.domain.VectorClock;

public class OutdatedStateException extends RuntimeException {
  public OutdatedStateException(VectorClock requestTS, VectorClock stateTS) {
    super("Request has timestamp " + requestTS + ", but the state timestamp is still " + stateTS);
  }
}
