package pt.tecnico.distledger.namingserver.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import pt.tecnico.distledger.namingserver.domain.exceptions.DuplicateServerEntryException;
import pt.tecnico.distledger.namingserver.domain.exceptions.ServerEntryNotFoundException;

/** Represents a service entry in the naming server. */
public class ServiceEntry {
  String name;
  ConcurrentMap<String, List<ServerEntry>> servers = new ConcurrentHashMap<>();

  public ServiceEntry(String name) {
    this.name = name;
  }

  /** Registers a server in the service entry. */
  public void registerServer(String qualifier, String target) {
    this.servers.compute(
        qualifier,
        (k, serverEntries) -> {
          ServerEntry newEntry = new ServerEntry(k, target);

          if (serverEntries == null) {
            serverEntries = Collections.synchronizedList(new ArrayList<>(List.of(newEntry)));
          } else if (serverEntries.contains(newEntry)) {
            throw new DuplicateServerEntryException(this.name, qualifier, target);
          } else {
            serverEntries.add(newEntry);
          }

          return serverEntries;
        });
  }

  public List<String> lookupServer(String qualifier) {
    List<ServerEntry> serverEntries = this.servers.get(qualifier);

    if (serverEntries == null) {
      return Collections.emptyList();
    }

    return serverEntries.stream()
        .filter(entry -> entry.qualifier().equals(qualifier))
        .map(ServerEntry::target)
        .toList();
  }

  /** Deletes a server from the service entry. */
  public void deleteServer(String target) {
    // Safety: we need to synchronize on the servers map since we are iterating over it
    synchronized (this.servers) {
      for (List<ServerEntry> serverEntries : this.servers.values()) {
        if (serverEntries.removeIf(serverEntry -> Objects.equals(serverEntry.target(), target))) {
          return;
        }
      }

      // If we reach this point, the server was not found
      throw new ServerEntryNotFoundException(this.name, target);
    }
  }
}
