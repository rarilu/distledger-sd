package pt.tecnico.distledger.server.visitors;

import pt.tecnico.distledger.server.domain.operation.Operation;

/** Represents an operation visitor that executes operations. */
public interface OperationExecutor extends OperationVisitor {
  default void execute(Operation op) {
    op.accept(this);
  }
}
