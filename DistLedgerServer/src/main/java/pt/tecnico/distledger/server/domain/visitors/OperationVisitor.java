package pt.tecnico.distledger.server.domain.visitors;

import pt.tecnico.distledger.server.domain.exceptions.OperationException;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

public abstract class OperationVisitor {
  public abstract void visit(CreateOp op) throws OperationException;

  public abstract void visit(DeleteOp op) throws OperationException;

  public abstract void visit(TransferOp op) throws OperationException;
}
