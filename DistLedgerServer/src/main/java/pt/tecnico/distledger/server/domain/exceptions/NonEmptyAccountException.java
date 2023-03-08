package pt.tecnico.distledger.server.domain.exceptions;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class NonEmptyAccountException extends StatusRuntimeException {
  public NonEmptyAccountException(String userId, int amount) {
    super(
        Status.FAILED_PRECONDITION.withDescription(
            "Account for user " + userId + " has " + amount + " left, needs to be empty"));
  }
}
