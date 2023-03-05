package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.function.Function;
import pt.tecnico.distledger.utils.Logger;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

public class UserService implements AutoCloseable {
  ManagedChannel channel;
  UserServiceGrpc.UserServiceBlockingStub stub;

  public UserService(String host, int port) {
    final String target = host + ":" + port;
    Logger.debug("Connecting to " + target);

    this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    this.stub = UserServiceGrpc.newBlockingStub(channel);
  }

  private <Request, Response> void makeRequest(
      Request request, Function<Request, Response> stubMethod) {
    try {
      Logger.debug("Sending request: " + request.toString());
      Response response = stubMethod.apply(request);
      String representation = response.toString();

      System.out.println("OK");
      System.out.println(representation);
      if (!representation.isEmpty()) {
        System.out.println();
      }
    } catch (StatusRuntimeException e) {
      System.out.println("Error: " + e.getStatus().getDescription());
    }
  }

  public void createAccount(String server, String userId) {
    CreateAccountRequest request = CreateAccountRequest.newBuilder().setUserId(userId).build();

    this.makeRequest(request, stub::createAccount);
  }

  // TODO: remote operation methods

  @Override
  public void close() {
    this.channel.shutdownNow();
  }
}
