package pt.tecnico.distledger.common.grpc;

import java.util.List;
import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions;

/** Collection of utility methods for gRPC Proto objects manipulation. */
public class ProtoUtils {
  private ProtoUtils() {}

  /** Convert a VectorClock domain entity to a Proto object. */
  public static DistLedgerCommonDefinitions.VectorClock toProto(VectorClock vectorClock) {
    List<Integer> timeStamps = vectorClock.toList();

    // Remove trailing zeros
    while (!timeStamps.isEmpty() && timeStamps.get(timeStamps.size() - 1) == 0) {
      timeStamps.remove(timeStamps.size() - 1);
    }

    return DistLedgerCommonDefinitions.VectorClock.newBuilder().addAllValues(timeStamps).build();
  }

  /** Convert a Proto VectorClock object to a domain entity. */
  public static VectorClock fromProto(DistLedgerCommonDefinitions.VectorClock vectorClock) {
    return new VectorClock(
        vectorClock.getValuesList().stream().mapToInt(Integer::intValue).toArray());
  }
}
