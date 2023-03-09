package pt.tecnico.distledger.server.domain.exceptions;

public class NonPositiveTransferException extends RuntimeException {
  public NonPositiveTransferException() {
    super("Transfers with non-positive amount are not allowed");
  }
}
