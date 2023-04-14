package pt.tecnico.distledger.server.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateResponse;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GossipRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GossipResponse;
import pt.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.visitors.LedgerStateGenerator;

/** Implements the Admin service, handling gRPC requests. */
public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {
  private static final String ACTIVATE_FAILED = "Activate failed: ";
  private static final String GOSSIP_FAILED = "Gossip failed: ";
  private static final String DEACTIVATE_FAILED = "Deactivate failed: ";
  private static final String GET_LEDGER_STATE_FAILED = "Get Ledger State failed: ";

  private final ServerState state;
  private final AtomicBoolean active;
  private final CrossServerService crossServerService;

  /**
   * Makes use of the VectorClock structure and functionality to keep track of the ledger indices of
   * the stable operations which were sent to each server in the last gossip. Unstable operations
   * are not tracked and will always be sent in each gossip.
   */
  private final VectorClock lastIndicesGossiped = new VectorClock();

  /** Creates a new Admin service. */
  public AdminServiceImpl(
      ServerState state, AtomicBoolean active, CrossServerService crossServerService) {
    this.state = state;
    this.active = active;
    this.crossServerService = crossServerService;
  }

  @Override
  public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {
    Logger.debug("Received Activate request:");
    Logger.debug(request + "\n");

    try {
      this.active.set(true);
      responseObserver.onNext(ActivateResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (RuntimeException e) {
      Logger.debug(ACTIVATE_FAILED + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver) {
    Logger.debug("Received Gossip request:");
    Logger.debug(request + "\n");

    try {
      synchronized (this.lastIndicesGossiped) {
        this.crossServerService.propagateState(
            server -> {
              LedgerStateGenerator generator = new LedgerStateGenerator();
              this.state
                  .visitLedger(generator, this.lastIndicesGossiped.get(server.id()))
                  .ifPresent(
                      lastIndexVisited ->
                          this.lastIndicesGossiped.set(server.id(), lastIndexVisited));
              return generator;
            },
            this.state.getId());
      }

      responseObserver.onNext(GossipResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (RuntimeException e) {
      Logger.debug(GOSSIP_FAILED + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void deactivate(
      DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {
    Logger.debug("Received Deactivate request:");
    Logger.debug(request + "\n");

    try {
      this.active.set(false);
      responseObserver.onNext(DeactivateResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (RuntimeException e) {
      Logger.debug(DEACTIVATE_FAILED + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void getLedgerState(
      GetLedgerStateRequest request, StreamObserver<GetLedgerStateResponse> responseObserver) {
    Logger.debug("Received GetLedgerState request:");
    Logger.debug(request + "\n");

    try {
      LedgerStateGenerator generator = new LedgerStateGenerator();
      this.state.visitLedger(generator);

      GetLedgerStateResponse response =
          GetLedgerStateResponse.newBuilder().setLedgerState(generator.build()).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (RuntimeException e) {
      Logger.debug(GET_LEDGER_STATE_FAILED + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }
}
