package pt.tecnico.distledger.namingserver.domain;

import java.util.HashMap;
import java.util.Map;

/** Represents the current state of the naming server. */
public class NamingServerState {
  private final Map<String, ServiceEntry> services = new HashMap<>();

  public void registerServer(String service, String qualifier, String target) {
    this.services.computeIfAbsent(service, ServiceEntry::new).registerServer(qualifier, target);
  }
}
