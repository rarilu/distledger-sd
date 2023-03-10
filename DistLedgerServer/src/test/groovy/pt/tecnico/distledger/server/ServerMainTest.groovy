package pt.tecnico.distledger.server

import spock.lang.Specification

class ServerMainTest extends Specification {
    def port = 2001
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

    def "server is interrupted"() {
        given: "a running server"
        def server = new ServerMain()
        def thread = Thread.start {
            server.main(new String[]{port.toString(), "A"})
        }

        when: "the server is interrupted after 1 second"
        sleep(1)
        thread.interrupt()
        thread.join()

        then: "the server output is correct"
        outBuf.toString() == "Server started, listening on " + port.toString() + "\n" +
                             "Server interrupted, shutting down\n"
    }
}
