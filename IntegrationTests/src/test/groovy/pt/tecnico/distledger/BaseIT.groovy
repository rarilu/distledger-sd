package pt.tecnico.distledger

import pt.tecnico.distledger.server.ServerMain
import pt.tecnico.distledger.userclient.UserClientMain
import pt.tecnico.distledger.adminclient.AdminClientMain

import spock.lang.Specification
import spock.lang.Timeout

abstract class BaseIT extends Specification {
    def port = 2001

    def initialStdin
    def initialStdout
    def outBuf

    def namingServerThread
    def serverThread

    @Timeout(5)
    def setup() {
        initialStdin = System.in
        initialStdout = System.out

        outBuf = new ByteArrayOutputStream()
        System.setOut(new PrintStream(outBuf))

        namingServerThread = Thread.start {
            NamingServer.main(new String[] {})
        }

        // hacky way to wait for the naming server to start
        def namingServerStartupMsg = "Naming Server started, listening on 5001\n"
        while (outBuf.size() != namingServerStartupMsg.length()) {}
        outBuf.reset()

        serverThread = Thread.start {
            ServerMain.main(new String[] { port.toString(), "A" })
        }

        // hacky way to wait for the server to start
        def serverStartupMsg = "Server started, listening on " + port.toString() + "\n"
        while (outBuf.size() != serverStartupMsg.length()) {}
        outBuf.reset()
    }

    def cleanup() {
        serverThread.interrupt()
        serverThread.join()

        namingServerThread.interrupt()
        namingServerThread.join()

        System.setIn(initialStdin)
        System.setOut(initialStdout)
    }

    def prepareUser(String input) {
        runUser(input)
        outBuf.reset()
    }

    def prepareAdmin(String input) {
        runAdmin(input)
        outBuf.reset()
    }

    def runUser(String input) {
        provideInput(input)
        UserClientMain.main(new String[]{"localhost", port.toString()})
    }

    def runAdmin(String input) {
        provideInput(input)
        AdminClientMain.main(new String[]{"localhost", port.toString()})
    }

    def provideInput(String input) {
        ByteArrayInputStream mockStdin = new ByteArrayInputStream(input.getBytes())
        System.setIn(mockStdin)
    }

    def getOutput() {
        def str = outBuf.toString()
        outBuf.reset()
        return str
    }
}