package pt.tecnico.distledger.namingserver.domain;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import pt.tecnico.distledger.namingserver.domain.exceptions.ServerEntryNotFoundException;

/** Represents the current state of the naming server. */
public class NamingServerState {
  private final ConcurrentMap<String, ServiceEntry> services = new ConcurrentHashMap<>();

  /** Registers a new server entry for the given service and target. */
  public void registerServer(String service, String qualifier, String target) {
    this.services.computeIfAbsent(service, ServiceEntry::new).registerServer(qualifier, target);
  }

  /** Deletes the server entry for the given service and target. */
  public void deleteServer(String service, String target) {
    if (this.services.computeIfPresent(
            service,
            (k, v) -> {
              v.deleteServer(target);
              return v;
            })
        == null) {
      throw new ServerEntryNotFoundException(service, target);
    }
  }

  /** Looks up the server entries for the given service and qualifier. */
  public List<String> lookup(String service, String qualifier) {
    return Optional.ofNullable(this.services.get(service))
        .map(s -> s.lookup(qualifier))
        .orElse(List.of());
  }

  /** Looks up the server entries for the given service. */
  public List<String> lookup(String service) {
    return Optional.ofNullable(this.services.get(service)).map(s -> s.lookup()).orElse(List.of());
  }
}
