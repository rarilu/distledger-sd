package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.operation.Operation;

/**
 * Manages the ledger state, abstracting away the necessary logic to keep it consistent between
 * replicas.
 */
public interface LedgerManager {
  /** Register a given operation in the ledger. */
  void addToLedger(Operation operation);
}
