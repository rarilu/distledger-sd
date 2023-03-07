package pt.tecnico.distledger.userclient

import spock.lang.Specification

import pt.tecnico.distledger.userclient.grpc.UserService;
import org.grpcmock.GrpcMock;

abstract class BaseTest extends Specification {
    def initialStdin
    def initialStdout
    def initialStderr
    def outBuf
    def errBuf

    protected static final String EXPECTED_USAGE_STRING = "Usage:\n" +
        "- createAccount <server> <username>\n" +
        "- deleteAccount <server> <username>\n" +
        "- balance <server> <username>\n" +
        "- transferTo <server> <username_from> <username_to> <amount>\n" +
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
        UserClientMain.main(new String[]{"localhost", GrpcMock.getGlobalPort().toString()})
    }
}