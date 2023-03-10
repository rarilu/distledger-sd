package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.visitors.OperationVisitor;

/** Represents a state-modifying operation. */
public abstract class Operation {
  private final String userId;

  protected Operation(String userId) {
    this.userId = userId;
  }

  public String getUserId() {
    return this.userId;
  }

  public abstract void accept(OperationVisitor visitor);
}
