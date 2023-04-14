package pt.tecnico.distledger.common.grpc;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.grpc.exceptions.DuplicateQualifierException;
import pt.tecnico.distledger.common.grpc.exceptions.ServerNotFoundException;

/** Abstracts the creation and caching of gRPC stubs. */
public class StubCache<T> implements AutoCloseable {
  private record CachedStub<T>(int id, String target, ManagedChannel channel, T stub)
      implements AutoCloseable {
    /**
     * Constructs a new CachedStub from an id, a target and a stub factory.
     *
     * <p>This static cannot be a constructor as all record constructors must defer to the canonical
     * constructor on their first statement, and that would not be possible in this case, as it
     * would not allow logging nor creating the stub (which depends on the channel).
     *
     * @param id the id of the server to be cached.
     * @param target the target to connect to.
     * @param stubFactory the function that creates a new stub of type T.
     * @param <T> the type of the stub to be cached.
     * @return a new CachedStub instance.
     */
    public static <T> CachedStub<T> build(int id, String target, Function<Channel, T> stubFactory) {
      Logger.debug("Connecting to " + target);
      final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
      final T stub = stubFactory.apply(channel);
      return new CachedStub<>(id, target, channel, stub);
    }

    @Override
    public void close() {
      this.channel().shutdownNow();
    }
  }

  private static final String SERVICE_NAME = "DistLedger";

  private final NamingService namingService;
  private final ConcurrentMap<String, CachedStub<T>> cachedStubs = new ConcurrentHashMap<>();
  private final Function<Channel, T> stubFactory;
  private final AtomicBoolean lookupByServiceCacheInvalidated = new AtomicBoolean(true);

  public StubCache(NamingService namingService, Function<Channel, T> stubFactory) {
    this.namingService = namingService;
    this.stubFactory = stubFactory;
  }

  /**
   * Gets a stub to communicate with a server that has the specified qualifier, either from the
   * cache or by performing a lookup through the Naming Service.
   *
   * @param qualifier the qualifier of the server to connect to.
   * @return a stub to communicate with the server.
   * @throws StatusRuntimeException if the lookup operation fails.
   * @throws DuplicateQualifierException if there is more than one server with the same qualifier.
   * @throws ServerNotFoundException if no server with specified qualifier is found.
   */
  public T getStub(String qualifier) {
    this.cachedStubs.computeIfAbsent(
        qualifier,
        qual -> {
          List<NamingService.Entry> entries = this.namingService.lookup(SERVICE_NAME, qual);
          if (entries.size() > 1) {
            throw new DuplicateQualifierException(qual);
          }

          return entries.stream()
              .findFirst()
              .map(entry -> CachedStub.build(entry.id(), entry.target(), this.stubFactory))
              .orElse(null); // If lookup result is empty list
        });

    return Optional.ofNullable(this.cachedStubs.get(qualifier))
        .map(CachedStub::stub)
        .orElseThrow(() -> new ServerNotFoundException(qualifier));
  }

  /** Invalidates the stub cache's entry for the specified qualifier, if one exists. */
  public void invalidateCachedStub(String qualifier) {
    Logger.debug("Invalidated cached stub for " + qualifier);

    this.cachedStubs.computeIfPresent(
        qualifier,
        (k, v) -> {
          v.close();
          return null;
        });
  }

  /**
   * Marks the lookup by service cache as invalidated.
   *
   * <p>This indicates that the replicated system's configuration has changed and thus that the next
   * call to {@link #forEachServerInService(Consumer)} should forcefully perform a new lookup by
   * service through the Naming Service, in order to obtain fresh information regarding what servers
   * are available in this service.
   */
  public void invalidateLookupByServiceCache() {
    this.lookupByServiceCacheInvalidated.set(true);
  }

  /**
   * Informs the StubCache that this server noticed another server's presence.
   *
   * <p>If the StubCache does not have a cached stub for the server with the specified ID, this
   * means that the replicated system's configuration has changed and thus that the lookup by
   * service cache should be invalidated.
   *
   * @param id the other server's ID.
   */
  public void noticeServer(int id) {
    if (this.cachedStubs.values().stream()
        .map(CachedStub::id)
        .noneMatch(knownId -> knownId == id)) {
      this.invalidateLookupByServiceCache();
    }
  }

  /**
   * Populates the cache with all servers in the service, if it is invalidated.
   *
   * <p>If the lookup by service cache is marked as invalidated, forcefully performs a new lookup by
   * service through the Naming Service, in order to obtain fresh information regarding what servers
   * are available in this service. Only creates a new stub if the target has changed for a given
   * qualifier, otherwise reuses the existing stub.
   */
  private synchronized void populateCacheFromLookupByService() {
    if (!this.lookupByServiceCacheInvalidated.get()) {
      return;
    }

    for (NamingService.Entry entry : this.namingService.lookup(SERVICE_NAME)) {
      this.cachedStubs.compute(
          entry.qualifier(),
          (qualifier, currentStub) -> {
            if (currentStub != null && Objects.equals(currentStub.target(), entry.target())) {
              return currentStub;
            } else {
              Optional.ofNullable(currentStub).ifPresent(CachedStub::close);
              return CachedStub.build(entry.id(), entry.target(), this.stubFactory);
            }
          });
    }

    this.lookupByServiceCacheInvalidated.set(false);
  }

  /**
   * Invokes a given consumer for each of the service's servers.
   *
   * <p>This method may cache the results of the lookup by service operation, so {@link
   * #invalidateLookupByServiceCache()} should be called before this if the replicated system's
   * configuration has changed.
   *
   * @throws StatusRuntimeException if a Naming Service lookup operation fails.
   */
  public void forEachServerInService(Consumer<NamingService.Entry> consumer) {
    this.populateCacheFromLookupByService();

    this.cachedStubs.entrySet().stream()
        .map(
            cacheEntry ->
                new NamingService.Entry(
                    cacheEntry.getKey(),
                    cacheEntry.getValue().target(),
                    cacheEntry.getValue().id()))
        .forEach(consumer);
  }

  /** Close channel immediately. */
  @Override
  public void close() {
    this.cachedStubs.values().forEach(CachedStub::close);
  }
}
