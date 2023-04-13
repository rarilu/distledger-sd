package pt.tecnico.distledger.server.grpc;

import java.util.Optional;
import java.util.function.Function;
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
   * @throws FailedPropagationException if the gRPC call fails.
   */
  private void propagateStateToServer(String qualifier, LedgerStateGenerator generator) {
    PropagateStateRequest request =
        PropagateStateRequest.newBuilder().setState(generator.build()).build();
    try {
      Logger.debug("Sending request: " + request);

      this.makeRequestWithRetryInvalidatingStubCache(
              qualifier,
              request,
              DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub
                  ::propagateState,
              MAX_TRIES)
          .orElseThrow(ServerUnavailableException::new);
    } catch (RuntimeException e) {
      throw new FailedPropagationException(e);
    }
  }

  /**
   * Propagates (a subset of) the state to all servers in this service.
   *
   * <p>If any individual propagation fails, the exception is logged and the propagation continues.
   *
   * @param generatorFactory a function that returns a {@link LedgerStateGenerator} for a given
   *     server entry. This can be used for the caller to control what operations the generator will
   *     visit, and, thus, what state will be propagated to each server. If {@code null} is
   *     returned, no propagation is attempted.
   * @throws io.grpc.StatusRuntimeException if a Naming Service lookup operation fails.
   */
  public void propagateState(Function<NamingService.Entry, LedgerStateGenerator> generatorFactory) {
    this.stubCache.forEachServerInService(
        entry -> {
          try {
            Optional.ofNullable(generatorFactory.apply(entry))
                .ifPresent(generator -> this.propagateStateToServer(entry.qualifier(), generator));
          } catch (FailedPropagationException e) {
            Logger.error(
                "Failed to propagate state to server " + entry.qualifier() + ": " + e.getMessage());
            // continue propagating to other servers; individual propagation failure is not critical
          }
        });
  }
}
