package pt.tecnico.distledger.server

import spock.lang.Specification

import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import java.util.concurrent.atomic.AtomicBoolean
import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.operation.DeleteOp
import pt.tecnico.distledger.server.domain.operation.TransferOp
import pt.tecnico.distledger.server.visitors.StandardOperationExecutor
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateRequest
import pt.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateResponse
import pt.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest
import pt.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ShutdownRequest
import pt.tecnico.distledger.contract.admin.AdminDistLedger.ShutdownResponse

class AdminServiceImplTest extends Specification {
    def executor
    def active
    def service
    def observer

    def setup() {
        def state = new ServerState()
        executor = new StandardOperationExecutor(state)
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
        executor.execute(new CreateOp("Alice"))
        executor.execute(new TransferOp("broker", "Alice", 100))
        executor.execute(new TransferOp("Alice", "broker", 100))
        executor.execute(new DeleteOp("Alice"))

        when: "get ledger state"
        service.getLedgerState(GetLedgerStateRequest.getDefaultInstance(), observer)

        then: "ledger state is correct"
        1 * observer.onNext(GetLedgerStateResponse.newBuilder().setLedgerState(ledgerState).build())
    }

    def "try to shutdown without passing the server"() {
        when: "shutdown is called"
        service.shutdown(ShutdownRequest.getDefaultInstance(), observer)

        then: "method fails with UNIMPLEMENTED"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "UNIMPLEMENTED: Server cannot be shutdown"
        })
    }

    def "shutdown successfully"() {
        given: "a mock server which is passed to the service"
        def server = Mock(io.grpc.Server)
        service.setServer(server)

        when: "shutdown is called"
        service.shutdown(ShutdownRequest.getDefaultInstance(), observer)

        then: "server is shutdown"
        1 * server.shutdown()

        and: "response is sent"
        1 * observer.onNext(ShutdownResponse.getDefaultInstance())
    }

    def "catch runtime exceptions"() {
        given: "an observer that throws an exception when onNext is called"
        observer.onNext(_) >> { throw new RuntimeException("Unknown error") }

        and: "a mock server which is passed to the service"
        service.setServer(Mock(io.grpc.Server))

        when: "a method is called"
        method.invoke(service, method.getParameterTypes()[0].getDefaultInstance(), observer)

        then: "method fails with RuntimeException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "UNKNOWN: Unknown error"
        })

        where: "method is any void function of AdminServiceImpl with 2 parameters"
        method << AdminServiceImpl.class.getDeclaredMethods().findAll { it.getReturnType() == void.class && it.getParameterCount() == 2 }
    }
}
