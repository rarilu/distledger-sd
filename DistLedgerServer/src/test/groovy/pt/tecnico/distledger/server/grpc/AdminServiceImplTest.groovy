package pt.tecnico.distledger.server.grpc

import spock.lang.Specification

import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import java.util.concurrent.atomic.AtomicBoolean
import pt.tecnico.distledger.common.domain.VectorClock
import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.operation.TransferOp
import pt.tecnico.distledger.server.visitors.OperationExecutor
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateRequest
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateResponse
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GossipRequest
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GossipResponse
import pt.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest
import pt.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse
import pt.tecnico.distledger.common.grpc.NamingService

class AdminServiceImplTest extends Specification {
    def state
    def active
    def namingService
    def crossServerService
    def service
    def observer

    def setup() {
        state = new ServerState(0)
        active = new AtomicBoolean(true)

        namingService = Mock(NamingService)
        namingService.lookup(*_) >> []

        crossServerService = new CrossServerService(namingService)
        service = new AdminServiceImpl(state, active, crossServerService)
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
                        .setPrevTS(DistLedgerCommonDefinitions.VectorClock.getDefaultInstance())
                        .setTS(DistLedgerCommonDefinitions.VectorClock.getDefaultInstance())
                        .setStable(true)
                        .build(),
                Operation.newBuilder().setType(OperationType.OP_TRANSFER_TO)
                        .setUserId("broker")
                        .setDestUserId("Alice")
                        .setAmount(100)
                        .setPrevTS(DistLedgerCommonDefinitions.VectorClock.getDefaultInstance())
                        .setTS(DistLedgerCommonDefinitions.VectorClock.getDefaultInstance())
                        .setStable(true)
                        .build(),
                Operation.newBuilder().setType(OperationType.OP_TRANSFER_TO)
                        .setUserId("Alice")
                        .setDestUserId("broker")
                        .setAmount(100)
                        .setPrevTS(DistLedgerCommonDefinitions.VectorClock.getDefaultInstance())
                        .setTS(DistLedgerCommonDefinitions.VectorClock.getDefaultInstance())
                        .setStable(true)
                        .build(),
        ]
        def ledgerState = LedgerState.newBuilder().addAllLedger(operations).build()

        and: "a server state with some operations"
        state.addToLedger(new CreateOp("Alice", new VectorClock(), new VectorClock()), true)
        state.addToLedger(new TransferOp("broker", "Alice", 100, new VectorClock(), new VectorClock()), true)
        state.addToLedger(new TransferOp("Alice", "broker", 100, new VectorClock(), new VectorClock()), true)

        when: "get ledger state"
        service.getLedgerState(GetLedgerStateRequest.getDefaultInstance(), observer)

        then: "ledger state is correct"
        1 * observer.onNext(GetLedgerStateResponse.newBuilder().setLedgerState(ledgerState).build())
    }

    def "catch runtime exceptions"() {
        given: "an observer that throws an exception when onNext is called"
        observer.onNext(_) >> { throw new RuntimeException("Unknown error") }

        when: "a method is called"
        method.invoke(service, method.getParameterTypes()[0].getDefaultInstance(), observer)

        then: "method fails with RuntimeException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "UNKNOWN: Unknown error"
        })

        where: "method is any void function of AdminServiceImpl with 2 parameters"
        method << AdminServiceImpl.class.getDeclaredMethods().findAll{
            it.getReturnType() == void.class && it.getParameterCount() == 2 && !it.isSynthetic()
        }
    }
}
