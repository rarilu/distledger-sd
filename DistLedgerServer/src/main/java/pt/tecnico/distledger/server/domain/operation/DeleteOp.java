package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.visitors.OperationVisitor;

public class DeleteOp extends Operation {
  public DeleteOp(String userId) {
    super(userId);
  }

  @Override
  public void accept(OperationVisitor visitor) {
    visitor.visit(this);
  }
}
