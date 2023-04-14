package pt.tecnico.distledger.testing;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream that delegates to a ThreadLocal. Useful in the integration tests, where we need to
 * provide a different OutputStream to each thread.
 */
class ThreadLocalOutputStream extends OutputStream {
  private final ThreadLocal<OutputStream> stream;

  /** Creates a new ThreadLocalOutputStream. */
  public ThreadLocalOutputStream(ThreadLocal<OutputStream> stream) {
    this.stream = stream;
  }

  @Override
  public void write(int b) throws IOException {
    stream.get().write(b);
  }

  /** Sets the OutputStream to delegate to in the current thread. */
  public void setStream(OutputStream stream) {
    this.stream.set(stream);
  }
}
