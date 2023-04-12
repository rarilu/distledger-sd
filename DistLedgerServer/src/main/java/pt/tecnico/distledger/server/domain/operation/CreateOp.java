package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.server.visitors.OperationVisitor;

/** Represents an account creation operation. */
public class CreateOp extends Operation {
  public CreateOp(String userId, VectorClock prevTS) {
    super(userId, prevTS);
  }

  @Override
  public void accept(OperationVisitor visitor) {
    visitor.visit(this);
  }
}
