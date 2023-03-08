import spock.lang.Specification

import io.grpc.stub.StreamObserver
import pt.tecnico.distledger.server.AdminServiceImpl
import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.operation.DeleteOp
import pt.tecnico.distledger.server.domain.operation.TransferOp
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateRequest
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateResponse

class AdminServiceImplTest extends Specification {
    def state
    def service

    def setup() {
        state = new ServerState()
        service = new AdminServiceImpl(state)
    }

    def "get empty ledger state"() {
        given: "a mock observer"
        def observer = Mock(StreamObserver)

        and: "an expected empty ledger state"
        def ledgerState = LedgerState.getDefaultInstance()

        when: "get ledger state"
        service.getLedgerState(getLedgerStateRequest.getDefaultInstance(), observer)

        then: "ledger state is empty"
        1 * observer.onNext(getLedgerStateResponse.newBuilder().setLedgerState(ledgerState).build())
    }

    def "get non-empty ledger state"() {
        given: "a mock observer"
        def observer = Mock(StreamObserver)

        and: "an expected ledger state"
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

        and: "a server state with some operations"
        state.registerOperation(new CreateOp("Alice"))
        state.registerOperation(new TransferOp("broker", "Alice", 100))
        state.registerOperation(new TransferOp("Alice", "broker", 100))
        state.registerOperation(new DeleteOp("Alice"))

        when: "get ledger state"
        service.getLedgerState(getLedgerStateRequest.getDefaultInstance(), observer)

        then: "ledger state is correct"
        1 * observer.onNext(getLedgerStateResponse.newBuilder().setLedgerState(ledgerState).build())
    }
}
