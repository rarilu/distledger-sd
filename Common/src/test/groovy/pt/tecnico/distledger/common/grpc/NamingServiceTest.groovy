package pt.tecnico.distledger.common.grpc

import pt.tecnico.distledger.contract.namingserver.NamingServiceGrpc
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.LookupRequest;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.LookupResponse;
import spock.lang.Specification

import org.grpcmock.GrpcMock

class NamingServiceTest extends Specification {
    def service

    def setup() {
        GrpcMock.configureFor(GrpcMock.grpcMock(5001).build().start())

        service = new NamingService()
    }

    def "looks up a service"() {
        given: "a mock server that returns a response with a list of targets for a given service"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(NamingServiceGrpc.getLookupMethod())
                        .willReturn(GrpcMock.response(LookupResponse.newBuilder()
                                .addAllTargets(["localhost:5001", "localhost:5002", "localhost:5003"]).build())))

        when: "a lookup of a service is requested"
        def result = service.lookup("DistLedger")

        then: "the result is correct"
        result == ["localhost:5001", "localhost:5002", "localhost:5003"]

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(NamingServiceGrpc.getLookupMethod())
                        .withRequest(LookupRequest
                                .newBuilder()
                                .setService("DistLedger")
                                .build()),
                GrpcMock.times(1)
        )
    }
}
