package pt.tecnico.distledger.server.domain.operation;

import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.server.visitors.OperationVisitor;

/** Represents a state-modifying operation. */
public abstract class Operation {
  private final String userId;
  private final VectorClock prevTimeStamp;
  private final VectorClock timeStamp;
  private AtomicBoolean stable = new AtomicBoolean(false);
  private AtomicBoolean failed = new AtomicBoolean(false);

  protected Operation(String userId, VectorClock prevTimeStamp, VectorClock timeStamp) {
    this.userId = userId;
    this.prevTimeStamp = prevTimeStamp;
    this.timeStamp = timeStamp;
  }

  public void setStable() {
    this.stable.set(true);
  }

  public boolean isStable() {
    return this.stable.get();
  }

  public void setFailed() {
    this.failed.set(true);
  }

  public boolean hasFailed() {
    return this.failed.get();
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
