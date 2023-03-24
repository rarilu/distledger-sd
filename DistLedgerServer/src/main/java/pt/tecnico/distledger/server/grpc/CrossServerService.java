package pt.tecnico.distledger.server.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.tecnico.distledger.server.domain.exceptions.FailedPropagationException;
import pt.tecnico.distledger.server.visitors.LedgerStateGenerator;

/** Handles CrossServer operations, making gRPC requests to the server's CrossServer service. */
public class CrossServerService implements AutoCloseable {
  private final ManagedChannel channel;
  private final DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub;

  /** Creates a new CrossServerService, connecting to the given host and port. */
  public CrossServerService(String host, int port) {
    final String target = host + ":" + port;
    Logger.debug("Connecting to " + target);

    this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    this.stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(this.channel);
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
      Logger.debug("Sending request: " + request.toString());
      this.stub.propagateState(request);
    } catch (StatusRuntimeException e) {
      throw new FailedPropagationException(e);
    }
  }

  /** Close channel immediately. */
  @Override
  public void close() {
    this.channel.shutdownNow();
  }
}
