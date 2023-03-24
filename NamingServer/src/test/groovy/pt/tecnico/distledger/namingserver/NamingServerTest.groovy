package pt.tecnico.distledger.namingserver

import spock.lang.Specification

class NamingServerTest extends Specification {
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
        when: "the naming server is run with no arguments"
        def namingServer = new NamingServer() // main is static, but this is needed to cover the constructor
        namingServer.main(new String[]{})

        then: "the output is empty"
        outBuf.toString() == "Naming Server started, listening on 5001\nPress enter to shutdown\n"
    }
}
