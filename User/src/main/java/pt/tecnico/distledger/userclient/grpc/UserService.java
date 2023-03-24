package pt.tecnico.distledger.userclient.grpc;

import java.util.Optional;
import pt.tecnico.distledger.common.grpc.BaseService;
import pt.tecnico.distledger.common.grpc.NamingService;
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountResponse;
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;
import pt.tecnico.distledger.contract.user.UserServiceGrpc;

/** Handles User operations, making gRPC requests to the server's User service. */
public class UserService extends BaseService<UserServiceGrpc.UserServiceBlockingStub> {
  private static final int MAX_TRIES = 2;

  /** Creates a new UserService, connecting to the given host and port. */
  public UserService(NamingService namingService) {
    super(namingService, UserServiceGrpc::newBlockingStub);
  }

  /** Handle the Balance command. */
  public Optional<String> balance(String server, String userId) {
    BalanceRequest request = BalanceRequest.newBuilder().setUserId(userId).build();
    return this.makeRequestWithRetryInvalidatingStubCache(
            server, request, UserServiceGrpc.UserServiceBlockingStub::balance, MAX_TRIES)
        .map(BalanceResponse::toString);
  }

  /** Handle the Create Account command. */
  public Optional<String> createAccount(String server, String userId) {
    CreateAccountRequest request = CreateAccountRequest.newBuilder().setUserId(userId).build();
    return this.makeRequestWithRetryInvalidatingStubCache(
            server, request, UserServiceGrpc.UserServiceBlockingStub::createAccount, MAX_TRIES)
        .map(CreateAccountResponse::toString);
  }

  /** Handle the Delete Account command. */
  public Optional<String> deleteAccount(String server, String userId) {
    DeleteAccountRequest request = DeleteAccountRequest.newBuilder().setUserId(userId).build();
    return this.makeRequestWithRetryInvalidatingStubCache(
            server, request, UserServiceGrpc.UserServiceBlockingStub::deleteAccount, MAX_TRIES)
        .map(DeleteAccountResponse::toString);
  }

  /** Handle the Transfer To command. */
  public Optional<String> transferTo(String server, String from, String dest, int amount) {
    TransferToRequest request =
        TransferToRequest.newBuilder()
            .setAccountFrom(from)
            .setAccountTo(dest)
            .setAmount(amount)
            .build();
    return this.makeRequestWithRetryInvalidatingStubCache(
            server, request, UserServiceGrpc.UserServiceBlockingStub::transferTo, MAX_TRIES)
        .map(TransferToResponse::toString);
  }
}
