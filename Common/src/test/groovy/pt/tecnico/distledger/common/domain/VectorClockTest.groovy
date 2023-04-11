package pt.tecnico.distledger.common.domain

import pt.tecnico.distledger.common.domain.exceptions.ConcurrentVectorClocksException

import spock.lang.Specification

class VectorClockTest extends Specification {
    def "increment is done correctly"() {
        given: "a vector clock"
        def clock = new VectorClock()

        when: "increment is called multiple times"
        clock.increment(0)
        clock.increment(2)
        clock.increment(2)

        and: "toString is called"
        def string = clock.toString()

        and: "toArray is called"
        def array = clock.toArray()

        then: "the string is correct"
        string == "(1, 0, 2)"

        and: "the array is correct"
        array == [1, 0, 2]
    }

    def "merge is done correctly"() {
        given: "a vector clock A"
        def a = new VectorClock([a0, a1, a2] as int[])

        and: "a vector clock B"
        def b = new VectorClock([b0, b1, b2] as int[])

        when: "A is merged with B"
        a.merge(b)

        and: "toString is called"
        def string = a.toString()

        then: "the string is correct"
        string == expected

        where:
        a0 | a1 | a2 | b0 | b1 | b2 | expected
        0  | 0  | 0  | 0  | 0  | 0  | "()"
        0  | 4  | 0  | 2  | 0  | 3  | "(2, 4, 3)"
        1  | 0  | 0  | 0  | 0  | 9  | "(1, 0, 9)"
        3  | 2  | 1  | 1  | 2  | 3  | "(3, 2, 3)"
        1  | 0  | 0  | 0  | 1  | 0  | "(1, 1)"
    }

    def "A happens before B and B does not happen before A"() {
        given: "A = (0, 1, 0)"
        def a = new VectorClock()
        a.increment(1)

        and: "B = (0, 1, 1)"
        def b = new VectorClock()
        b.increment(1)
        b.increment(2)

        when: "they are compared"
        def order = a < b
        def eOrder = a <= b
        def revOrder = b < a
        def equal = a.equals(b)

        then: "A < B"
        order == true

        and: "A <= B"
        eOrder == true

        and: "B > A"
        revOrder == false

        and: "A != B"
        equal == false
    }

    def "A is equal to B but not equal to null"() {
        given: "A = (1)"
        def a = new VectorClock()
        a.increment(0)

        and: "B = (1)"
        def b = new VectorClock()
        b.increment(0)

        when: "A is compared to B"
        def equal = a.equals(b)

        and: "A is hashed"
        def aHash = a.hashCode()

        and: "B is hashed"
        def bHash = b.hashCode()

        then: "A == B"
        equal == true

        and: "hashes are equal"
        aHash == bHash

        when: "A is compared to null"
        equal = a.equals(null)

        then: "A != null"
        equal == false
    }

    def "A and B are concurrent and happen before C"() {
        given: "A = (1, 0, 0, 0)"
        def a = new VectorClock()
        a.increment(0)

        and: "B = (0, 1, 0, 0)"
        def b = new VectorClock()
        b.increment(1)

        and: "C = (1, 1, 0, 1)"
        def c = new VectorClock()
        c.increment(0)
        c.increment(1)
        c.increment(3)

        when: "A and C are compared"
        def aOrder = a < c

        then: "A < C"
        aOrder == true

        when: "B and C are compared"
        def bOrder = b < c

        then: "B < C"
        bOrder == true

        when: "A and B are compared"
        a < b

        then: "an exception is thrown"
        thrown ConcurrentVectorClocksException
    }
}
