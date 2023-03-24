package pt.tecnico.distledger.userclient

import io.grpc.Status
import pt.tecnico.distledger.contract.user.UserServiceGrpc
import pt.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest
import pt.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountResponse
import org.grpcmock.GrpcMock

class DeleteAccountTest extends BaseTest {
    def "user provides invalid delete account command"() {
        given: "an invalid delete account input"
        provideInput("deleteAccount wrong\nexit\n")

        when: "the user client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == ("> " + EXPECTED_USAGE_STRING + "\n> ")

        and: "the mock server received no requests"
        GrpcMock.verifyThat(GrpcMock.calledMethod(UserServiceGrpc.getDeleteAccountMethod()), GrpcMock.never())
    }

    def "delete account returns empty response"() {
        given: "a delete account input"
        provideInput("deleteAccount A Alice\nexit\n")

        and: "a mock server that returns an empty response"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(UserServiceGrpc.getDeleteAccountMethod())
                        .willReturn(GrpcMock.response(DeleteAccountResponse.getDefaultInstance())))

        when: "the user client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == "> OK\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(UserServiceGrpc.getDeleteAccountMethod())
                        .withRequest(DeleteAccountRequest
                                .newBuilder()
                                .setUserId("Alice")
                                .build()),
                GrpcMock.times(1)
        )
    }

    def "delete account returns a status exception"() {
        given: "a delete account input"
        provideInput("deleteAccount A Alice\nexit\n")

        and: "a mock server that returns a status exception"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(UserServiceGrpc.getDeleteAccountMethod())
                        .willReturn(GrpcMock.statusException(
                                Status.NOT_FOUND
                                        .withDescription("Account not found")
                        ))
        )

        when: "the user client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == "> Error: NOT_FOUND: Account not found\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(UserServiceGrpc.getDeleteAccountMethod())
                        .withRequest(DeleteAccountRequest
                                .newBuilder()
                                .setUserId("Alice")
                                .build()),
                GrpcMock.times(1)
        )
    }
}
