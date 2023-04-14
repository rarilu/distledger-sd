package pt.tecnico.distledger.common.grpc

import pt.tecnico.distledger.common.domain.VectorClock
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions

import spock.lang.Specification

class ProtoUtilsTest extends Specification {
    def "converts a vector clock to a protobuf message"() {
        given: "a vector clock"
        def clock = new VectorClock((int[])[2, 1, 0])

        when: "the vector clock is converted to a protobuf message"
        def message = ProtoUtils.toProto(clock)

        then: "the message is correct"
        message == DistLedgerCommonDefinitions.VectorClock.newBuilder()
                .addValues(2)
                .addValues(1)
                .build()
    }
}
