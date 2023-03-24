package pt.tecnico.distledger.adminclient.grpc;

import java.util.Optional;
import pt.tecnico.distledger.common.NamingService;
import pt.tecnico.distledger.common.grpc.BaseService;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateResponse;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ShutdownRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ShutdownResponse;
import pt.tecnico.distledger.contract.admin.AdminServiceGrpc;

/** Handles Admin operations, making gRPC requests to the server's Admin service. */
public class AdminService extends BaseService<AdminServiceGrpc.AdminServiceBlockingStub> {
  private static final int MAX_TRIES = 2;

  public AdminService(NamingService namingService) {
    super(namingService, AdminServiceGrpc::newBlockingStub);
  }

  /** Handle the Activate command. */
  public Optional<String> activate(String server) {
    ActivateRequest request = ActivateRequest.getDefaultInstance();
    return this.makeRequestWithRetryInvalidatingStubCache(
            server, request, AdminServiceGrpc.AdminServiceBlockingStub::activate, MAX_TRIES)
        .map(ActivateResponse::toString);
  }

  /** Handle the Deactivate command. */
  public Optional<String> deactivate(String server) {
    DeactivateRequest request = DeactivateRequest.getDefaultInstance();
    return this.makeRequestWithRetryInvalidatingStubCache(
            server, request, AdminServiceGrpc.AdminServiceBlockingStub::deactivate, MAX_TRIES)
        .map(DeactivateResponse::toString);
  }

  /** Handle the Get Ledger State command. */
  public Optional<String> getLedgerState(String server) {
    GetLedgerStateRequest request = GetLedgerStateRequest.getDefaultInstance();
    return this.makeRequestWithRetryInvalidatingStubCache(
            server, request, AdminServiceGrpc.AdminServiceBlockingStub::getLedgerState, MAX_TRIES)
        .map(GetLedgerStateResponse::toString);
  }

  /** Handle the Shutdown command. */
  public Optional<String> shutdown(String server) {
    ShutdownRequest request = ShutdownRequest.getDefaultInstance();
    return this.makeRequestWithRetryInvalidatingStubCache(
            server, request, AdminServiceGrpc.AdminServiceBlockingStub::shutdown, MAX_TRIES)
        .map(ShutdownResponse::toString);
  }
}
