package pt.tecnico.distledger.common.domain;

import java.util.ArrayList;
import java.util.stream.Collectors;
import pt.tecnico.distledger.common.domain.exceptions.ConcurrentVectorClocksException;

/** Vector clock implementation class. */
public final class VectorClock implements Comparable<VectorClock> {
  private final ArrayList<Integer> timeStamps = new ArrayList<Integer>();

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

  @Override
  public int compareTo(VectorClock other) {
    int order = 0;

    for (int i = 0; i < Math.max(this.timeStamps.size(), other.timeStamps.size()); ++i) {
      int cmp = Integer.signum(Integer.compare(this.get(i), other.get(i)));
      if (order == 0) {
        order = cmp;
      } else if (order != cmp && cmp != 0) {
        throw new ConcurrentVectorClocksException();
      }
    }

    return order;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof VectorClock vectorClock) {
      return this.compareTo(vectorClock) == 0;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.timeStamps.hashCode();
  }

  @Override
  public String toString() {
    return this.timeStamps.stream()
        .map(Object::toString)
        .collect(Collectors.joining(", ", "(", ")"));
  }
}
