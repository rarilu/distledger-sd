package pt.tecnico.distledger.common;

import java.util.concurrent.atomic.AtomicBoolean;

/** Helper class to print debug messages. */
public final class Logger {
  /**
   * Set flag to true to print debug messages. The flag can be set by passing Maven the -Ddebug
   * command line option.
   */
  private static final AtomicBoolean debugFlag =
      new AtomicBoolean(System.getProperty("debug") != null);

  private Logger() {}

  /**
   * Sets the debug flag.
   *
   * @param flag the new value of the debug flag
   */
  public static void setDebugFlag(boolean flag) {
    debugFlag.set(flag);
  }

  /**
   * Prints a debug message if the debug flag is set.
   *
   * @param debugMessage the message to print
   */
  public static void debug(String debugMessage) {
    if (debugFlag.get()) {
      System.err.println(debugMessage);
    }
  }

  /**
   * Prints an error message.
   *
   * @param errorMessage the message to print
   */
  public static void error(String errorMessage) {
    System.err.println(errorMessage);
  }
}
