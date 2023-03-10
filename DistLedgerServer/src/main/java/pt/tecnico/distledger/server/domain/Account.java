package pt.tecnico.distledger.server.domain;

/** Represents an account. */
public class Account {
  private int balance;

  public Account(int balance) {
    this.balance = balance;
  }

  public Account() {
    this(0);
  }

  public int getBalance() {
    return this.balance;
  }

  public void setBalance(int balance) {
    this.balance = balance;
  }
}
