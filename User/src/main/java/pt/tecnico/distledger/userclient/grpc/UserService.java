package pt.tecnico.distledger.userclient.grpc;

import java.util.Optional;
import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.common.grpc.BaseService;
import pt.tecnico.distledger.common.grpc.NamingService;
import pt.tecnico.distledger.common.grpc.ProtoUtils;
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.tecnico.distledger.contract.user.UserServiceGrpc;

/** Handles User operations, making gRPC requests to the server's User service. */
public class UserService extends BaseService<UserServiceGrpc.UserServiceBlockingStub> {
  private static final int MAX_TRIES = 2;
  private final VectorClock prevTimeStamp = new VectorClock();

  public UserService(NamingService namingService) {
    super(namingService, UserServiceGrpc::newBlockingStub);
  }

  /** Handle the Balance command. */
  public Optional<String> balance(String server, String userId) {
    BalanceRequest request =
        BalanceRequest.newBuilder()
            .setUserId(userId)
            .setPrevTS(ProtoUtils.toProto(this.prevTimeStamp))
            .build();

    return this.makeRequestWithRetryInvalidatingStubCache(
            server, request, UserServiceGrpc.UserServiceBlockingStub::balance, MAX_TRIES)
        .map(BalanceResponse::getValue)
        .map(value -> "value: " + value + "\n");
  }

  /** Handle the Create Account command. */
  public Optional<String> createAccount(String server, String userId) {
    CreateAccountRequest request =
        CreateAccountRequest.newBuilder()
            .setUserId(userId)
            .setPrevTS(ProtoUtils.toProto(this.prevTimeStamp))
            .build();

    if (this.makeRequestWithRetryInvalidatingStubCache(
            server, request, UserServiceGrpc.UserServiceBlockingStub::createAccount, MAX_TRIES)
        .isPresent()) {
      return Optional.of("");
    }

    return Optional.empty();
  }

  /** Handle the Transfer To command. */
  public Optional<String> transferTo(String server, String from, String dest, int amount) {
    TransferToRequest request =
        TransferToRequest.newBuilder()
            .setAccountFrom(from)
            .setAccountTo(dest)
            .setAmount(amount)
            .setPrevTS(ProtoUtils.toProto(this.prevTimeStamp))
            .build();

    if (this.makeRequestWithRetryInvalidatingStubCache(
            server, request, UserServiceGrpc.UserServiceBlockingStub::transferTo, MAX_TRIES)
        .isPresent()) {
      return Optional.of("");
    }

    return Optional.empty();
  }
}
