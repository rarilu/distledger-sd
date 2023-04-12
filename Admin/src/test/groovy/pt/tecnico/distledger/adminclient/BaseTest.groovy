package pt.tecnico.distledger.adminclient

import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger
import pt.tecnico.distledger.contract.namingserver.NamingServiceGrpc
import spock.lang.Specification

import org.grpcmock.GrpcMock

abstract class BaseTest extends Specification {
    def mockServerTarget
    def mockServerEntry
    def initialStdin
    def initialStdout
    def outBuf

    protected static final String EXPECTED_USAGE_STRING = "Usage:\n" +
        "- activate <server>\n" +
        "- deactivate <server>\n" +
        "- getLedgerState <server>\n" +
        "- gossip <server>\n" +
        "- exit\n"

    def setup() {
        GrpcMock.configureFor(GrpcMock.grpcMock(0).build().start())
        // Port 0 means that the OS will assign a random free port

        mockServerTarget = "localhost:" + GrpcMock.getGlobalPort().toString()
        mockServerEntry = NamingServerDistLedger.LookupResponse.Entry.newBuilder()
                .setQualifier("DistLedger")
                .setTarget(mockServerTarget)
                .setId(0)
                .build()
        GrpcMock.stubFor(
                GrpcMock.unaryMethod(NamingServiceGrpc.getLookupMethod())
                        .willReturn(GrpcMock.response(
                                NamingServerDistLedger.LookupResponse.newBuilder()
                                        .addEntries(mockServerEntry)
                                        .build())))

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
        def adminClient = new AdminClientMain() // Main is static, but this is needed to cover the constructor
        adminClient.main(new String[]{mockServerTarget})
    }
}
