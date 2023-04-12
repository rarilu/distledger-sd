package pt.tecnico.distledger.userclient

import org.grpcmock.GrpcMock
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions
import pt.tecnico.distledger.contract.user.UserDistLedger
import pt.tecnico.distledger.contract.user.UserServiceGrpc

class TimestampTest extends BaseTest {
    def "timestamp output is correct"() {
        given: "an input with a series of commands interleaved with timestamp commands"
        provideInput("timestamp\n" +
                "createAccount A Alice\n" +
                "timestamp\n" +
                "transferTo B broker Alice 100\n" +
                "timestamp\n" +
                "balance B Alice\n" +
                "timestamp\n" +
                "exit\n");

        and: "a mock server that returns responses with sample values"
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(UserServiceGrpc.getCreateAccountMethod())
                        .willReturn(GrpcMock.response(UserDistLedger.CreateAccountResponse.newBuilder()
                                .setValueTS(DistLedgerCommonDefinitions.VectorClock
                                        .newBuilder()
                                        .addAllValues([5])
                                        .build())
                                .build())))
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(UserServiceGrpc.getTransferToMethod())
                        .willReturn(GrpcMock.response(UserDistLedger.TransferToResponse.newBuilder()
                                .setValueTS(DistLedgerCommonDefinitions.VectorClock
                                        .newBuilder()
                                        .addAllValues([5, 1])
                                        .build())
                                .build())))
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(UserServiceGrpc.getBalanceMethod())
                        .willReturn(GrpcMock.response(UserDistLedger.BalanceResponse.newBuilder()
                                .setValue(100)
                                .setValueTS(DistLedgerCommonDefinitions.VectorClock
                                        .newBuilder()
                                        .addAllValues([3, 5])
                                        .build())
                                .build())))

        when: "the user client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() ==
                "> OK\ntimestamp: ()\n\n" +
                "> OK\n\n" +
                "> OK\ntimestamp: (5)\n\n" +
                "> OK\n\n" +
                "> OK\ntimestamp: (5, 1)\n\n" +
                "> OK\nvalue: 100\n\n" +
                "> OK\ntimestamp: (5, 5)\n" +
                "\n> "
    }
}
