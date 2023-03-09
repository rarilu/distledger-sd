package pt.tecnico.distledger.userclient

import io.grpc.Status
import org.grpcmock.GrpcMock
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse
import pt.tecnico.distledger.contract.user.UserServiceGrpc

class TransferToTest extends BaseTest {
    def "user provides invalid transfer to command"() {
        given: "an invalid transfer to input"
        provideInput("transferTo missing one argument\nexit\n")

        when: "the user client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == ("> " + EXPECTED_USAGE_STRING + "\n> ")

        and: "the mock server received no requests"
        GrpcMock.verifyThat(GrpcMock.calledMethod(UserServiceGrpc.getTransferToMethod()), GrpcMock.never())
    }

    def "user provides invalid transfer to amount"() {
        given: "an invalid transfer to amount input"
        provideInput("transferTo A Alice Bob notANumber\nexit\n")

        when: "the user client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == ("> Error: Invalid number provided\n> ")
    }

    def "transfer to returns empty response"() {
        given: "a transfer to input"
        provideInput("transferTo A Alice Bob 100\nexit\n")

        and: "a mock server that returns an empty response"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(UserServiceGrpc.getTransferToMethod())
                        .willReturn(GrpcMock.response(TransferToResponse.getDefaultInstance())))

        when: "the user client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == "> OK\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(UserServiceGrpc.getTransferToMethod())
                        .withRequest(TransferToRequest
                                .newBuilder()
                                .setAccountFrom("Alice")
                                .setAccountTo("Bob")
                                .setAmount(100)
                                .build()),
                GrpcMock.times(1)
        )
    }

    def "transfer to returns a status exception"() {
        given: "a transfer to input"
        provideInput("transferTo A Alice Bob 10000\nexit\n")

        and: "a mock server that returns a status exception"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(UserServiceGrpc.getTransferToMethod())
                        .willReturn(GrpcMock.statusException(
                                Status.FAILED_PRECONDITION
                                        .withDescription("Source account does not have sufficient balance")
                        ))
        )

        when: "the user client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == "> Error: Source account does not have sufficient balance\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(UserServiceGrpc.getTransferToMethod())
                        .withRequest(TransferToRequest
                                .newBuilder()
                                .setAccountFrom("Alice")
                                .setAccountTo("Bob")
                                .setAmount(10000)
                                .build()),
                GrpcMock.times(1)
        )
    }
}
