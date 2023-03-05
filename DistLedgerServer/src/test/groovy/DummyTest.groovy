import spock.lang.Specification
class DummyTest extends Specification {
    def "one plus one should equal two"() {
        expect:
        1 + 1 == 2
    }
}