package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.function.Function;
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.tecnico.distledger.utils.Logger;

/** Handles User operations, making gRPC requests to the server's User service. */
public class UserService implements AutoCloseable {
  private final ManagedChannel channel;
  private final UserServiceGrpc.UserServiceBlockingStub stub;

  /** Creates a new UserService, connecting to the given host and port. */
  public UserService(String host, int port) {
    final String target = host + ":" + port;
    Logger.debug("Connecting to " + target);

    this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    this.stub = UserServiceGrpc.newBlockingStub(this.channel);
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

  /** Handle the Balance command. */
  public void balance(String server, String userId) {
    BalanceRequest request = BalanceRequest.newBuilder().setUserId(userId).build();
    this.makeRequest(request, this.stub::balance);
  }

  /** Handle the Create Account command. */
  public void createAccount(String server, String userId) {
    CreateAccountRequest request = CreateAccountRequest.newBuilder().setUserId(userId).build();
    this.makeRequest(request, this.stub::createAccount);
  }

  /** Handle the Delete Account command. */
  public void deleteAccount(String server, String userId) {
    DeleteAccountRequest request = DeleteAccountRequest.newBuilder().setUserId(userId).build();
    this.makeRequest(request, this.stub::deleteAccount);
  }

  /** Handle the Transfer To command. */
  public void transferTo(String server, String from, String dest, int amount) {
    TransferToRequest request =
        TransferToRequest.newBuilder()
            .setAccountFrom(from)
            .setAccountTo(dest)
            .setAmount(amount)
            .build();
    this.makeRequest(request, this.stub::transferTo);
  }

  /** Close channel immediately. */
  @Override
  public void close() {
    this.channel.shutdownNow();
  }
}
