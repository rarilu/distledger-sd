package pt.tecnico.distledger.server.visitors;

import pt.tecnico.distledger.common.grpc.ProtoUtils;
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation;
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

/** Generates a LedgerState by visiting operations. */
public class LedgerStateGenerator implements OperationVisitor {
  private final LedgerState.Builder builder;

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
            .setPrevTS(ProtoUtils.toProto(op.getPrevTimeStamp()))
            .setTS(ProtoUtils.toProto(op.getTimeStamp()))
            .setStable(op.isStable())
            .setFailed(op.hasFailed())
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
            .setPrevTS(ProtoUtils.toProto(op.getPrevTimeStamp()))
            .setTS(ProtoUtils.toProto(op.getTimeStamp()))
            .setStable(op.isStable())
            .setFailed(op.hasFailed())
            .build());
  }
}
