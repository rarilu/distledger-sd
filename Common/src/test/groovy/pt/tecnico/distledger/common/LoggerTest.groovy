package pt.tecnico.distledger.common

import spock.lang.Specification

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

    def "error prints to stderr when debug flag is clear"() {
        given: "the debug flag is clear"
        Logger.setDebugFlag(false)

        when: "an error is logged"
        Logger.error("test")

        then: "the error is printed to stderr"
        errBuf.toString() == "test\n"
    }

    def "error prints to stderr when debug flag is set"() {
        given: "the debug flag is set"
        Logger.setDebugFlag(true)

        when: "an error is logged"
        Logger.error("test")

        then: "the error is printed to stderr"
        errBuf.toString() == "test\n"
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
