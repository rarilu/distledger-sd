package pt.tecnico.distledger.server.visitors;

import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation;
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

public class LedgerStateGenerator implements OperationVisitor {
  private LedgerState.Builder builder;

  public LedgerStateGenerator() {
    this.builder = LedgerState.newBuilder();
  }

  public LedgerState build() {
    return this.builder.build();
  }

  @Override
  public void visit(CreateOp op) {
    this.builder.addLedger(
        Operation.newBuilder()
            .setType(OperationType.OP_CREATE_ACCOUNT)
            .setUserId(op.getUserId())
            .build());
  }

  @Override
  public void visit(DeleteOp op) {
    this.builder.addLedger(
        Operation.newBuilder()
            .setType(OperationType.OP_DELETE_ACCOUNT)
            .setUserId(op.getUserId())
            .build());
  }

  @Override
  public void visit(TransferOp op) {
    this.builder.addLedger(
        Operation.newBuilder()
            .setType(OperationType.OP_TRANSFER_TO)
            .setUserId(op.getUserId())
            .setDestUserId(op.getDestUserId())
            .setAmount(op.getAmount())
            .build());
  }
}
