package pt.tecnico.distledger.common.domain

import spock.lang.Specification

import java.util.List

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

        and: "toList is called"
        def list = clock.toList()

        then: "the string is correct"
        string == "(1, 0, 2)"

        and: "the array is correct"
        list == List.of(1, 0, 2)
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

        when: "A is compared to B"
        def order = VectorClock.compare(a, b)

        then: "A < B"
        order == VectorClock.Order.BEFORE
    }

    def "A is equal to B but not equal to null"() {
        given: "A = (1)"
        def a = new VectorClock()
        a.increment(0)

        and: "B = (1)"
        def b = new VectorClock()
        b.increment(0)

        when: "A is compared to B"
        def order = VectorClock.compare(a, b)

        then: "A == B"
        order == VectorClock.Order.EQUAL
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

        when: "A is compared to C"
        def acOrder = VectorClock.compare(a, c)

        then: "A < C"
        acOrder == VectorClock.Order.BEFORE

        when: "C is compared to B"
        def cbOrder = VectorClock.compare(c, b)

        then: "C > B"
        cbOrder == VectorClock.Order.AFTER

        when: "A is compared to B"
        def abOrder = VectorClock.compare(a, b)

        then: "they are concurrent"
        abOrder == VectorClock.Order.CONCURRENT
    }
}
