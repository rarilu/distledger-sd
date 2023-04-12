package pt.tecnico.distledger

import pt.tecnico.distledger.server.ServerMain
import pt.tecnico.distledger.userclient.UserClientMain
import pt.tecnico.distledger.adminclient.AdminClientMain
import pt.tecnico.distledger.namingserver.NamingServer
import pt.tecnico.distledger.testing.ThreadLocalInputStream

import spock.lang.Specification
import spock.lang.Timeout

abstract class BaseIT extends Specification {
    def aPort = 2001
    def bPort = 2002

    def initialStdin
    def initialStdout
    def outBuf

    def mockStdin
    def writeNamingServerStdin
    def writeAServerStdin
    def writeBServerStdin

    def namingServerThread
    def aServerThread
    def bServerThread

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
        def readAServerStdin = new PipedInputStream()
        writeAServerStdin = new PipedOutputStream(readAServerStdin)
        def readBServerStdin = new PipedInputStream()
        writeBServerStdin = new PipedOutputStream(readBServerStdin)

        // Start the naming server and set the input stream
        namingServerThread = Thread.start {
            mockStdin.setStream(readNamingServerStdin)
            NamingServer.main(new String[] {})
        }

        // Hacky way to wait for the naming server to start
        def namingServerStartupMsg = "Naming Server started, listening on 5001\nPress enter to shutdown\n"
        while (outBuf.size() != namingServerStartupMsg.length()) {}
        outBuf.reset()

        // Start the a server and set the input stream
        aServerThread = Thread.start {
            mockStdin.setStream(readAServerStdin)
            ServerMain.main(new String[] { aPort.toString(), "A" })
        }

        // Hacky way to wait for the a server to start
        def aServerStartupMsg = "Server started, listening on " + aPort.toString() + "\nPress enter to shutdown\n"
        while (outBuf.size() != aServerStartupMsg.length()) {}
        outBuf.reset()

        // Start the b server and set the input stream
        bServerThread = Thread.start {
            mockStdin.setStream(readBServerStdin)
            ServerMain.main(new String[] { bPort.toString(), "B" })
        }

        // Hacky way to wait for the b server to start
        def bServerStartupMsg = "Server started, listening on " +
                bPort.toString() +
                "\nPress enter to shutdown\n"
        while (outBuf.size() != bServerStartupMsg.length()) {}
        outBuf.reset()
    }

    def cleanup() {
        // Send input to the servers to shutdown
        writeAServerStdin.write("\n".getBytes())
        writeBServerStdin.write("\n".getBytes())
        aServerThread.join()
        bServerThread.join()

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
        UserClientMain.main(new String[]{})
    }

    def runAdmin(String input) {
        provideInput(input)
        AdminClientMain.main(new String[]{})
    }

    def provideInput(String input) {
        mockStdin.setStream(new ByteArrayInputStream(input.getBytes()))
    }

    def extractOutput() {
        def str = outBuf.toString()
        outBuf.reset()
        return str
    }
}
