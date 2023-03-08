package pt.tecnico.distledger.server.domain.exceptions;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class ServerUnavailableException extends StatusRuntimeException {
  public ServerUnavailableException() {
    super(Status.UNAVAILABLE.withDescription("Server is unavailable"));
  }
}
