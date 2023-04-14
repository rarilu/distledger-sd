package pt.tecnico.distledger.server.grpc;

import java.util.Optional;
import java.util.function.Function;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.common.grpc.BaseService;
import pt.tecnico.distledger.common.grpc.NamingService;
import pt.tecnico.distledger.common.grpc.ProtoUtils;
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.tecnico.distledger.server.domain.Stamped;
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
   * @param qualifier the target server's qualifier.
   * @param generator the populated LedgerStateGenerator to propagate.
   * @param replicaTimeStamp the replica's vector clock.
   * @param ownId this server's ID.
   * @throws FailedPropagationException if the gRPC call fails.
   */
  private void propagateStateToServer(
      String qualifier, LedgerStateGenerator generator, VectorClock replicaTimeStamp, int ownId) {
    PropagateStateRequest request =
        PropagateStateRequest.newBuilder()
            .setState(generator.build())
            .setReplicaTS(ProtoUtils.toProto(replicaTimeStamp))
            .setId(ownId)
            .build();
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
   * Propagates (a subset of) the state to all other servers in this service.
   *
   * <p>If any individual propagation fails, the exception is logged and the propagation continues.
   *
   * @param generatorFactory a function that returns a {@link LedgerStateGenerator} for a given
   *     server entry. This can be used for the caller to control what operations the generator will
   *     visit, and, thus, what state will be propagated to each server. If {@code null} is
   *     returned, no propagation is attempted.
   * @param ownId this server's ID.
   * @throws io.grpc.StatusRuntimeException if a Naming Service lookup operation fails.
   */
  public void propagateState(
      Function<NamingService.Entry, Stamped<LedgerStateGenerator>> generatorFactory, int ownId) {
    this.stubCache.forEachServerInService(
        entry -> {
          if (entry.id() == ownId) {
            return;
          }

          try {
            Optional.ofNullable(generatorFactory.apply(entry))
                .ifPresent(
                    generator ->
                        this.propagateStateToServer(
                            entry.qualifier(), generator.value(), generator.timeStamp(), ownId));
          } catch (FailedPropagationException e) {
            Logger.error(
                "Failed to propagate state to server "
                    + entry.qualifier()
                    + " ("
                    + entry.target()
                    + "): "
                    + e.getMessage());
            // continue propagating to other servers; individual propagation failure is not critical
          }
        });
  }

  /**
   * Propagates an empty ledger state to all other servers in this service.
   *
   * <p>This is used so that the other servers become aware of this server's presence and can
   * invalidate any cache they may have of the replicated system's configuration.
   *
   * @param ownId this server's assigned ID
   */
  public void sendStartupBeacon(int ownId) {
    this.propagateState(
        entry -> new Stamped<>(new LedgerStateGenerator(), new VectorClock()), ownId);
  }

  /**
   * Informs that this server noticed another server's presence.
   *
   * <p>This must be called when appropriate to ensure caching is properly invalidated if necessary,
   * i.e., if the reported server is not known by the cache.
   *
   * @param id the other server's ID.
   */
  public void noticeServer(int id) {
    this.stubCache.noticeServer(id);
  }
}
