package pt.tecnico.distledger.common.client;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;
import pt.tecnico.distledger.common.Logger;

/** Base code for parsing user input and executing corresponding commands. */
public abstract class BaseCommandParser {
  protected static final String SPACE = " ";
  private static final String HELP = "help";
  private static final String EXIT = "exit";

  protected abstract void dispatchCommand(String cmd, String line);

  protected abstract void printUsage();

  /** Reads a line from stdin. */
  private Optional<String> readLine() throws IOException {
    int c;
    StringBuilder line = new StringBuilder();

    while ((c = System.in.read()) != -1 && (char) c != '\n') {
      line.append((char) c);
    }

    if (c == -1 && (line.length() == 0)) {
      return Optional.empty();
    } else {
      return Optional.of(line.toString());
    }
  }

  /**
   * Parses the input from the user and executes the corresponding commands, in a loop until an
   * explicit exit instruction or the end of user input.
   */
  public void parseInput() {
    boolean exit = false;

    while (!exit) {
      try {
        System.out.print("> ");
        String line = this.readLine().map(String::trim).orElseThrow(NoSuchElementException::new);
        String cmd = line.split(SPACE)[0];

        switch (cmd) {
          case HELP -> this.printUsage();
          case EXIT -> exit = true;
          default -> this.dispatchCommand(cmd, line);
        }
      } catch (NoSuchElementException e) {
        exit = true; // End of user input
      } catch (IOException e) {
        Logger.error("Error reading from stdin: " + e.getMessage());
        exit = true;
      }
    }
  }

  protected void handleServiceCallResponse(Supplier<Optional<String>> serviceCall) {
    try {
      // Needs to be a supplier for lazy evaluation
      // Any exceptions must be thrown inside this try block

      final String representation =
          serviceCall.get().orElseThrow(() -> new RuntimeException("Server is unavailable"));
      System.out.println("OK");
      System.out.println(representation);
    } catch (RuntimeException e) {
      System.out.println("Error: " + e.getMessage());
      System.out.println();
    }
  }
}
