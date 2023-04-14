package pt.tecnico.distledger

import pt.tecnico.distledger.server.ServerMain
import pt.tecnico.distledger.userclient.UserClientMain
import pt.tecnico.distledger.adminclient.AdminClientMain
import pt.tecnico.distledger.namingserver.NamingServer
import pt.tecnico.distledger.testing.ThreadLocalInputStream
import pt.tecnico.distledger.testing.ThreadLocalOutputStream

import spock.lang.Specification
import spock.lang.Timeout

abstract class BaseIT extends Specification {
    def baseServerPort = 2000

    def initialStdin
    def initialStdout
    def mockStdin
    def mockStdout

    def writeNamingServerStdin
    def writeServerStdins = []
    def writeAdminStdin
    def writeUserStdins = []

    def namingServerOutBuf
    def serverOutBufs = []
    def adminOutBuf
    def userOutBufs = []

    def namingServerThread
    def serverThreads = []
    def adminThread
    def userThreads = []

    @Timeout(5)
    def setup() {
        // A thread local is used to guarantee that the input is provided only to the current
        // thread - otherwise, the servers would shutdown
        initialStdin = System.in
        mockStdin = new ThreadLocalInputStream(new ThreadLocal<InputStream>() {
            @Override
            protected InputStream initialValue() {
                return initialStdin
            }
        })
        System.setIn(mockStdin)


        // A thread local is used to guarantee that the output is provided only to the current
        // thread
        initialStdout = System.out
        mockStdout = new ThreadLocalOutputStream(new ThreadLocal<OutputStream>() {
            @Override
            protected OutputStream initialValue() {
                return initialStdout
            }
        })
        System.setOut(new PrintStream(mockStdout))

        // Prepare the naming server input and output stream
        def readNamingServerStdin = new PipedInputStream()
        writeNamingServerStdin = new PipedOutputStream(readNamingServerStdin)
        namingServerOutBuf = new ByteArrayOutputStream()

        // Start the naming server and set the input and output stream
        namingServerThread = Thread.start {
            mockStdout.setStream(namingServerOutBuf)
            mockStdin.setStream(readNamingServerStdin)
            NamingServer.main(new String[] {})
        }

        // Wait for naming server to start
        while (!namingServerOutBuf.toString().endsWith("Press enter to shutdown\n")) {}
        namingServerOutBuf.reset()

        // Prepare the admin client input and output stream
        def readAdminStdin = new PipedInputStream()
        writeAdminStdin = new PipedOutputStream(readAdminStdin)
        adminOutBuf = new ByteArrayOutputStream()

        // Start the admin client and set the input and output stream
        adminThread = Thread.start {
            mockStdout.setStream(adminOutBuf)
            mockStdin.setStream(readAdminStdin)
            AdminClientMain.main(new String[]{})
        }

        // Wait for the admin client to start
        while (!adminOutBuf.toString().endsWith("> ")) {}
        adminOutBuf.reset()
    }

    def prepareServers(List<String> qualifiers) {
        prepareServers(qualifiers, true)
    }

    def prepareServers(List<String> qualifiers, boolean waitForStart) {
        for (i in 0..<qualifiers.size()) {
            // Prepare the server input and output stream
            def readServerStdin = new PipedInputStream()
            writeServerStdins << new PipedOutputStream(readServerStdin)
            serverOutBufs << new ByteArrayOutputStream()

            // Start the server and set the input and output stream
            serverThreads << Thread.start {
                mockStdout.setStream(serverOutBufs[i])
                mockStdin.setStream(readServerStdin)
                ServerMain.main(new String[] { (baseServerPort + i).toString(), qualifiers[i] })
            }
            
            // Wait for the server to start
            if (waitForStart) {
                while (!serverOutBufs[i].toString().endsWith("Press enter to shutdown\n")) {}
                serverOutBufs[i].reset()
            }
        }
    }

    def prepareUsers(int n) {
        for (i in 0..<n) {
            // Prepare the user client input and output stream
            def readUserStdin = new PipedInputStream()
            writeUserStdins << new PipedOutputStream(readUserStdin)
            userOutBufs << new ByteArrayOutputStream()

            // Start the user client and set the input and output stream
            userThreads << Thread.start {
                mockStdout.setStream(userOutBufs[i])
                mockStdin.setStream(readUserStdin)
                UserClientMain.main(new String[]{})
            }

            // Wait for the user client to start
            while (!userOutBufs[i].toString().endsWith("> ")) {}
            userOutBufs[i].reset()
        }
    }

    def stopServer(int i) {
        if (serverThreads[i].isAlive()) {
            writeServerStdins[i].write("\n".getBytes())
            serverThreads[i].join()
        }
        return serverOutBufs[i].toString()
    }

    def stopNamingServer() {
        if (namingServerThread.isAlive()) {
            writeNamingServerStdin.write("\n".getBytes())
            namingServerThread.join()
        }
        return namingServerOutBuf.toString()
    }

    def cleanup() {
        // Send input to the servers to shutdown
        for (i in 0..<serverThreads.size()) {
            stopServer(i)
        }

        // Send input to the naming server to shutdown
        stopNamingServer()

        // Shutdown admin client
        writeAdminStdin.close()
        adminThread.join()

        // Shutdown user clients
        for (i in 0..<userThreads.size()) {
            writeUserStdins[i].close()
            userThreads[i].join()
        }

        System.setIn(initialStdin)
        System.setOut(initialStdout)
    }

    def runAdmin(String input) {
        writeAdminStdin.write((input + '\n').getBytes())
        while (!adminOutBuf.toString().endsWith("\n\n> ")) {}
        def str = adminOutBuf.toString().replace("\n\n> ", "")
        adminOutBuf.reset()
        return str
    }

    def runUser(String input) {
        runUser(0, input)
    }

    def runUser(int i, String input) {
        dispatchUser(i, input)
        return waitUser(i)
    }

    def dispatchUser(String input) {
        dispatchUser(0, input)
    }

    def dispatchUser(int i, String input) {
        writeUserStdins[i].write((input + '\n').getBytes())
    }

    def waitUser() {
        return waitUser(0)
    }

    def waitUser(int i) {
        while (!userOutBufs[i].toString().endsWith("\n\n> ")) {}
        def str = userOutBufs[i].toString().replace("\n\n> ", "")
        userOutBufs[i].reset()
        return str
    }
}
