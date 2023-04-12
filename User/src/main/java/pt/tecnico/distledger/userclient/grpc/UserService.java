package pt.tecnico.distledger.userclient.grpc;

import java.util.Optional;
import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.common.grpc.BaseService;
import pt.tecnico.distledger.common.grpc.NamingService;
import pt.tecnico.distledger.common.grpc.ProtoUtils;
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;
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

    Optional<BalanceResponse> response =
        this.makeRequestWithRetryInvalidatingStubCache(
            server, request, UserServiceGrpc.UserServiceBlockingStub::balance, MAX_TRIES);

    response
        .map(BalanceResponse::getValueTS)
        .map(ProtoUtils::fromProto)
        .ifPresent(this.prevTimeStamp::merge);

    return response.map(BalanceResponse::getValue).map(value -> "value: " + value + "\n");
  }

  /** Handle the Create Account command. */
  public Optional<String> createAccount(String server, String userId) {
    CreateAccountRequest request =
        CreateAccountRequest.newBuilder()
            .setUserId(userId)
            .setPrevTS(ProtoUtils.toProto(this.prevTimeStamp))
            .build();

    Optional<CreateAccountResponse> response =
        this.makeRequestWithRetryInvalidatingStubCache(
            server, request, UserServiceGrpc.UserServiceBlockingStub::createAccount, MAX_TRIES);

    response
        .map(CreateAccountResponse::getValueTS)
        .map(ProtoUtils::fromProto)
        .ifPresent(this.prevTimeStamp::merge);

    return response.isPresent() ? Optional.of("") : Optional.empty();
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

    Optional<TransferToResponse> response =
        this.makeRequestWithRetryInvalidatingStubCache(
            server, request, UserServiceGrpc.UserServiceBlockingStub::transferTo, MAX_TRIES);

    response
        .map(TransferToResponse::getValueTS)
        .map(ProtoUtils::fromProto)
        .ifPresent(this.prevTimeStamp::merge);

    return response.isPresent() ? Optional.of("") : Optional.empty();
  }

  /** Handle the Timestamp command. */
  public Optional<String> timestamp() {
    return Optional.of("timestamp: " + this.prevTimeStamp.toString() + "\n");
  }
}
