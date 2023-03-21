package pt.tecnico.distledger.server.visitors;

import pt.tecnico.distledger.server.domain.exceptions.UnsupportedOperationException;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

/**
 * Represents an operation executor that throws an exception when a write operation is attempted.
 */
public class DummyOperationExecutor implements OperationExecutor {
  @Override
  public void visit(CreateOp op) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(DeleteOp op) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(TransferOp op) {
    throw new UnsupportedOperationException();
  }
}
