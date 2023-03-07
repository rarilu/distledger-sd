package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.function.Function;
import pt.tecnico.distledger.utils.Logger;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

public class UserService implements AutoCloseable {
  ManagedChannel channel;
  UserServiceGrpc.UserServiceBlockingStub stub;

  public UserService(String host, int port) {
    final String target = host + ":" + port;
    Logger.debug("Connecting to " + target);

    this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    this.stub = UserServiceGrpc.newBlockingStub(this.channel);
  }

  private <RequestT, ResponseT> void makeRequest(
      RequestT request, Function<RequestT, ResponseT> stubMethod) {
    try {
      Logger.debug("Sending request: " + request.toString());
      ResponseT response = stubMethod.apply(request);
      String representation = response.toString();

      System.out.println("OK");
      System.out.println(representation);
      if (!representation.isEmpty()) {
        System.out.println();
      }
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
