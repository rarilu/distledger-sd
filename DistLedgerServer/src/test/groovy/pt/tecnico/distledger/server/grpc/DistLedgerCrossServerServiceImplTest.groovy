package pt.tecnico.distledger.server.grpc

import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver

import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicBoolean

import pt.tecnico.distledger.common.domain.VectorClock
import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation
import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest
import pt.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateResponse

import spock.lang.Specification

class DistLedgerCrossServerServiceImplTest extends Specification {
    def state
    def active
    def crossServerService
    def service
    def observer

    def setup() {
        state = new ServerState(0)
        active = new AtomicBoolean(true)
        crossServerService = Mock(CrossServerService)
        service = new DistLedgerCrossServerServiceImpl(state, active, crossServerService)
        observer = Mock(StreamObserver)
    }

    def "propagate state"() {
        given: "a state to propagate"
        def operations = [
                // The operation below will only be executed after the account is created
                Operation.newBuilder().setType(OperationType.OP_TRANSFER_TO)
                        .setUserId("broker")
                        .setDestUserId("Alice")
                        .setAmount(100)
                        .setPrevTS(DistLedgerCommonDefinitions.VectorClock.newBuilder().addValues(1).build())
                        .setTS(DistLedgerCommonDefinitions.VectorClock.newBuilder().addValues(0).addValues(1).build())
                        .build(),
                Operation.newBuilder().setType(OperationType.OP_CREATE_ACCOUNT)
                        .setUserId("Alice")
                        .setPrevTS(DistLedgerCommonDefinitions.VectorClock.getDefaultInstance())
                        .setTS(DistLedgerCommonDefinitions.VectorClock.newBuilder().addValues(1).build())
                        .build(),
                Operation.newBuilder().setType(OperationType.OP_CREATE_ACCOUNT)
                        .setUserId("Bob")
                        .setPrevTS(DistLedgerCommonDefinitions.VectorClock.getDefaultInstance())
                        .setTS(DistLedgerCommonDefinitions.VectorClock.newBuilder().addValues(2).build())
                        .build(),
                // The operation below has a previous timestamp that is not in the ledger, will be pending
                Operation.newBuilder().setType(OperationType.OP_TRANSFER_TO)
                        .setUserId("Alice")
                        .setDestUserId("broker")
                        .setAmount(50)
                        .setPrevTS(DistLedgerCommonDefinitions.VectorClock.newBuilder().addValues(10).build())
                        .setTS(DistLedgerCommonDefinitions.VectorClock.newBuilder().addValues(4).build())
                        .build(),
                // The operation below will fail
                Operation.newBuilder().setType(OperationType.OP_CREATE_ACCOUNT)
                        .setUserId("Alice")
                        .setPrevTS(DistLedgerCommonDefinitions.VectorClock.newBuilder().addValues(1).build())
                        .setTS(DistLedgerCommonDefinitions.VectorClock.newBuilder().addValues(5).build())
                        .build()
        ]
        def prop = LedgerState.newBuilder().addAllLedger(operations).build()

        when: "the state is propagated"
        service.propagateState(PropagateStateRequest.newBuilder().setState(prop).build(), observer)

        then: "the response is received"
        1 * observer.onNext(PropagateStateResponse.getDefaultInstance())

        and: "the account balances are correct"
        state.getAccounts().size() == 3
        state.getAccountBalance("Alice", new VectorClock()).value() == 100
        state.getAccountBalance("Bob", new VectorClock()).value() == 0
        state.getAccountBalance("broker", new VectorClock()).value() == 900

        and: "the ledger is correct"
        state.ledger.size() == 5
        state.ledger[0].isStable() && !state.ledger[0].hasFailed()
        state.ledger[1].isStable() && !state.ledger[1].hasFailed()
        state.ledger[2].isStable() && state.ledger[2].hasFailed()
        state.ledger[3].isStable() && !state.ledger[3].hasFailed()
        !state.ledger[4].isStable() && !state.ledger[4].hasFailed()
    }

    def "propagate state with invalid operation"() {
        given: "a state to propagate"
        def operations = [
                Operation.newBuilder().setType(OperationType.OP_UNSPECIFIED).build()
        ]
        def prop = LedgerState.newBuilder().addAllLedger(operations).build()

        when: "the state is propagated"
        service.propagateState(PropagateStateRequest.newBuilder().setState(prop).build(), observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof StatusRuntimeException
                    && it.getMessage() == "INVALID_ARGUMENT: Failed to create operation from request"
        })
    }

    def "catch runtime exceptions"() {
        given: "a mocked observer that always throws"
        observer.onNext(_) >> { throw new RuntimeException("Unknown error") }

        when: "a method is called"
        method.invoke(service, method.getParameterTypes()[0].getDefaultInstance(), observer)

        then: "method fails with RuntimeException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "UNKNOWN: Unknown error"
        })

        where: "method is any public void function of UserServiceImpl"
        method << DistLedgerCrossServerServiceImpl.class.getDeclaredMethods().findAll {
            it.getReturnType() == void.class && Modifier.isPublic(it.getModifiers())
        }
    }

    def "deactivate server"() {
        when: "server is deactivated"
        active.set(false)

        and: "a method is called"
        method.invoke(service, method.getParameterTypes()[0].getDefaultInstance(), observer)

        then: "method fails with ServerUnavailableException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "UNAVAILABLE: Server is unavailable"
        })

        where: "method is any void function of UserServiceImpl"
        method << DistLedgerCrossServerServiceImpl.class.getDeclaredMethods().findAll {
            it.getReturnType() == void.class && Modifier.isPublic(it.getModifiers())
        }
    }
}
