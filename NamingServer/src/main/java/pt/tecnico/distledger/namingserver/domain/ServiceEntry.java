package pt.tecnico.distledger.namingserver.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import pt.tecnico.distledger.namingserver.domain.exceptions.DuplicateServerEntryException;
import pt.tecnico.distledger.namingserver.domain.exceptions.ServerEntryNotFoundException;

/** Represents a service entry in the naming server. */
public class ServiceEntry {
  private final String name;
  private final ConcurrentMap<String, ServerEntry> servers = new ConcurrentHashMap<>();
  private final Set<String> targets = ConcurrentHashMap.newKeySet();
  private final AtomicInteger nextId = new AtomicInteger(0);

  public ServiceEntry(String name) {
    this.name = name;
  }

  /**
   * Registers a server in the service entry.
   *
   * @return a newly-assigned server ID, within this service.
   */
  public int registerServer(String qualifier, String target) {
    // Add the target to the set of registered targets, and throw an exception if it was already
    // present
    if (!this.targets.add(target)) {
      throw new DuplicateServerEntryException(this.name, qualifier, target);
    }

    int id = this.nextId.getAndIncrement();
    if (this.servers.putIfAbsent(qualifier, new ServerEntry(qualifier, target, id)) != null) {
      // An entry is already present, do not override it
      throw new DuplicateServerEntryException(this.name, qualifier, target);
    }

    return id;
  }

  /** Deletes a server from the service entry. */
  public void deleteServer(String target) {
    if (this.servers.values().removeIf(serverEntry -> target.equals(serverEntry.target()))) {
      this.targets.remove(target); // Remove the target from the set of registered targets
      return;
    }

    // If we reach this point, the server was not found
    throw new ServerEntryNotFoundException(this.name, target);
  }

  /** Looks up servers with the given qualifier in the service entry. */
  public Optional<ServerEntry> lookup(String qualifier) {
    return Optional.ofNullable(this.servers.get(qualifier));
  }

  /** Looks up all servers in the service entry. */
  public List<ServerEntry> lookup() {
    return this.servers.values().stream().toList();
  }
}
