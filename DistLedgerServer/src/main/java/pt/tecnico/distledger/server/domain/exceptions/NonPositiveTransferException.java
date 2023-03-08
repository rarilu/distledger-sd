package pt.tecnico.distledger.server.domain.exceptions;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class NonPositiveTransferException extends StatusRuntimeException {
  public NonPositiveTransferException() {
    super(
        Status.INVALID_ARGUMENT.withDescription(
            "Transfers with non-positive amount are not allowed"));
  }
}
