package pt.tecnico.distledger.server.visitors;

import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

public abstract class OperationVisitor {
  public abstract void visit(CreateOp op);

  public abstract void visit(DeleteOp op);

  public abstract void visit(TransferOp op);
}
