package pt.tecnico.distledger.server

import spock.lang.Specification

class ServerMainTest extends Specification {
    def outBuf
    def initialStdout

    def setup() {
        initialStdout = System.out
        outBuf = new ByteArrayOutputStream()
        System.setOut(new PrintStream(outBuf))
    }

    def cleanup() {
        System.setOut(initialStdout)
    }

    def "main is called with no arguments"() {
        when: "the server is run with no arguments"
        def server = new ServerMain() // main is static, but this is needed to cover the constructor
        server.main(new String[]{})

        then: "the output is empty"
        outBuf.toString() == ""
    }
}
