package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.grpc.CrossServerService;
import pt.tecnico.distledger.server.visitors.LedgerStateGenerator;

/**
 * Implementation of LedgerManager that first replicates the operation to secondary servers and only
 * then modifies the server state.
 */
public class ReplicatingLedgerManager implements LedgerManager {
  private static final String SECONDARY_QUALIFIER = "B";

  private final CrossServerService crossServerService;
  private final ServerState state;

  public ReplicatingLedgerManager(CrossServerService crossServerService, ServerState state) {
    this.crossServerService = crossServerService;
    this.state = state;
  }

  @Override
  public void addToLedger(Operation operation) {
    LedgerStateGenerator generator = new LedgerStateGenerator();
    this.state.addToLedgerAndVisit(operation, generator);
    this.crossServerService.propagateState(SECONDARY_QUALIFIER, generator);
  }
}
