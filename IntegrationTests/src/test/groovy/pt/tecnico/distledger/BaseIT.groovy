package pt.tecnico.distledger

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import pt.tecnico.distledger.server.ServerMain
import pt.tecnico.distledger.userclient.UserClientMain
import pt.tecnico.distledger.adminclient.AdminClientMain
import pt.tecnico.distledger.namingserver.NamingServer
import pt.tecnico.distledger.testing.ThreadLocalInputStream

import spock.lang.Specification
import spock.lang.Timeout

abstract class BaseIT extends Specification {
    def port = 2001

    def initialStdin
    def initialStdout
    def outBuf

    def mockStdin
    def writeNamingServerStdin
    def writeServerStdin

    def namingServerThread
    def serverThread

    @Timeout(5)
    def setup() {
        initialStdin = System.in
        initialStdout = System.out

        outBuf = new ByteArrayOutputStream()
        System.setOut(new PrintStream(outBuf))

        // A thread local is used to guarantee that the input is provided only to the current
        // thread - otherwise, the servers would shutdown
        mockStdin = new ThreadLocalInputStream(new ThreadLocal<InputStream>() {
            @Override
            protected InputStream initialValue() {
                return initialStdin
            }
        })
        System.setIn(mockStdin)

        // Prepare the server input streams
        def readNamingServerStdin = new PipedInputStream()
        writeNamingServerStdin = new PipedOutputStream(readNamingServerStdin)
        def readServerStdin = new PipedInputStream()
        writeServerStdin = new PipedOutputStream(readServerStdin)

        // Start the naming server and set the input stream
        namingServerThread = Thread.start {
            mockStdin.setStream(readNamingServerStdin)
            NamingServer.main(new String[] {})
        }

        // Hacky way to wait for the naming server to start
        def namingServerStartupMsg = "Naming Server started, listening on 5001\nPress enter to shutdown\n"
        while (outBuf.size() != namingServerStartupMsg.length()) {}
        outBuf.reset()

        // Start the server and set the input stream
        serverThread = Thread.start {
            mockStdin.setStream(readServerStdin)
            ServerMain.main(new String[] { port.toString(), "A" })
        }

        // Hacky way to wait for the server to start
        def serverStartupMsg = "Server started, listening on " + port.toString() + "\nPress enter to shutdown\n"
        while (outBuf.size() != serverStartupMsg.length()) {}
        outBuf.reset()
    }

    def cleanup() {
        // Send input to the server to shutdown
        writeServerStdin.write("\n".getBytes())
        serverThread.join()

        // Send input to the naming server to shutdown
        writeNamingServerStdin.write("\n".getBytes())
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
        mockStdin.setStream(new ByteArrayInputStream(input.getBytes()))
    }

    def getOutput() {
        def str = outBuf.toString()
        outBuf.reset()
        return str
    }
}