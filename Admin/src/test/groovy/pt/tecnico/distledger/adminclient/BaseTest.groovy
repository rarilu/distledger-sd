package pt.tecnico.distledger.adminclient

import spock.lang.Specification

import pt.tecnico.distledger.adminclient.grpc.AdminService;
import org.grpcmock.GrpcMock;

abstract class BaseTest extends Specification {
    def initialStdin
    def initialStdout
    def initialStderr
    def outBuf
    def errBuf

    protected static final String EXPECTED_USAGE_STRING = "Usage:\n" +
        "- activate <server>\n" +
        "- deactivate <server>\n" +
        "- getLedgerState <server>\n" +
        "- gossip <server>\n" +
        "- exit\n"

    def setup() {
        GrpcMock.configureFor(GrpcMock.grpcMock(0).build().start())
        // port 0 means that the OS will assign a random free port

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

    def runMain() {
        AdminClientMain.main(new String[]{"localhost", GrpcMock.getGlobalPort().toString()})
    }
}
