package pt.tecnico.distledger.common.grpc;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.grpc.exceptions.DuplicateQualifierException;
import pt.tecnico.distledger.common.grpc.exceptions.ServerNotFoundException;

/** Abstracts the creation and caching of gRPC stubs. */
public class StubCache<T> implements AutoCloseable {
  private record CachedStub<T>(ManagedChannel channel, T stub) implements AutoCloseable {
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
          List<String> targets = this.namingService.lookup(SERVICE_NAME, qual);
          if (targets.size() > 1) {
            throw new DuplicateQualifierException(qual);
          }

          return targets.stream()
              .findFirst()
              .map(
                  target -> {
                    Logger.debug("Connecting to " + target);
                    final ManagedChannel channel =
                        ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                    final T stub = this.stubFactory.apply(channel);

                    return new CachedStub<>(channel, stub);
                  })
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

  /** Close channel immediately. */
  @Override
  public void close() {
    this.cachedStubs.values().forEach(CachedStub::close);
  }
}
