package pt.tecnico.distledger.common.grpc;

import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.NamingService;
import pt.tecnico.distledger.common.StubCache;

/**
 * Abstracts the basic functionality of a client service.
 *
 * @param <S> the type of the stub to be used by the service.
 */
public abstract class BaseService<S> implements AutoCloseable {
  private final StubCache<S> stubCache;

  protected BaseService(NamingService namingService, Function<Channel, S> stubFactory) {
    this.stubCache = new StubCache<>(namingService, stubFactory);
  }

  /**
   * Dispatches requests to the server, showing the response to the user.
   *
   * @param qualifier the server's qualifier.
   * @param request the request to be sent.
   * @param dispatcher the function that dispatches the request to the server. It receives the stub
   *     and the request, and returns the response.
   * @param <Q> the request type.
   * @param <R> the response type.
   * @return the response, or an empty if the server is unavailable.
   * @throws RuntimeException if the request fails or no server with the specified qualifier is
   *     found.
   */
  protected <Q, R> Optional<R> makeRequest(
      String qualifier, Q request, BiFunction<S, Q, R> dispatcher) {
    try {
      S stub = this.stubCache.getStub(qualifier);

      Logger.debug("Sending request: " + request.toString());
      return Optional.of(dispatcher.apply(stub, request));
    } catch (StatusRuntimeException e) {
      if (Objects.equals(e.getStatus().getCode(), io.grpc.Status.Code.UNAVAILABLE)) {
        return Optional.empty();
      }

      throw e;
    }
    // purposely not catching RuntimeException, thrown when no server is found
    // with the specified qualifier
  }

  /**
   * Dispatches requests to the server, showing the response to the user, retrying if necessary when
   * the server is unavailable.
   *
   * @param qualifier the server's qualifier.
   * @param request the request to be sent.
   * @param dispatcher the function that dispatches the request to the server. It receives the stub
   *     and the request, and returns the response.
   * @param maxTries the maximum number of tries to be performed.
   * @param <Q> the request type.
   * @param <R> the response type.
   * @return the response, or an empty if the server is unavailable after maxTries.
   * @throws RuntimeException if a request fails or no server with the specified qualifier is found.
   */
  protected <Q, R> Optional<R> makeRequestWithRetryInvalidatingStubCache(
      String qualifier, Q request, BiFunction<S, Q, R> dispatcher, int maxTries) {
    for (int i = 0; i < maxTries; i++) {
      final Optional<R> response = this.makeRequest(qualifier, request, dispatcher);

      if (response.isPresent()) {
        return response;
      } else {
        this.stubCache.invalidateCachedStub(qualifier);
      }
    }

    return Optional.empty();
  }

  /** Close underlying stubCache. */
  @Override
  public void close() {
    this.stubCache.close();
  }
}
