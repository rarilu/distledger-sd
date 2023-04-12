package pt.tecnico.distledger.adminclient

import io.grpc.Status
import pt.tecnico.distledger.contract.admin.AdminServiceGrpc
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GossipRequest
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GossipResponse
import org.grpcmock.GrpcMock

class GossipTest extends BaseTest {
    def "user provides invalid gossip command"() {
        given: "an invalid gossip input"
        provideInput("gossip wrong usage\nexit\n")

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == ("> " + EXPECTED_USAGE_STRING + "\n> ")

        and: "the mock server received no requests"
        GrpcMock.verifyThat(GrpcMock.calledMethod(AdminServiceGrpc.getGossipMethod()), GrpcMock.never())
    }

    def "gossip server returns empty response"() {
        given: "a gossip input"
        provideInput("gossip A\nexit\n")

        and: "a mock server that returns an empty response"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(AdminServiceGrpc.getGossipMethod())
                        .willReturn(GrpcMock.response(GossipResponse.getDefaultInstance())))

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == "> OK\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(AdminServiceGrpc.getGossipMethod())
                        .withRequest(GossipRequest.getDefaultInstance()),
                GrpcMock.times(1)
        )
    }

    def "gossip returns a status exception"() {
        given: "a gossip server input"
        provideInput("gossip A\nexit\n")

        and: "a mock server that returns a status exception"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(AdminServiceGrpc.getGossipMethod())
                        .willReturn(GrpcMock.statusException(
                                Status.UNKNOWN
                                        .withDescription("The server raised an exception")
                        ))
        )

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == "> Error: UNKNOWN: The server raised an exception\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(AdminServiceGrpc.getGossipMethod())
                        .withRequest(GossipRequest.getDefaultInstance()),
                GrpcMock.times(1)
        )
    }
}
