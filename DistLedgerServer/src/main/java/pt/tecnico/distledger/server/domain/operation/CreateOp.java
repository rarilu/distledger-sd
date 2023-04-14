package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.server.domain.exceptions.SystemAccountException;
import pt.tecnico.distledger.server.visitors.OperationVisitor;

/** Represents an account creation operation. */
public class CreateOp extends Operation {
  /** Creates a new account creation operation. */
  public CreateOp(String userId, VectorClock prevTimeStamp, VectorClock timeStamp) {
    super(userId, prevTimeStamp, timeStamp);

    if (userId.equals("broker")) {
      throw new SystemAccountException(userId);
    }
  }

  @Override
  public void accept(OperationVisitor visitor) {
    visitor.visit(this);
  }
}
