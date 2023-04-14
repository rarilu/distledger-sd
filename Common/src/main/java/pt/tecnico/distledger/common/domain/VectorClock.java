package pt.tecnico.distledger.common.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Vector clock implementation class. */
public final class VectorClock {
  private final ArrayList<Integer> timeStamps = new ArrayList<>();

  /** Creates a new vector clock with all timestamps set to 0. */
  public VectorClock() {}

  /**
   * Creates a new vector clock with the given timestamps.
   *
   * @param timeStamps Timestamps to initialize the clock with.
   */
  public VectorClock(int[] timeStamps) {
    for (int i = 0; i < timeStamps.length; i++) {
      if (timeStamps[i] > 0) {
        this.set(i, timeStamps[i]);
      }
    }
  }

  /**
   * Creates a new vector clock from a given vector clock.
   *
   * @param other Vector clock to copy
   */
  public VectorClock(VectorClock other) {
    this.timeStamps.addAll(other.timeStamps);
  }

  /** Represents the possible results of a comparison between two vector clocks. */
  public enum Order {
    BEFORE,
    EQUAL,
    AFTER,
    CONCURRENT
  }

  /**
   * Increment the timestamp of the given replica.
   *
   * @param replica Replica to increment.
   */
  public void increment(int replica) {
    this.set(replica, this.get(replica) + 1);
  }

  /**
   * Merges the given clock into this one.
   *
   * @param other Clock to merge with.
   */
  public void merge(VectorClock other) {
    for (int i = 0; i < other.timeStamps.size(); i++) {
      this.mergeSingle(other, i);
    }
  }

  /**
   * Merges the timestamp of a single replica of the given clock into this one.
   *
   * @param other Clock to merge with.
   * @param replica Replica to merge.
   */
  public void mergeSingle(VectorClock other, int replica) {
    this.set(replica, Math.max(this.get(replica), other.get(replica)));
  }

  /**
   * Gets the timestamp of the given replica.
   *
   * @param replica Replica id.
   * @return The timestamp.
   */
  public int get(int replica) {
    if (replica < this.timeStamps.size()) {
      return this.timeStamps.get(replica);
    } else {
      return 0;
    }
  }

  /**
   * Sets the timestamp of the given replica to the given value.
   *
   * @param replica Replica id.
   * @param ts New timestamp.
   */
  public void set(int replica, int ts) {
    this.timeStamps.ensureCapacity(replica + 1); // Avoid allocating each add
    while (replica >= this.timeStamps.size()) {
      this.timeStamps.add(0);
    }

    this.timeStamps.set(replica, ts);
  }

  /** Converts the vector clock to a list of timestamps. */
  public List<Integer> toList() {
    return new ArrayList<>(this.timeStamps);
  }

  /**
   * Compares two vector clocks.
   *
   * @param c1 First clock.
   * @param c2 Second clock.
   * @return the result of the comparison.
   */
  public static Order compare(VectorClock c1, VectorClock c2) {
    Order order = Order.EQUAL;

    for (int i = 0; i < Math.max(c1.timeStamps.size(), c2.timeStamps.size()); ++i) {
      Order comparison;
      if (c1.get(i) < c2.get(i)) {
        comparison = Order.BEFORE;
      } else if (c1.get(i) > c2.get(i)) {
        comparison = Order.AFTER;
      } else {
        comparison = Order.EQUAL;
      }

      if (order == Order.EQUAL) {
        order = comparison;
      } else if (order != comparison && comparison != Order.EQUAL) {
        return Order.CONCURRENT;
      }
    }

    return order;
  }

  @Override
  public String toString() {
    return this.timeStamps.stream()
        .map(Object::toString)
        .collect(Collectors.joining(", ", "(", ")"));
  }
}
