package pt.tecnico.distledger.adminclient.grpc;

import pt.tecnico.distledger.common.NamingService;
import pt.tecnico.distledger.common.grpc.ClientService;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateRequest;
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ShutdownRequest;
import pt.tecnico.distledger.contract.admin.AdminServiceGrpc;

/** Handles Admin operations, making gRPC requests to the server's Admin service. */
public class AdminService extends ClientService<AdminServiceGrpc.AdminServiceBlockingStub> {
  private static final int MAX_TRIES = 2;

  public AdminService(NamingService namingService) {
    super(namingService, AdminServiceGrpc::newBlockingStub);
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
}
