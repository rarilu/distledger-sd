package pt.tecnico.distledger.common.client;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Supplier;

/** Base code for parsing user input and executing corresponding commands. */
public abstract class BaseCommandParser {
  protected static final String SPACE = " ";
  private static final String HELP = "help";
  private static final String EXIT = "exit";

  protected abstract void dispatchCommand(String cmd, String line);

  protected abstract void printUsage();

  /**
   * Parses the input from the user and executes the corresponding commands, in a loop until an
   * explicit exit instruction or the end of user input.
   */
  public void parseInput() {
    boolean exit = false;

    try (final Scanner scanner = new Scanner(System.in)) {
      while (!exit) {
        try {
          System.out.print("> ");
          String line = scanner.nextLine().trim();
          String cmd = line.split(SPACE)[0];

          switch (cmd) {
            case HELP -> this.printUsage();
            case EXIT -> exit = true;
            default -> this.dispatchCommand(cmd, line);
          }
        } catch (NoSuchElementException e) {
          exit = true;
        }
      }
    }
  }

  protected void handleServiceCallResponse(Supplier<Optional<String>> serviceCall) {
    try {
      // needs to be a supplier for lazy evaluation
      // any exceptions must be thrown inside this try block

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
