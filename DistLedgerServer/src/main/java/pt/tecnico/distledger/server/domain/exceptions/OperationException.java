package pt.tecnico.distledger.server.domain.exceptions;

import io.grpc.Status;

public class OperationException extends Exception {
  private final Status status;

  public OperationException(Status status) {
    super(status.getDescription());
    this.status = status;
  }

  public final Status getGrpcStatus() {
    return this.status;
  }
}
