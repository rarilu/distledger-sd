package pt.tecnico.distledger.userclient

import io.grpc.Status
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.VectorClock
import pt.tecnico.distledger.contract.user.UserServiceGrpc
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse
import org.grpcmock.GrpcMock

class BalanceTest extends BaseTest {
    def "user provides invalid balance command"() {
        given: "an invalid balance input"
        provideInput("balance wrong\nexit\n")

        when: "the user client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == ("> " + EXPECTED_USAGE_STRING + "\n> ")

        and: "the mock server received no requests"
        GrpcMock.verifyThat(GrpcMock.calledMethod(UserServiceGrpc.getBalanceMethod()), GrpcMock.never())
    }

    def "balance returns a response"() {
        given: "a balance input"
        provideInput("balance A Alice\nexit\n")

        and: "a mock server that returns a response with a certain balance"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(UserServiceGrpc.getBalanceMethod())
                        .willReturn(GrpcMock.response(BalanceResponse.newBuilder()
                                .setValue(balance)
                                .setValueTS(VectorClock
                                        .newBuilder()
                                        .addAllValues([3, 1])
                                        .build())
                                .build())))

        when: "the user client is run"
        runMain()

        then: "the output is correct"
        if (balance == 0) {
             outBuf.toString() == "> OK\n\n> "
        } else {
             outBuf.toString() == "> OK\nvalue: " + balance + "\n\n> "
        }

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(UserServiceGrpc.getBalanceMethod())
                        .withRequest(BalanceRequest
                                .newBuilder()
                                .setUserId("Alice")
                                .setPrevTS(VectorClock
                                        .newBuilder()
                                        .addAllValues([])
                                        .build()
                                )
                                .build()),
                GrpcMock.times(1)
        )

        where:
        balance << [0, 100]
    }

    def "balance returns a status exception"() {
        given: "a balance input"
        provideInput("balance A Alice\nexit\n")

        and: "a mock server that returns a status exception"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(UserServiceGrpc.getBalanceMethod())
                        .willReturn(GrpcMock.statusException(
                                Status.NOT_FOUND
                                        .withDescription("Account Alice not found")
                        ))
        )

        when: "the user client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == "> Error: NOT_FOUND: Account Alice not found\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(UserServiceGrpc.getBalanceMethod())
                        .withRequest(BalanceRequest
                                .newBuilder()
                                .setUserId("Alice")
                                .setPrevTS(VectorClock
                                        .newBuilder()
                                        .addAllValues([])
                                        .build()
                                )
                                .build()),
                GrpcMock.times(1)
        )
    }
}
