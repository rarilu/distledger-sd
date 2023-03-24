package pt.tecnico.distledger.common.grpc

import pt.tecnico.distledger.common.grpc.exceptions.DuplicateQualifierException;
import pt.tecnico.distledger.common.grpc.exceptions.ServerNotFoundException;

import spock.lang.Specification

class StubCacheTest extends Specification {
    def "getStub throws DuplicateQualifierException"() {
        given: "a mocked naming service which returns two targets"
        def namingService = Mock(NamingService)
        namingService.lookup(*_) >> ["localhost:5001", "localhost:5002"]

        and: "a StubCache using the mocked naming service"
        def cache = new StubCache<Boolean>(namingService, x -> True)

        when: "getStub is called"
        cache.getStub("A")

        then: "a DuplicateQualifierException is thrown"
        thrown(DuplicateQualifierException)
    }

    def "getStub throws ServerNotFoundException"() {
        given: "a mocked naming service which returns no targets"
        def namingService = Mock(NamingService)
        namingService.lookup(*_) >> []

        and: "a StubCache using the mocked naming service"
        def cache = new StubCache<Boolean>(namingService, x -> True)

        when: "getStub is called"
        cache.getStub("A")

        then: "a ServerNotFoundException is thrown"
        thrown(ServerNotFoundException)
    }
}
