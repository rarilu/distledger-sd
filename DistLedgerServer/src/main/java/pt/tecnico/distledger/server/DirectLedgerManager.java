package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;

/** Implementation of LedgerManager that directly modifies the server state. */
public class DirectLedgerManager implements LedgerManager {
  private final ServerState state;

  public DirectLedgerManager(ServerState state) {
    this.state = state;
  }

  @Override
  public void addToLedger(Operation operation) {
    this.state.addToLedger(operation);
  }
}
