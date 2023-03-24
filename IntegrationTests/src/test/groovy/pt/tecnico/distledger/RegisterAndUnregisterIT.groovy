package pt.tecnico.distledger

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import pt.tecnico.distledger.server.ServerMain
import pt.tecnico.distledger.namingserver.NamingServer
import pt.tecnico.distledger.testing.ThreadLocalInputStream

import spock.lang.Specification

class RegisterAndUnregisterIT extends Specification {
    def port = 2001

    def initialStdin
    def initialStdout
    def outBuf

    def mockStdin
    def writeNamingServerStdin
    def writeServerStdin

    def namingServerThread
    def serverThread

    def setup() {
        initialStdin = System.in
        initialStdout = System.out

        outBuf = new ByteArrayOutputStream()
        System.setOut(new PrintStream(outBuf))

        // A thread local is used to guarantee that the input is provided only to the current
        // thread - otherwise, the servers would shutdown unprompted
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

        // Start the naming server and set the input stream
        namingServerThread = Thread.start {
            mockStdin.setStream(readNamingServerStdin)
            NamingServer.main(new String[] {})
        }

        // Hacky way to wait for the naming server to start
        def namingServerStartupMsg = "Naming Server started, listening on 5001\nPress enter to shutdown\n"
        while (outBuf.size() != namingServerStartupMsg.length()) {}
        outBuf.reset()
    }

    def startServer(untilRegister) {
        def readServerStdin = new PipedInputStream()
        writeServerStdin = new PipedOutputStream(readServerStdin)

        // Start the server and set the input stream
        serverThread = Thread.start {
            mockStdin.setStream(readServerStdin)
            ServerMain.main(new String[] { port.toString(), "A" })
        }

        // Hacky way to wait for the server to start
        def serverStartupMsg = "Server started, listening on " + port + "\n"
        if (untilRegister) {
            serverStartupMsg += "Press enter to shutdown\n"
        }
        while (outBuf.size() != serverStartupMsg.length()) {}
        outBuf.reset()
    }

    def extractOutput() {
        def str = outBuf.toString()
        outBuf.reset()
        return str
    }

    def "fail to unregister the server"() {
        given: "a server is running"
        startServer(true)

        and: "the naming server is stopped"
        writeNamingServerStdin.write("\n".getBytes());
        namingServerThread.join()

        when: "the server tries to unregister"
        writeServerStdin.write("\n".getBytes());

        and: "the server is stopped"
        serverThread.join()

        then: "the server fails to unregister but closes normally"
        extractOutput() == ""
    }

    def "register and unregister a server"() {
        given: "the naming server is stopped"
        writeNamingServerStdin.write("\n".getBytes());
        namingServerThread.join()

        when: "a server is started"
        startServer(false)

        and: "the server is stopped"
        serverThread.join()

        then: "the server fails to register but closes normally"
        extractOutput() == ""
    }
}