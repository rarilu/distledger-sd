package pt.tecnico.distledger.adminclient

import io.grpc.Status
import org.grpcmock.GrpcMock
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateRequest
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateResponse
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc

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
                        .willReturn(GrpcMock.response(getLedgerStateResponse.getDefaultInstance())))

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == "> OK\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(AdminServiceGrpc.getGetLedgerStateMethod())
                        .withRequest(getLedgerStateRequest.getDefaultInstance()),
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
        outBuf.toString() == "> Error: The server raised an exception\n\n> "

        and: "the mock server received the correct request, exactly once"
        GrpcMock.verifyThat(
                GrpcMock.calledMethod(AdminServiceGrpc.getGetLedgerStateMethod())
                        .withRequest(getLedgerStateRequest.getDefaultInstance()),
                GrpcMock.times(1)
        )
    }
}
