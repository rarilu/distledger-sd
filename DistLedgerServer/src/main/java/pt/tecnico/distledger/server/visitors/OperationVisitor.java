package pt.tecnico.distledger.server.visitors;

import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

/** Represents a visitor for operations. */
public interface OperationVisitor {
  void visit(CreateOp op);

  void visit(TransferOp op);
}
