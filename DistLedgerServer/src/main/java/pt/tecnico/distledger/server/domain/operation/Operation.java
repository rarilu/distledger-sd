package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.server.visitors.OperationVisitor;

/** Represents a state-modifying operation. */
public abstract class Operation {
  private final String userId;
  private final VectorClock prevTimeStamp;
  private final VectorClock timeStamp;
  private boolean stable = false;

  protected Operation(String userId, VectorClock prevTimeStamp, VectorClock timeStamp) {
    this.userId = userId;
    this.prevTimeStamp = prevTimeStamp;
    this.timeStamp = timeStamp;
  }

  public void setStable(boolean stable) {
    this.stable = stable;
  }

  public boolean isStable() {
    return this.stable;
  }

  public String getUserId() {
    return this.userId;
  }

  public VectorClock getPrevTimeStamp() {
    return this.prevTimeStamp;
  }

  public VectorClock getTimeStamp() {
    return this.timeStamp;
  }

  public abstract void accept(OperationVisitor visitor);
}
