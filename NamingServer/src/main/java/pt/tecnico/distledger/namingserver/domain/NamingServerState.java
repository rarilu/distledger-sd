package pt.tecnico.distledger.namingserver.domain;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Represents the current state of the naming server. */
public class NamingServerState {
  private final ConcurrentMap<String, ServiceEntry> services = new ConcurrentHashMap<>();

  public void registerServer(String service, String qualifier, String target) {
    this.services.computeIfAbsent(service, ServiceEntry::new).registerServer(qualifier, target);
  }

  public void deleteServer(String service, String target) {
    this.services.computeIfPresent(
        service,
        (k, v) -> {
          v.deleteServer(target);
          return v;
        });
  }
}
