package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.OperationException;

public abstract class Operation {
  private String userId;

  public Operation(String userId) {
    this.userId = userId;
  }

  public String getUserId() {
    return this.userId;
  }

  public abstract void apply(ServerState state) throws OperationException;
}
