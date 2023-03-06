package pt.tecnico.distledger.userclient

import io.grpc.Status
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse
import org.grpcmock.GrpcMock;

class CreateAccountTest extends BaseTest {
    def "user provides invalid create account command"() {
        given: "an invalid create account input"
        provideInput("createAccount wrong\nexit\n")

        when: "the input is parsed"
        commandParser.parseInput()

        then: "the output is correct"
        outBuf.toString() == ("> " + "Usage:\n"
        + "- createAccount <server> <username>\n"
        + "- deleteAccount <server> <username>\n"
        + "- balance <server> <username>\n"
        + "- transferTo <server> <username_from> <username_to> <amount>\n"
        + "- exit\n" + "\n> ")

        and: "the mock server received no requests"
        GrpcMock.verifyThat(GrpcMock.calledMethod(UserServiceGrpc.getCreateAccountMethod()), GrpcMock.never())
    }

    def "create account returns empty response"() {
        given: "a create account input"
        provideInput("createAccount A Alice\nexit\n")

        and: "a mock server that returns an empty response"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(UserServiceGrpc.getCreateAccountMethod())
                        .willReturn(GrpcMock.response(CreateAccountResponse.getDefaultInstance())))

        when: "the input is parsed"
        commandParser.parseInput()

        then: "the output is correct"
        outBuf.toString() == "> OK\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(UserServiceGrpc.getCreateAccountMethod())
                        .withRequest(CreateAccountRequest
                                .newBuilder()
                                .setUserId("Alice")
                                .build()),
                GrpcMock.times(1)
        )
    }

    def "create account returns a status exception"() {
        given: "a create account input"
        provideInput("createAccount A Alice\nexit\n")

        and: "a mock server that returns a status exception"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(UserServiceGrpc.getCreateAccountMethod())
                        .willReturn(GrpcMock.statusException(
                                Status.ALREADY_EXISTS
                                        .withDescription("Account already exists")
                        ))
        )

        when: "the input is parsed"
        commandParser.parseInput()

        then: "the output is correct"
        outBuf.toString() == "> Error: Account already exists\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(UserServiceGrpc.getCreateAccountMethod())
                        .withRequest(CreateAccountRequest
                                .newBuilder()
                                .setUserId("Alice")
                                .build()),
                GrpcMock.times(1)
        )
    }
}
