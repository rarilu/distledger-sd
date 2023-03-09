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

public class UserService implements AutoCloseable {
  private final ManagedChannel channel;
  private final UserServiceGrpc.UserServiceBlockingStub stub;

  public UserService(String host, int port) {
    final String target = host + ":" + port;
    Logger.debug("Connecting to " + target);

    this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    this.stub = UserServiceGrpc.newBlockingStub(this.channel);
  }

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

  public void balance(String server, String userId) {
    BalanceRequest request = BalanceRequest.newBuilder().setUserId(userId).build();
    this.makeRequest(request, this.stub::balance);
  }

  public void createAccount(String server, String userId) {
    CreateAccountRequest request = CreateAccountRequest.newBuilder().setUserId(userId).build();
    this.makeRequest(request, this.stub::createAccount);
  }

  public void deleteAccount(String server, String userId) {
    DeleteAccountRequest request = DeleteAccountRequest.newBuilder().setUserId(userId).build();
    this.makeRequest(request, this.stub::deleteAccount);
  }

  public void transferTo(String server, String from, String dest, int amount) {
    TransferToRequest request =
        TransferToRequest.newBuilder()
            .setAccountFrom(from)
            .setAccountTo(dest)
            .setAmount(amount)
            .build();
    this.makeRequest(request, this.stub::transferTo);
  }

  @Override
  public void close() {
    this.channel.shutdownNow();
  }
}
