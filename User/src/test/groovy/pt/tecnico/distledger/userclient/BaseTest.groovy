package pt.tecnico.distledger.userclient

import spock.lang.Specification

import org.grpcmock.GrpcMock;

abstract class BaseTest extends Specification {
    def initialStdin
    def initialStdout
    def outBuf

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

        outBuf = new ByteArrayOutputStream()
        System.setOut(new PrintStream(outBuf))
    }

    def cleanup() {
        System.setIn(initialStdin)
        System.setOut(initialStdout)
    }

    def provideInput(String input) {
        ByteArrayInputStream mockStdin = new ByteArrayInputStream(input.getBytes())
        System.setIn(mockStdin)
    }

    def runMain() {
        UserClientMain.main(new String[]{"localhost", GrpcMock.getGlobalPort().toString()})
    }
}
