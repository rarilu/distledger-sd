package pt.tecnico.distledger.server.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateResponse;
import pt.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.ServerUnavailableException;
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
  private static final String PARSE_FAILED = "Failed to create operation from request";

  private final ServerState state;
  private final AtomicBoolean active;
  private final OperationExecutor executor;

  /**
   * Creates a new DistLedgerCrossServerServiceImpl.
   *
   * @param state The server state
   * @param active This server's active flag
   */
  public DistLedgerCrossServerServiceImpl(ServerState state, AtomicBoolean active) {
    this.state = state;
    this.active = active;
    this.executor = new StandardOperationExecutor(state);
  }

  private Operation parseOperation(DistLedgerCommonDefinitions.Operation operation) {
    return switch (operation.getType()) {
      case OP_CREATE_ACCOUNT -> new CreateOp(operation.getUserId());
      case OP_DELETE_ACCOUNT -> new DeleteOp(operation.getUserId());
      case OP_TRANSFER_TO -> new TransferOp(
          operation.getUserId(), operation.getDestUserId(), operation.getAmount());
      default -> throw new IllegalArgumentException(PARSE_FAILED);
    };
  }

  @Override
  public void propagateState(
      PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {
    try {
      if (!active.get()) {
        throw new ServerUnavailableException();
      }
      List<Operation> operations =
          request.getState().getLedgerList().stream()
              .map(this::parseOperation)
              .collect(Collectors.toList());

      // Safety: Secondary servers only execute operations given by the primary server on
      // propagateStateRequests.
      // As such, there are no concurency issues if the state is locked here.
      synchronized (this.state) {
        this.state.reset();
        operations.stream().forEach(op -> op.accept(executor));
      }

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
