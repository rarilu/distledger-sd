package pt.tecnico.distledger.common;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.DeleteRequest;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.LookupRequest;
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

  private final String target;
  private final ManagedChannel channel;
  private final NamingServiceGrpc.NamingServiceBlockingStub stub;

  /** Creates a new NamingService, connecting to the given target. */
  public NamingService(String target) {
    this.target = target;

    Logger.debug("Connecting to naming service at " + this.target);

    this.channel = ManagedChannelBuilder.forTarget(this.target).usePlaintext().build();
    this.stub = NamingServiceGrpc.newBlockingStub(this.channel);
  }

  /** Creates a new NamingService, connecting to the well-known host and port. */
  public NamingService() {
    this(WELL_KNOWN_TARGET);
  }

  /** Executes a register request. */
  public void register(String service, String qualifier, String target) {
    RegisterRequest request =
        RegisterRequest.newBuilder()
            .setService(service)
            .setQualifier(qualifier)
            .setTarget(target)
            .build();

    Logger.debug("Register request: " + request);

    this.stub.register(request);
  }

  /** Executes a delete request. */
  public void delete(String service, String target) {
    DeleteRequest request =
        DeleteRequest.newBuilder().setService(service).setTarget(target).build();

    Logger.debug("Delete request: " + request);

    this.stub.delete(request);
  }

  /**
   * Executes a lookup request, searching by service and qualifier.
   *
   * @return a list of targets that match the given service and qualifier.
   */
  public List<String> lookup(String service, String qualifier) {
    LookupRequest request =
        LookupRequest.newBuilder().setService(service).setQualifier(qualifier).build();

    Logger.debug("Lookup request: " + request);

    return this.stub.lookup(request).getTargetsList();
  }

  /**
   * Executes a lookup request, searching by service.
   *
   * @return a list of targets that match the given service.
   */
  public List<String> lookup(String service) {
    // TODO
    throw new UnsupportedOperationException("Not implemented yet.");
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
