package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.visitors.OperationVisitor;

public abstract class Operation {
  private String userId;

  public Operation(String userId) {
    this.userId = userId;
  }

  public String getUserId() {
    return this.userId;
  }

  public abstract void accept(OperationVisitor visitor);
}
