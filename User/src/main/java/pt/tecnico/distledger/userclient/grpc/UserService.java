package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.distledger.utils.Logger;
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

  // TODO: remote operation methods

  @Override
  public void close() {
    this.channel.shutdownNow();
  }
}
