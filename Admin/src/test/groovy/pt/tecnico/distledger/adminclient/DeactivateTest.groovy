package pt.tecnico.distledger.adminclient

import io.grpc.Status
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse
import org.grpcmock.GrpcMock

class DeactivateTest extends BaseTest {
    def "user provides invalid deactivate command"() {
        given: "an invalid deactivate input"
        provideInput("deactivate wrong usage\nexit\n")

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == EXPECTED_USAGE_STRING

        and: "the mock server received no requests"
        GrpcMock.verifyThat(GrpcMock.calledMethod(AdminServiceGrpc.getDeactivateMethod()), GrpcMock.never())
    }

    def "deactivate server returns empty response"() {
        given: "a deactivate input"
        provideInput("deactivate A\nexit\n")

        and: "a mock server that returns an empty response"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(AdminServiceGrpc.getDeactivateMethod())
                        .willReturn(GrpcMock.response(DeactivateResponse.getDefaultInstance())))

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == "> OK\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(AdminServiceGrpc.getDeactivateMethod())
                        .withRequest(DeactivateRequest.getDefaultInstance()),
                GrpcMock.times(1)
        )
    }

    def "deactivate returns a status exception"() {
        given: "a create account input"
        provideInput("deactivate A\nexit\n")

        and: "a mock server that returns a status exception"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(AdminServiceGrpc.getDeactivateMethod())
                        .willReturn(GrpcMock.statusException(
                                Status.UNKNOWN
                                        .withDescription("The server raised an exception")
                        ))
        )

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == "> Error: The server raised an exception\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(AdminServiceGrpc.getDeactivateMethod())
                        .withRequest(DeactivateRequest.getDefaultInstance()),
                GrpcMock.times(1)
        )
    }
}
