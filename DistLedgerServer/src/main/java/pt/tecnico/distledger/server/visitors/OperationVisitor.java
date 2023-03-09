package pt.tecnico.distledger.server.visitors;

import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

public interface OperationVisitor {
  void visit(CreateOp op);

  void visit(DeleteOp op);

  void visit(TransferOp op);
}
