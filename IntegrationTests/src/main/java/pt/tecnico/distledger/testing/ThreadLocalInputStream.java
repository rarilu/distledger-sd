package pt.tecnico.distledger.testing;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream that delegates to a ThreadLocal. Useful in the integration tests, where we need to
 * provide a different InputStream to each thread.
 */
class ThreadLocalInputStream extends InputStream {
  private final ThreadLocal<InputStream> stream;

  /** Creates a new ThreadLocalInputStream. */
  public ThreadLocalInputStream(ThreadLocal<InputStream> stream) {
    this.stream = stream;
  }

  @Override
  public int read() throws IOException {
    return stream.get().read();
  }

  /** Sets the InputStream to delegate to in the current thread. */
  public void setStream(InputStream stream) {
    this.stream.set(stream);
  }
}
