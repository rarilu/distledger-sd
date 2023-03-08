package pt.tecnico.distledger.server.domain.exceptions;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class UnknownAccountException extends StatusRuntimeException {
  public UnknownAccountException(String account) {
    super(Status.NOT_FOUND.withDescription("Account " + account + " does not exist"));
  }
}
