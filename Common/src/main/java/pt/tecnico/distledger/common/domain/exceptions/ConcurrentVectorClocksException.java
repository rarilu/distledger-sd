package pt.tecnico.distledger.common.domain.exceptions;

/** Represents an exception thrown when two concurrent vector clocks are compared. */
public class ConcurrentVectorClocksException extends RuntimeException {
  public ConcurrentVectorClocksException() {
    super("Concurrent vector clocks cannot be compared");
  }
}
