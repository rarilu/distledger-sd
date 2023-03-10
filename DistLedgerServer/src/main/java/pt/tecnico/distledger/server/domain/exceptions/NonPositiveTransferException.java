package pt.tecnico.distledger.server.domain.exceptions;

/** Represents an exception thrown when a transfer with a non-positive amount is attempted. */
public class NonPositiveTransferException extends RuntimeException {
  public NonPositiveTransferException() {
    super("Transfers with non-positive amount are not allowed");
  }
}
