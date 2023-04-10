package pt.tecnico.distledger.common.domain.exceptions;

/** Represents an exception thrown when a transfer from an account to itself is attempted. */
public class ConcurrentVectorClocksException extends RuntimeException {
  public ConcurrentVectorClocksException() {
    super("Concurrent vector clocks cannot be compared");
  }
}
