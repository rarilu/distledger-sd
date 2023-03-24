package pt.tecnico.distledger.common

import spock.lang.Specification
import spock.lang.Unroll

class LoggerTest extends Specification {
    def initialStderr
    def errBuf

    def setup() {
        initialStderr = System.err
        errBuf = new ByteArrayOutputStream()
        System.setErr(new PrintStream(errBuf))
    }

    def cleanup() {
        System.setErr(initialStderr)
    }

    static {
        // Magic: gives us 100% coverage :)
        System.setProperty("debug", "true")
    }

    @Unroll
    def "error prints to stderr independently of debug flag"() {
        given: "the debug flag is clear"
        Logger.setDebugFlag(flag)

        when: "an error is logged"
        Logger.error("test")

        then: "the error is printed to stderr"
        errBuf.toString() == "test\n"

        where:
        flag << [true, false]
    }

    def "debug does not print to stderr when debug flag is clear"() {
        given: "the debug flag is clear"
        Logger.setDebugFlag(false)

        when: "a debug message is logged"
        Logger.debug("test")

        then: "the debug message is not printed to stderr"
        errBuf.toString() == ""
    }

    def "debug prints to stderr when debug flag is set"() {
        given: "the debug flag is set"
        Logger.setDebugFlag(true)

        when: "a debug message is logged"
        Logger.debug("test")

        then: "the debug message is printed to stderr"
        errBuf.toString() == "test\n"
    }
}
