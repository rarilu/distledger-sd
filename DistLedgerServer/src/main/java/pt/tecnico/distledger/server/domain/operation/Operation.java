package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.server.visitors.OperationVisitor;

/** Represents a state-modifying operation. */
public abstract class Operation {
  private final String userId;
  private final VectorClock prevTimeStamp;

  protected Operation(String userId, VectorClock prevTimeStamp) {
    this.userId = userId;
    this.prevTimeStamp = prevTimeStamp;
  }

  public String getUserId() {
    return this.userId;
  }

  public VectorClock getPrevTimeStamp() {
    return this.prevTimeStamp;
  }

  public abstract void accept(OperationVisitor visitor);
}
