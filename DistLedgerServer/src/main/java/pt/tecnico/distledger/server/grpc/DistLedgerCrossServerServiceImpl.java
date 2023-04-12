package pt.tecnico.distledger.server.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateResponse;
import pt.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.visitors.OperationExecutor;

/** Implements the CrossServer service, handling gRPC requests. */
public class DistLedgerCrossServerServiceImpl
    extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {
  private static final String PROPAGATE_FAILED = "Propagate State failed: ";
  private static final String PARSE_FAILED = "Failed to create operation from request";

  private final AtomicBoolean active;
  private final OperationExecutor executor;

  /**
   * Creates a new DistLedgerCrossServerServiceImpl.
   *
   * @param state The server state
   * @param active This server's active flag
   */
  public DistLedgerCrossServerServiceImpl(ServerState state, AtomicBoolean active) {
    this.active = active;
    this.executor = new OperationExecutor(state);
  }

  private Operation parseOperation(DistLedgerCommonDefinitions.Operation operation) {
    return switch (operation.getType()) {
      case OP_CREATE_ACCOUNT -> new CreateOp(operation.getUserId());
      case OP_TRANSFER_TO -> new TransferOp(
          operation.getUserId(), operation.getDestUserId(), operation.getAmount());
      default -> throw new IllegalArgumentException(PARSE_FAILED);
    };
  }

  @Override
  public void propagateState(
      PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {
    Logger.debug("Received PropagateState request");
    Logger.debug(request + "\n");

    try {
      if (!active.get()) {
        throw new ServerUnavailableException();
      }

      // First we parse the operations from the request, to ensure we don't modify the state if
      // the request is invalid.
      List<Operation> operations =
          request.getState().getLedgerList().stream().map(this::parseOperation).toList();

      // Safety: the operations are executed and stored in the ledger in the same order they were
      // received. Since the A server always waits for a response before continuing, we don't
      // need to worry about concurrent calls to propagateState.
      operations.forEach(op -> op.accept(executor));

      responseObserver.onNext(PropagateStateResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (ServerUnavailableException e) {
      Logger.debug(PROPAGATE_FAILED + e.getMessage());
      responseObserver.onError(
          Status.UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
    } catch (IllegalArgumentException e) {
      Logger.debug(PROPAGATE_FAILED + e.getMessage());
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
    } catch (RuntimeException e) {
      Logger.debug(PROPAGATE_FAILED + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }
}
