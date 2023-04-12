package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.server.visitors.OperationVisitor;

/** Represents a state-modifying operation. */
public abstract class Operation {
  private final String userId;
  private final VectorClock prevTS;

  protected Operation(String userId, VectorClock prevTS) {
    this.userId = userId;
    this.prevTS = prevTS;
  }

  public String getUserId() {
    return this.userId;
  }

  public VectorClock getPrevTS() {
    return this.prevTS;
  }

  public abstract void accept(OperationVisitor visitor);
}
