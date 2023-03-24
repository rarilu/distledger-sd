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
    def primaryPort = 2001
    def secondaryPort = 2002

    def initialStdin
    def initialStdout
    def outBuf

    def mockStdin
    def writeNamingServerStdin
    def writePrimaryServerStdin
    def writeSecondaryServerStdin

    def namingServerThread
    def primaryServerThread
    def secondaryServerThread

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
        def readPrimaryServerStdin = new PipedInputStream()
        writePrimaryServerStdin = new PipedOutputStream(readPrimaryServerStdin)
        def readSecondaryServerStdin = new PipedInputStream()
        writeSecondaryServerStdin = new PipedOutputStream(readSecondaryServerStdin)

        // Start the naming server and set the input stream
        namingServerThread = Thread.start {
            mockStdin.setStream(readNamingServerStdin)
            NamingServer.main(new String[] {})
        }

        // Hacky way to wait for the naming server to start
        def namingServerStartupMsg = "Naming Server started, listening on 5001\nPress enter to shutdown\n"
        while (outBuf.size() != namingServerStartupMsg.length()) {}
        outBuf.reset()

        // Start the primary server and set the input stream
        primaryServerThread = Thread.start {
            mockStdin.setStream(readPrimaryServerStdin)
            ServerMain.main(new String[] { primaryPort.toString(), "A" })
        }

        // Hacky way to wait for the primary server to start
        def primaryServerStartupMsg = "Server started, listening on " + primaryPort.toString() + "\nPress enter to shutdown\n"
        while (outBuf.size() != primaryServerStartupMsg.length()) {}
        outBuf.reset()

        // Start the secondary server and set the input stream
        secondaryServerThread = Thread.start {
            mockStdin.setStream(readSecondaryServerStdin)
            ServerMain.main(new String[] { secondaryPort.toString(), "B" })
        }

        // Hacky way to wait for the secondary server to start
        def secondaryServerStartupMsg = "Server started, listening on " +
                secondaryPort.toString() +
                "\nPress enter to shutdown\n"
        while (outBuf.size() != secondaryServerStartupMsg.length()) {}
        outBuf.reset()
    }

    def cleanup() {
        // Send input to the servers to shutdown
        writePrimaryServerStdin.write("\n".getBytes())
        writeSecondaryServerStdin.write("\n".getBytes())
        primaryServerThread.join()
        secondaryServerThread.join()

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
