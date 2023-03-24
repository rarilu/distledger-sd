package pt.tecnico.distledger.namingserver.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import pt.tecnico.distledger.namingserver.domain.exceptions.DuplicateServerEntryException;
import pt.tecnico.distledger.namingserver.domain.exceptions.ServerEntryNotFoundException;

/** Represents a service entry in the naming server. */
public class ServiceEntry {
  private final String name;
  private final ConcurrentMap<String, List<ServerEntry>> servers = new ConcurrentHashMap<>();
  private final Set<String> targets = ConcurrentHashMap.newKeySet();

  public ServiceEntry(String name) {
    this.name = name;
  }

  /** Registers a server in the service entry. */
  public void registerServer(String qualifier, String target) {
    // Add the target to the set of registered targets, and throw an exception if it was already
    // present
    if (!this.targets.add(target)) {
      throw new DuplicateServerEntryException(this.name, target);
    }

    // Add the server entry to the list of servers for the given qualifier
    this.servers.compute(
        qualifier,
        (k, serverEntries) -> {
          ServerEntry newEntry = new ServerEntry(k, target);

          if (serverEntries == null) {
            serverEntries = Collections.synchronizedList(new ArrayList<>(List.of(newEntry)));
          } else {
            serverEntries.add(newEntry);
          }

          return serverEntries;
        });
  }

  /** Deletes a server from the service entry. */
  public void deleteServer(String target) {
    // Safety: no need to synchronize while iterating the concurrent map; standard says it's safe
    for (List<ServerEntry> serverEntries : this.servers.values()) {
      if (serverEntries.removeIf(serverEntry -> target.equals(serverEntry.target()))) {
        // Remove the target from the set of registered targets
        this.targets.remove(target);
        return;
      }
    }

    // If we reach this point, the server was not found
    throw new ServerEntryNotFoundException(this.name, target);
  }

  /** Looks up servers with the given qualifier in the service entry. */
  public List<String> lookup(String qualifier) {
    List<ServerEntry> serverEntries = this.servers.getOrDefault(qualifier, List.of());

    // Safety: we need to synchronize on the server entries list since we are iterating over it
    synchronized (serverEntries) {
      return serverEntries.stream()
          .filter(entry -> qualifier.equals(entry.qualifier()))
          .map(ServerEntry::target)
          .toList();
    }
  }

  /** Looks up all servers in the service entry. */
  public List<String> lookup() {
    List<String> targets = new ArrayList<>();

    // Safety: no need to synchronize while iterating the concurrent map; standard says it's safe
    for (List<ServerEntry> serverEntries : this.servers.values()) {
      // Safety: we need to synchronize on the server entries list since we are iterating over it
      synchronized (serverEntries) {
        targets.addAll(serverEntries.stream().map(ServerEntry::target).toList());
      }
    }

    return targets;
  }
}
