package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.visitors.OperationVisitor;

/** Represents an account creation operation. */
public class CreateOp extends Operation {
  public CreateOp(String userId) {
    super(userId);
  }

  @Override
  public void accept(OperationVisitor visitor) {
    visitor.visit(this);
  }
}
