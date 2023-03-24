package pt.tecnico.distledger.adminclient

import io.grpc.Status
import org.grpcmock.GrpcMock
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateRequest
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateResponse
import pt.tecnico.distledger.contract.admin.AdminServiceGrpc

class GetLedgerStateTest extends BaseTest {
    def "user provides invalid get ledger state command"() {
        given: "an invalid get ledger state input"
        provideInput("getLedgerState wrong usage\nexit\n")

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == ("> " + EXPECTED_USAGE_STRING + "\n> ")

        and: "the mock server received no requests"
        GrpcMock.verifyThat(GrpcMock.calledMethod(AdminServiceGrpc.getGetLedgerStateMethod()), GrpcMock.never())
    }

    def "get ledger state server returns empty response"() {
        given: "a get ledger state input"
        provideInput("getLedgerState A\nexit\n")

        and: "a mock server that returns an empty response"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(AdminServiceGrpc.getGetLedgerStateMethod())
                        .willReturn(GrpcMock.response(GetLedgerStateResponse.getDefaultInstance())))

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == "> OK\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(AdminServiceGrpc.getGetLedgerStateMethod())
                        .withRequest(GetLedgerStateRequest.getDefaultInstance()),
                GrpcMock.times(1)
        )
    }

    def "get ledger state server returns non-empty response"() {
        given: "a get ledger state input"
        provideInput("getLedgerState A\nexit\n")

        and: "a mock server that returns a non-empty response"
        def operations = [
                Operation.newBuilder().setType(OperationType.OP_CREATE_ACCOUNT)
                        .setUserId("Alice")
                        .build(),
                Operation.newBuilder().setType(OperationType.OP_TRANSFER_TO)
                        .setUserId("broker")
                        .setDestUserId("Alice")
                        .setAmount(100)
                        .build(),
                Operation.newBuilder().setType(OperationType.OP_TRANSFER_TO)
                        .setUserId("Alice")
                        .setDestUserId("broker")
                        .setAmount(100)
                        .build(),
                Operation.newBuilder().setType(OperationType.OP_DELETE_ACCOUNT)
                        .setUserId("Alice")
                        .build()
        ]
        def ledgerState = LedgerState.newBuilder().addAllLedger(operations).build()
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(AdminServiceGrpc.getGetLedgerStateMethod())
                        .willReturn(GrpcMock.response(GetLedgerStateResponse.newBuilder()
                        .setLedgerState(ledgerState).build())))

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == ("> OK\n"
                + "ledgerState {\n" +
                "  ledger {\n" +
                "    type: OP_CREATE_ACCOUNT\n" +
                "    userId: \"Alice\"\n" +
                "  }\n" +
                "  ledger {\n" +
                "    type: OP_TRANSFER_TO\n" +
                "    userId: \"broker\"\n" +
                "    destUserId: \"Alice\"\n" +
                "    amount: 100\n" +
                "  }\n" +
                "  ledger {\n" +
                "    type: OP_TRANSFER_TO\n" +
                "    userId: \"Alice\"\n" +
                "    destUserId: \"broker\"\n" +
                "    amount: 100\n" +
                "  }\n" +
                "  ledger {\n" +
                "    type: OP_DELETE_ACCOUNT\n" +
                "    userId: \"Alice\"\n" +
                "  }\n" +
                "}\n"
                + "\n> ")

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(AdminServiceGrpc.getGetLedgerStateMethod())
                        .withRequest(GetLedgerStateRequest.getDefaultInstance()),
                GrpcMock.times(1)
        )
    }

    def "get ledger state returns a status exception"() {
        given: "a get ledger state input"
        provideInput("getLedgerState A\nexit\n")

        and: "a mock server that returns a status exception"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(AdminServiceGrpc.getGetLedgerStateMethod())
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
                GrpcMock.calledMethod(AdminServiceGrpc.getGetLedgerStateMethod())
                        .withRequest(GetLedgerStateRequest.getDefaultInstance()),
                GrpcMock.times(1)
        )
    }
}
