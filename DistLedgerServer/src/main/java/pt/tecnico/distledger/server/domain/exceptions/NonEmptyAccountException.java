package pt.tecnico.distledger.server.domain.exceptions;

import io.grpc.Status;

public class NonEmptyAccountException extends OperationException {
  public NonEmptyAccountException(String userId, int amount) {
    super(
        Status.FAILED_PRECONDITION.withDescription(
            "Account for user " + userId + " has " + amount + " left, needs to be empty"));
  }
}
