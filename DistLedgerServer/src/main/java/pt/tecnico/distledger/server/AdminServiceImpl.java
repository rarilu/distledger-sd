package pt.tecnico.distledger.server;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.visitors.LedgerStateGenerator;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {
  private ServerState state;

  public AdminServiceImpl(ServerState state) {
    this.state = state;
  }

  @Override
  public void getLedgerState(
      getLedgerStateRequest request, StreamObserver<getLedgerStateResponse> responseObserver) {
    LedgerStateGenerator generator = new LedgerStateGenerator();

    this.state.visitLedger(generator);

    getLedgerStateResponse response =
        getLedgerStateResponse.newBuilder().setLedgerState(generator.build()).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
