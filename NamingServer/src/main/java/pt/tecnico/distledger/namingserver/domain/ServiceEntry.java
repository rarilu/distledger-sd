package pt.tecnico.distledger.namingserver.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pt.tecnico.distledger.namingserver.domain.exceptions.DuplicateServerEntryException;

/** Represents a service entry in the naming server. */
public class ServiceEntry {
  String name;
  Map<String, List<ServerEntry>> servers = new HashMap<>();

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
            serverEntries = new ArrayList<>(List.of(newEntry));
          } else if (serverEntries.contains(newEntry)) {
            throw new DuplicateServerEntryException(this.name, qualifier, target);
          } else {
            serverEntries.add(newEntry);
          }

          return serverEntries;
        });
  }
}
