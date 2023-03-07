package pt.tecnico.distledger.adminclient

import spock.lang.Specification

import pt.tecnico.distledger.adminclient.grpc.AdminService;
import org.grpcmock.GrpcMock;

abstract class BaseTest extends Specification {
    def runMain 

    def initialStdin
    def initialStdout
    def initialStderr
    def outBuf
    def errBuf

    def usageString = ("> " + "Usage:\n"
        + "- activate <server>\n"
        + "- deactivate <server>\n"
        + "- getLedgerState <server>\n"
        + "- gossip <server>\n"
        + "- exit\n" + "\n> ")

    def setup() {
        GrpcMock.configureFor(GrpcMock.grpcMock(0).build().start())
        // port 0 means that the OS will assign a random free port

        runMain = () -> AdminClientMain.main(new String[]{"localhost", GrpcMock.getGlobalPort().toString()})

        initialStdin = System.in
        initialStdout = System.out
        initialStderr = System.err
        outBuf = new ByteArrayOutputStream()
        errBuf = new ByteArrayOutputStream()
        System.setOut(new PrintStream(outBuf))
        System.setErr(new PrintStream(errBuf))
    }

    def cleanup() {
        System.setIn(initialStdin)
        System.setOut(initialStdout)
        System.setErr(initialStderr)
    }

    def provideInput(String input) {
        ByteArrayInputStream mockStdin = new ByteArrayInputStream(input.getBytes())
        System.setIn(mockStdin)
    }
}
