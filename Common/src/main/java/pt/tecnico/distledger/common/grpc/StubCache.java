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
import java.util.function.Consumer;
import java.util.function.Function;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.grpc.exceptions.DuplicateQualifierException;
import pt.tecnico.distledger.common.grpc.exceptions.ServerNotFoundException;

/** Abstracts the creation and caching of gRPC stubs. */
public class StubCache<T> implements AutoCloseable {
  private record CachedStub<T>(String target, ManagedChannel channel, T stub)
      implements AutoCloseable {
    /**
     * Constructs a new CachedStub from a target and a stub factory.
     *
     * <p>This static cannot be a constructor as all record constructors must defer to the canonical
     * constructor on their first statement, and that would not be possible in this case, as it
     * would not allow logging nor creating the stub (which depends on the channel).
     *
     * @param target the target to connect to.
     * @param stubFactory the function that creates a new stub of type T.
     * @param <T> the type of the stub to be cached.
     * @return a new CachedStub instance.
     */
    public static <T> CachedStub<T> build(String target, Function<Channel, T> stubFactory) {
      Logger.debug("Connecting to " + target);
      final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
      final T stub = stubFactory.apply(channel);
      return new CachedStub<>(target, channel, stub);
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
              .map(entry -> CachedStub.build(entry.target(), this.stubFactory))
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
   * Invokes a given consumer for each of the service's servers.
   *
   * @throws StatusRuntimeException if a Naming Service lookup operation fails.
   */
  public void forEachServerInService(Consumer<NamingService.Entry> consumer) {
    List<NamingService.Entry> entries = this.namingService.lookup(SERVICE_NAME);

    for (NamingService.Entry entry : entries) {
      this.cachedStubs.compute(
          entry.qualifier(),
          (qualifier, currentStub) -> {
            if (currentStub != null && Objects.equals(currentStub.target(), entry.target())) {
              return currentStub;
            } else {
              Optional.ofNullable(currentStub).ifPresent(CachedStub::close);
              return CachedStub.build(entry.target(), this.stubFactory);
            }
          });
      consumer.accept(entry);
    }
  }

  /** Close channel immediately. */
  @Override
  public void close() {
    this.cachedStubs.values().forEach(CachedStub::close);
  }
}
