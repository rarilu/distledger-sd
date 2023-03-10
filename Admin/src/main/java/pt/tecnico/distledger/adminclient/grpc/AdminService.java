package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.function.Function;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateRequest;
import pt.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.tecnico.distledger.utils.Logger;

/** Handles Admin operations, making gRPC requests to the server's Admin service. */
public class AdminService implements AutoCloseable {
  private final ManagedChannel channel;
  private final AdminServiceGrpc.AdminServiceBlockingStub stub;

  /** Creates a new AdminService, connecting to the given host and port. */
  public AdminService(String host, int port) {
    final String target = host + ":" + port;
    Logger.debug("Connecting to " + target);

    this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    this.stub = AdminServiceGrpc.newBlockingStub(this.channel);
  }

  /** Dispatches requests to the server, showing the response to the user. */
  private <Q, R> void makeRequest(Q request, Function<Q, R> stubMethod) {
    try {
      Logger.debug("Sending request: " + request.toString());
      R response = stubMethod.apply(request);
      String representation = response.toString();

      System.out.println("OK");
      System.out.println(representation);
    } catch (StatusRuntimeException e) {
      System.out.println("Error: " + e.getStatus().getDescription());
      System.out.println();
    }
  }

  /** Handle the Activate command. */
  public void activate(String server) {
    ActivateRequest request = ActivateRequest.getDefaultInstance();
    this.makeRequest(request, this.stub::activate);
  }

  /** Handle the Deactivate command. */
  public void deactivate(String server) {
    DeactivateRequest request = DeactivateRequest.getDefaultInstance();
    this.makeRequest(request, this.stub::deactivate);
  }

  /** Handle the Get Ledger State command. */
  public void getLedgerState(String server) {
    GetLedgerStateRequest request = GetLedgerStateRequest.getDefaultInstance();
    this.makeRequest(request, this.stub::getLedgerState);
  }

  /** Close channel immediately. */
  @Override
  public void close() {
    this.channel.shutdownNow();
  }
}
