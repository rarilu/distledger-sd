package pt.tecnico.distledger.common.grpc

import pt.tecnico.distledger.contract.namingserver.NamingServiceGrpc
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.LookupRequest
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.LookupResponse
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
                                .addAllEntries([
                                        LookupResponse.Entry.newBuilder().setQualifier("A").setTarget("localhost:5001").setId(0).build(),
                                        LookupResponse.Entry.newBuilder().setQualifier("B").setTarget("localhost:5002").setId(1).build(),
                                        LookupResponse.Entry.newBuilder().setQualifier("C").setTarget("localhost:5003").setId(2).build()
                                        ]).build())))

        when: "a lookup of a service is requested"
        def result = service.lookup("DistLedger")

        then: "the result is correct"
        result == [new NamingService.Entry("A", "localhost:5001", 0),
                   new NamingService.Entry("B", "localhost:5002", 1),
                   new NamingService.Entry("C", "localhost:5003", 2)]

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
