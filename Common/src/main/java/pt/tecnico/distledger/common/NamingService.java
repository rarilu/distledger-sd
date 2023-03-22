package pt.tecnico.distledger.common;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.DeleteRequest;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.RegisterRequest;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ShutdownRequest;
import pt.tecnico.distledger.contract.namingserver.NamingServiceGrpc;

/**
 * Handles name register, delete and lookup operations, making gRPC requests to the naming server's
 * Naming Service.
 */
public class NamingService implements AutoCloseable {
  /** Target host and port for the well-known naming server. */
  private static final String WELL_KNOWN_TARGET = "localhost:5001";

  private final ManagedChannel channel;
  private final NamingServiceGrpc.NamingServiceBlockingStub stub;

  /** Creates a new NamingService, connecting to the well-known host and port. */
  public NamingService() {
    Logger.debug("Connecting to naming service at " + WELL_KNOWN_TARGET);
    this.channel = ManagedChannelBuilder.forTarget(WELL_KNOWN_TARGET).usePlaintext().build();
    this.stub = NamingServiceGrpc.newBlockingStub(this.channel);
  }

  /** Executes a register request. */
  public void register(String service, String qualifier, String target) {
    RegisterRequest request =
        RegisterRequest.newBuilder()
            .setService(service)
            .setQualifier(qualifier)
            .setTarget(target)
            .build();

    this.stub.register(request);
  }

  /** Executes a delete request. */
  public void delete(String service, String target) {
    DeleteRequest request =
        DeleteRequest.newBuilder().setService(service).setTarget(target).build();

    this.stub.delete(request);
  }

  /** Executes a lookup request. */
  public void lookup(String service, String qualifier) {
    // TODO
  }

  /** Executes a shutdown request. */
  public void shutdown() {
    ShutdownRequest request = ShutdownRequest.getDefaultInstance();

    this.stub.shutdown(request);
  }

  /** Close channel immediately. */
  @Override
  public void close() {
    this.channel.shutdownNow();
  }
}
