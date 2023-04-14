package pt.tecnico.distledger.server.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.grpc.ProtoUtils;
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateResponse;
import pt.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

/** Implements the CrossServer service, handling gRPC requests. */
public class DistLedgerCrossServerServiceImpl
    extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {
  private static final String PROPAGATE_FAILED = "Propagate State failed: ";
  private static final String PARSE_FAILED = "Failed to create operation from request";

  private final AtomicBoolean active;
  private final ServerState state;
  private CrossServerService crossServerService;

  /**
   * Creates a new DistLedgerCrossServerServiceImpl.
   *
   * @param state The server state
   * @param active This server's active flag
   * @param crossServerService The server's cross server service instance
   */
  public DistLedgerCrossServerServiceImpl(
      ServerState state, AtomicBoolean active, CrossServerService crossServerService) {
    this.state = state;
    this.active = active;
    this.crossServerService = crossServerService;
  }

  private Operation parseOperation(DistLedgerCommonDefinitions.Operation operation) {
    Operation op = switch (operation.getType()) {
      case OP_CREATE_ACCOUNT -> new CreateOp(
          operation.getUserId(),
          ProtoUtils.fromProto(operation.getPrevTS()),
          ProtoUtils.fromProto(operation.getReplicaTS()),
          operation.getReplicaId());
      case OP_TRANSFER_TO -> new TransferOp(
          operation.getUserId(),
          operation.getDestUserId(),
          operation.getAmount(),
          ProtoUtils.fromProto(operation.getPrevTS()),
          ProtoUtils.fromProto(operation.getReplicaTS()),
          operation.getReplicaId());
      default -> throw new IllegalArgumentException(PARSE_FAILED);
    };

    // May have failed already on the other server
    if (operation.getFailed()) {
      op.setFailed();
    }

    return op;
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

      this.crossServerService.noticeServer(request.getId());

      // First we parse the operations from the request, to ensure we don't modify the state if
      // the request is invalid.
      List<Operation> operations =
          request.getState().getLedgerList().stream().map(this::parseOperation).toList();

      // Then, we add the operations to the ledger.
      if (this.state.addToLedger(operations, ProtoUtils.fromProto(request.getReplicaTS()))) {
        // If any operation was stabilized, other operations may be able to be stabilized as well.
        this.state.stabilize();
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
