package pt.tecnico.distledger.server.domain.exceptions;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class AccountAlreadyExistsException extends StatusRuntimeException {
  public AccountAlreadyExistsException(String userId) {
    super(Status.ALREADY_EXISTS.withDescription("Account for user " + userId + " already exists"));
  }
}
