package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.server.visitors.OperationVisitor;

/** Represents a state-modifying operation. */
public abstract class Operation {
  private final String userId;
  private final VectorClock prevTimestamp;

  protected Operation(String userId, VectorClock prevTimestamp) {
    this.userId = userId;
    this.prevTimestamp = prevTimestamp;
  }

  public String getUserId() {
    return this.userId;
  }

  public VectorClock getPrevTimestamp() {
    return this.prevTimestamp;
  }

  public abstract void accept(OperationVisitor visitor);
}
