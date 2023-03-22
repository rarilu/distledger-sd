package pt.tecnico.distledger.server

import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver

import java.util.concurrent.atomic.AtomicBoolean
import java.util.List;

import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.operation.TransferOp
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateResponse

import spock.lang.Specification

class DistLedgerCrossServerServiceImplTest extends Specification {
    def state
    def active
    def service
    def observer

    def setup() {
        state = new ServerState()
        active = new AtomicBoolean(true)
        service = new DistLedgerCrossServerServiceImpl(state, active)
        observer = Mock(StreamObserver)
    }

    def "propagate state to clean state"() {
        given: "a state to propagate"
        def operations = [
                Operation.newBuilder().setType(OperationType.OP_CREATE_ACCOUNT)
                        .setUserId("Alice")
                        .build(),
                Operation.newBuilder().setType(OperationType.OP_CREATE_ACCOUNT)
                        .setUserId("Bob")
                        .build(),
                Operation.newBuilder().setType(OperationType.OP_TRANSFER_TO)
                        .setUserId("broker")
                        .setDestUserId("Alice")
                        .setAmount(100)
                        .build(),
                Operation.newBuilder().setType(OperationType.OP_DELETE_ACCOUNT)
                        .setUserId("Bob")
                        .build()
        ]
        def prop = LedgerState.newBuilder().addAllLedger(operations).build()

        when: "the state is propagated"
        service.propagateState(PropagateStateRequest.newBuilder().setState(prop).build(), observer);

        then: "the response is received"
        1 * observer.onNext(PropagateStateResponse.getDefaultInstance())

        and: "the account balances are correct"
        state.getAccounts().size() == 2
        state.getAccountBalance("Alice") == 100
    }

    def "propagate state"() {
        given: "a state to propagate"
        def operations = [
                Operation.newBuilder().setType(OperationType.OP_CREATE_ACCOUNT)
                        .setUserId("Alice")
                        .build(),
                Operation.newBuilder().setType(OperationType.OP_TRANSFER_TO)
                        .setUserId("broker")
                        .setDestUserId("Alice")
                        .setAmount(100)
                        .build()
        ]
        def prop = LedgerState.newBuilder().addAllLedger(operations).build()

        and: "a state with accounts"
        state.getAccounts().put("Alice", 200)
        state.getAccounts().put("Bob", 100)

        when: "the state is propagated"
        service.propagateState(PropagateStateRequest.newBuilder().setState(prop).build(), observer);

        then: "the response is received"
        1 * observer.onNext(PropagateStateResponse.getDefaultInstance())

        and: "the account balances are correct"
        state.getAccounts().size() == 2
        state.getAccountBalance("Alice") == 100
    }

    def "propagate state to deactivated server"() {
        given: "a state to propagate"
        def prop = LedgerState.newBuilder().addAllLedger([]).build()

        when: "server is deactivated"
        active.set(false)

        and: "a state is propagated"
        service.propagateState(PropagateStateRequest.newBuilder().setState(prop).build(), observer);

        then: "propagation fails with ServerUnavailableException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "UNAVAILABLE: Server is unavailable"
        })
    }

    def "propagate state to deactivated server"() {
        given: "a state to propagate"
        def operations = [
                Operation.newBuilder().setType(OperationType.OP_UNSPECIFIED).build()
        ]
        def prop = LedgerState.newBuilder().addAllLedger(operations).build()

        when: "a state is propagated"
        service.propagateState(PropagateStateRequest.newBuilder().setState(prop).build(), observer)

        then: "propagation fails with ServerUnavailableException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "INVALID_ARGUMENT: Failed to create operation from request"
        })
    }
}