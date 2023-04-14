package pt.tecnico.distledger.server.domain.operation;

import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.server.visitors.OperationVisitor;

/** Represents a state-modifying operation. */
public abstract class Operation {
  private final String userId;
  private final VectorClock prevTimeStamp;
  private final VectorClock replicaTimeStamp;
  private final int replicaId;
  private AtomicBoolean stable = new AtomicBoolean(false);
  private AtomicBoolean failed = new AtomicBoolean(false);

  protected Operation(
      String userId, VectorClock prevTimeStamp, VectorClock replicaTimeStamp, int replicaId) {
    this.userId = userId;
    this.prevTimeStamp = prevTimeStamp;
    this.replicaTimeStamp = replicaTimeStamp;
    this.replicaId = replicaId;
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

  public VectorClock getReplicaTimeStamp() {
    return this.replicaTimeStamp;
  }

  /**
   * Returns the timestamp of this operation, as described in the project statement.
   *
   * @return the timestamp of this operation.
   */
  public VectorClock getTimeStamp() {
    VectorClock timeStamp = new VectorClock(this.prevTimeStamp);
    timeStamp.mergeSingle(this.replicaTimeStamp, this.replicaId);
    return timeStamp;
  }

  public int getReplicaId() {
    return this.replicaId;
  }

  public abstract void accept(OperationVisitor visitor);
}
