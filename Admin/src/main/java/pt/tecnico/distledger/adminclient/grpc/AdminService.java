package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.StatusRuntimeException;
import java.util.Objects;
import java.util.function.BiFunction;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.NamingService;
import pt.tecnico.distledger.common.StubCache;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ShutdownRequest;
import pt.tecnico.distledger.contract.admin.AdminServiceGrpc;

/** Handles Admin operations, making gRPC requests to the server's Admin service. */
public class AdminService implements AutoCloseable {
  private static final int MAX_TRIES = 2;

  private final StubCache<AdminServiceGrpc.AdminServiceBlockingStub> stubCache;

  public AdminService(NamingService namingService) {
    this.stubCache = new StubCache<>(namingService, AdminServiceGrpc::newBlockingStub);
  }

  /**
   * Dispatches requests to the server, showing the response to the user.
   *
   * @param qualifier the server's qualifier.
   * @param request the request to be sent.
   * @param dispatcher the function that dispatches the request to the server. It receives the stub
   *     and the request, and returns the response.
   * @param <Q> the request type.
   * @param <R> the response type.
   * @return false if the server is unavailable (to allow retrying), true otherwise.
   */
  private <Q, R> boolean makeRequest(
      String qualifier,
      Q request,
      BiFunction<AdminServiceGrpc.AdminServiceBlockingStub, Q, R> dispatcher) {
    try {
      AdminServiceGrpc.AdminServiceBlockingStub stub = this.stubCache.getStub(qualifier);

      Logger.debug("Sending request: " + request.toString());
      R response = dispatcher.apply(stub, request);
      String representation = response.toString();

      System.out.println("OK");
      System.out.println(representation);
    } catch (StatusRuntimeException e) {
      if (Objects.equals(e.getStatus().getCode(), io.grpc.Status.Code.UNAVAILABLE)) {
        return false;
      }

      System.out.println("Error: " + e.getStatus().getDescription());
      System.out.println();
    } catch (RuntimeException e) {
      // This happens when no server is found with the specified qualifier
      System.out.println("Error: " + e.getMessage());
      System.out.println();
    }

    return true;
  }

  /**
   * Dispatches requests to the server, showing the response to the user, retrying if necessary when
   * the server is unavailable.
   *
   * @param qualifier the server's qualifier.
   * @param request the request to be sent.
   * @param dispatcher the function that dispatches the request to the server. It receives the stub
   *     and the request, and returns the response.
   * @param maxTries the maximum number of tries to be performed.
   * @param <Q> the request type.
   * @param <R> the response type.
   */
  private <Q, R> void makeRequestWithRetryInvalidatingStubCache(
      String qualifier,
      Q request,
      BiFunction<AdminServiceGrpc.AdminServiceBlockingStub, Q, R> dispatcher,
      int maxTries) {
    for (int i = 0; i < maxTries; i++) {
      if (this.makeRequest(qualifier, request, dispatcher)) {
        return;
      }

      this.stubCache.invalidateCachedStub(qualifier);
    }

    System.out.println("Error: Server is unavailable.");
    System.out.println();
  }

  /** Handle the Activate command. */
  public void activate(String server) {
    ActivateRequest request = ActivateRequest.getDefaultInstance();
    this.makeRequestWithRetryInvalidatingStubCache(
        server, request, AdminServiceGrpc.AdminServiceBlockingStub::activate, MAX_TRIES);
  }

  /** Handle the Deactivate command. */
  public void deactivate(String server) {
    DeactivateRequest request = DeactivateRequest.getDefaultInstance();
    this.makeRequestWithRetryInvalidatingStubCache(
        server, request, AdminServiceGrpc.AdminServiceBlockingStub::deactivate, MAX_TRIES);
  }

  /** Handle the Get Ledger State command. */
  public void getLedgerState(String server) {
    GetLedgerStateRequest request = GetLedgerStateRequest.getDefaultInstance();
    this.makeRequestWithRetryInvalidatingStubCache(
        server, request, AdminServiceGrpc.AdminServiceBlockingStub::getLedgerState, MAX_TRIES);
  }

  /** Handle the Shutdown command. */
  public void shutdown(String server) {
    ShutdownRequest request = ShutdownRequest.getDefaultInstance();
    this.makeRequestWithRetryInvalidatingStubCache(
        server, request, AdminServiceGrpc.AdminServiceBlockingStub::shutdown, MAX_TRIES);
  }

  /** Close channel immediately. */
  @Override
  public void close() {
    this.stubCache.close();
  }
}
