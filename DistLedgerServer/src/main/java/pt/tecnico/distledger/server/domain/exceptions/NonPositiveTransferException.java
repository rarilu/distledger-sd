package pt.tecnico.distledger.server.domain.exceptions;

import io.grpc.Status;

public class NonPositiveTransferException extends OperationException {
  public NonPositiveTransferException() {
    super(
        Status.INVALID_ARGUMENT.withDescription(
            "Transfers with non-positive amount are not allowed"));
  }
}
