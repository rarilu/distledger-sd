package pt.tecnico.distledger.common.grpc;

import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import java.util.Objects;
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
public abstract class ClientService<S> implements AutoCloseable {
  private final StubCache<S> stubCache;

  protected ClientService(NamingService namingService, Function<Channel, S> stubFactory) {
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
   * @return false if the server is unavailable (to allow retrying), true otherwise.
   */
  protected <Q, R> boolean makeRequest(
      String qualifier, Q request, BiFunction<S, Q, R> dispatcher) {
    try {
      S stub = this.stubCache.getStub(qualifier);

      Logger.debug("Sending request: " + request.toString());
      R response = dispatcher.apply(stub, request);
      String representation = response.toString();

      System.out.println("OK");
      System.out.println(representation);
    } catch (StatusRuntimeException e) {
      if (Objects.equals(e.getStatus().getCode(), io.grpc.Status.Code.UNAVAILABLE)) {
        return false;
      }

      System.out.println("Error: " + e.getStatus().getDescription());
      System.out.println();
    } catch (RuntimeException e) {
      // This happens when no server is found with the specified qualifier
      System.out.println("Error: " + e.getMessage());
      System.out.println();
    }

    return true;
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
   */
  protected <Q, R> void makeRequestWithRetryInvalidatingStubCache(
      String qualifier, Q request, BiFunction<S, Q, R> dispatcher, int maxTries) {
    for (int i = 0; i < maxTries; i++) {
      if (this.makeRequest(qualifier, request, dispatcher)) {
        return;
      }

      this.stubCache.invalidateCachedStub(qualifier);
    }

    System.out.println("Error: Server is unavailable.");
    System.out.println();
  }

  /** Close underlying stubCache. */
  @Override
  public void close() {
    this.stubCache.close();
  }
}
