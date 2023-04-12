package pt.tecnico.distledger.server.grpc;

import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.grpc.BaseService;
import pt.tecnico.distledger.common.grpc.NamingService;
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.tecnico.distledger.server.domain.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.server.grpc.exceptions.FailedPropagationException;
import pt.tecnico.distledger.server.visitors.LedgerStateGenerator;

/** Handles CrossServer operations, making gRPC requests to the server's CrossServer service. */
public class CrossServerService
    extends BaseService<DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub> {
  private static final int MAX_TRIES = 2;

  /** Creates a new CrossServerService using the given NamingService. */
  public CrossServerService(NamingService service) {
    super(service, DistLedgerCrossServerServiceGrpc::newBlockingStub);
  }

  /**
   * Handle the PropagateState request. Uses the filled LedgerStateGenerator to build the
   * LedgerState proto and send it to the server.
   *
   * @throws FailedPropagationException if the gRPC call fails
   */
  public void propagateState(String server, LedgerStateGenerator generator) {
    PropagateStateRequest request =
        PropagateStateRequest.newBuilder().setState(generator.build()).build();
    try {
      Logger.debug("Sending request: " + request);

      this.makeRequestWithRetryInvalidatingStubCache(
              server,
              request,
              DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub
                  ::propagateState,
              MAX_TRIES)
          .orElseThrow(ServerUnavailableException::new);
    } catch (RuntimeException e) {
      throw new FailedPropagationException(e);
    }
  }
}
