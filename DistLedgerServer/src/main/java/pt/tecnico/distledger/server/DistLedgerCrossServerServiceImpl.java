package pt.tecnico.distledger.server;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateResponse;
import pt.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.visitors.OperationExecutor;
import pt.tecnico.distledger.server.visitors.StandardOperationExecutor;

/** Implements the CrossServer service, handling gRPC requests. */
public class DistLedgerCrossServerServiceImpl
    extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {
  private static final String PROPAGATE_FAILED = "Prapagate State failed: ";

  private final ServerState state;
  private final OperationExecutor executor;

  public DistLedgerCrossServerServiceImpl(ServerState state) {
    this.state = state;
    this.executor = new StandardOperationExecutor(state);
  }

  private Operation parseOperation(DistLedgerCommonDefinitions.Operation operation) {
    return switch (operation.getType()) {
      case OP_CREATE_ACCOUNT -> new CreateOp(operation.getUserId());
      case OP_DELETE_ACCOUNT -> new DeleteOp(operation.getUserId());
      case OP_TRANSFER_TO -> new TransferOp(
          operation.getUserId(), operation.getDestUserId(), operation.getAmount());
      default -> throw Status.UNKNOWN.asRuntimeException();
    };
  }

  @Override
  public void propagateState(
      PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {
    try {
      this.state.resetLedger();
      for (DistLedgerCommonDefinitions.Operation operation : request.getState().getLedgerList()) {
        this.executor.execute(parseOperation(operation));
      }
      responseObserver.onNext(PropagateStateResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (RuntimeException e) {
      Logger.debug(PROPAGATE_FAILED + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }
}
