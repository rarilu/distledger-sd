package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.exceptions.OperationException;
import pt.tecnico.distledger.server.domain.visitors.OperationVisitor;

public class DeleteOp extends Operation {
  public DeleteOp(String userId) {
    super(userId);
  }

  @Override
  public void accept(OperationVisitor visitor) throws OperationException {
    visitor.visit(this);
  }
}
