package pt.tecnico.distledger.userclient

import io.grpc.Status
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountResponse
import org.grpcmock.GrpcMock;

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
                                Status.ALREADY_EXISTS
                                        .withDescription("Account already exists")
                        ))
        )

        when: "the user client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == "> Error: Account already exists\n\n> "

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
