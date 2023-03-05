package pt.tecnico.distledger.utils;

/** Helper class to print debug messages. */
public final class Logger {
  /**
   * Set flag to true to print debug messages. The flag can be set by passing Maven the -Ddebug
   * command line option.
   */
  private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

  private Logger() {}

  /**
   * Prints a debug message if the debug flag is set.
   *
   * @param debugMessage the message to print
   */
  public static void debug(String debugMessage) {
    if (DEBUG_FLAG) {
      System.err.println(debugMessage);
    }
  }
}
