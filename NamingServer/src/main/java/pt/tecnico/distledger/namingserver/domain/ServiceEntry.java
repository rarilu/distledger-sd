package pt.tecnico.distledger.namingserver.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import pt.tecnico.distledger.namingserver.domain.exceptions.DuplicateServerEntryException;
import pt.tecnico.distledger.namingserver.domain.exceptions.ServerEntryNotFoundException;

/** Represents a service entry in the naming server. */
public class ServiceEntry {
  private final String name;
  private final ConcurrentMap<String, List<ServerEntry>> servers = new ConcurrentHashMap<>();
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
    if (!this.targets.add(target) || this.servers.containsKey(qualifier)) {
      this.targets.remove(target); // remove target if it was added
      throw new DuplicateServerEntryException(this.name, qualifier, target);
    }

    int id = this.nextId.getAndIncrement();

    // Add the server entry to the list of servers for the given qualifier
    this.servers.compute(
        qualifier,
        (k, serverEntries) -> {
          ServerEntry newEntry = new ServerEntry(k, target, id);

          if (serverEntries == null) {
            serverEntries = Collections.synchronizedList(new ArrayList<>(List.of(newEntry)));
          } else {
            serverEntries.add(newEntry);
          }

          return serverEntries;
        });

    return id;
  }

  /** Deletes a server from the service entry. */
  public void deleteServer(String target) {
    // Safety: no need to synchronize while iterating the concurrent map; standard says it's safe
    for (List<ServerEntry> serverEntries : this.servers.values()) {
      if (serverEntries.removeIf(serverEntry -> target.equals(serverEntry.target()))) {
        this.targets.remove(target); // remove the target from the set of registered targets
        this.servers.values().removeIf(List::isEmpty);
        return;
      }
    }

    // If we reach this point, the server was not found
    throw new ServerEntryNotFoundException(this.name, target);
  }

  /** Looks up servers with the given qualifier in the service entry. */
  public List<ServerEntry> lookup(String qualifier) {
    List<ServerEntry> serverEntries = this.servers.getOrDefault(qualifier, List.of());

    // Safety: we need to synchronize on the server entries list since we are iterating over it
    // It's okay to return the records without cloning them, since they are immutable
    synchronized (serverEntries) {
      return serverEntries.stream().filter(entry -> qualifier.equals(entry.qualifier())).toList();
    }
  }

  /** Looks up all servers in the service entry. */
  public List<ServerEntry> lookup() {
    List<ServerEntry> entries = new ArrayList<>();

    // Safety: no need to synchronize while iterating the concurrent map; standard says it's safe
    // It's okay to return the records without cloning them, since they are immutable
    for (List<ServerEntry> serverEntries : this.servers.values()) {
      // Safety: we need to synchronize on the server entries list since we are iterating over it
      synchronized (serverEntries) {
        entries.addAll(serverEntries);
      }
    }

    return entries;
  }
}
