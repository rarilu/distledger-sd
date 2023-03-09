package pt.tecnico.distledger.adminclient

import io.grpc.Status
import pt.tecnico.distledger.contract.admin.AdminServiceGrpc
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse
import org.grpcmock.GrpcMock

class ActivateTest extends BaseTest {
    def "user provides invalid activate command"() {
        given: "an invalid activate input"
        provideInput("activate wrong usage\nexit\n")

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == ("> " + EXPECTED_USAGE_STRING + "\n> ")

        and: "the mock server received no requests"
        GrpcMock.verifyThat(GrpcMock.calledMethod(AdminServiceGrpc.getActivateMethod()), GrpcMock.never())
    }

    def "activate server returns empty response"() {
        given: "an activate input"
        provideInput("activate A\nexit\n")

        and: "a mock server that returns an empty response"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(AdminServiceGrpc.getActivateMethod())
                        .willReturn(GrpcMock.response(ActivateResponse.getDefaultInstance())))

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == "> OK\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(AdminServiceGrpc.getActivateMethod())
                        .withRequest(ActivateRequest.getDefaultInstance()),
                GrpcMock.times(1)
        )
    }

    def "activate returns a status exception"() {
        given: "an activate server input"
        provideInput("activate A\nexit\n")

        and: "a mock server that returns a status exception"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(AdminServiceGrpc.getActivateMethod())
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
                GrpcMock.calledMethod(AdminServiceGrpc.getActivateMethod())
                        .withRequest(ActivateRequest.getDefaultInstance()),
                GrpcMock.times(1)
        )
    }
}
