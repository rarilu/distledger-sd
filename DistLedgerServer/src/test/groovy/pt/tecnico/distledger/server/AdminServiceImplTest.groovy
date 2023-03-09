package pt.tecnico.distledger.server

import spock.lang.Specification

import io.grpc.stub.StreamObserver
import java.util.concurrent.atomic.AtomicBoolean
import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.operation.DeleteOp
import pt.tecnico.distledger.server.domain.operation.TransferOp
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateRequest
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateResponse
import pt.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest
import pt.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse

class AdminServiceImplTest extends Specification {
    def state
    def active
    def service
    def observer

    def setup() {
        state = new ServerState()
        active = new AtomicBoolean(true)
        service = new AdminServiceImpl(state, active)
        observer = Mock(StreamObserver)
    }

    def "toggle server active"() {
        when: "server is deactivated"
        service.deactivate(DeactivateRequest.getDefaultInstance(), observer)
        
        then: "active flag is correct"
        active.get() == false

        and: "response is sent"
        1 * observer.onNext(DeactivateResponse.getDefaultInstance())

        when: "server is activated"
        service.activate(ActivateRequest.getDefaultInstance(), observer)

        then: "active flag is correct"
        active.get() == true

        and: "response is sent"
        1 * observer.onNext(ActivateResponse.getDefaultInstance())

        when: "server is deactivated again"
        service.deactivate(DeactivateRequest.getDefaultInstance(), observer)

        then: "active flag is correct"
        active.get() == false

        and: "response is sent"
        1 * observer.onNext(DeactivateResponse.getDefaultInstance())
    }

    def "get empty ledger state"() {
        given: "an expected empty ledger state"
        def ledgerState = LedgerState.getDefaultInstance()

        when: "get ledger state"
        service.getLedgerState(GetLedgerStateRequest.getDefaultInstance(), observer)

        then: "ledger state is empty"
        1 * observer.onNext(GetLedgerStateResponse.newBuilder().setLedgerState(ledgerState).build())
    }

    def "get non-empty ledger state"() {
        given: "an expected ledger state"
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
        service.getLedgerState(GetLedgerStateRequest.getDefaultInstance(), observer)

        then: "ledger state is correct"
        1 * observer.onNext(GetLedgerStateResponse.newBuilder().setLedgerState(ledgerState).build())
    }
}
