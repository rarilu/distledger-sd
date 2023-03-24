![Coverage](.github/badges/jacoco.svg)

# DistLedger

Distributed Systems Project 2022/2023

## Authors

**Group A02**

### Team Members

| Number | Name            | User                              | Email                                             |
|--------|-----------------|-----------------------------------|---------------------------------------------------|
| 99266  | Luís Fonseca    | <https://github.com/luishfonseca> | <mailto:luis.h.fonseca@tecnico.ulisboa.pt>        |
| 99311  | Rafael Oliveira | <https://github.com/RafDevX>      | <mailto:rafael.serra.oliveira@tecnico.ulisboa.pt> |
| 99316  | Ricardo Antunes | <https://github.com/RiscadoA>     | <mailto:ricardo.g.antunes@tecnico.ulisboa.pt>     |

## Getting Started

The overall system is made up of several modules. The main server is the _DistLedgerServer_. The clients are the _User_ 
and the _Admin_. The definition of messages and services is in the _Contract_. The naming server is the _NamingServer_.
Code common to more than one module is in the _Common_ module.

See the [Project Statement](https://github.com/tecnico-distsys/DistLedger) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run:

```s
javac -version
mvn -version
```

### Installation

To compile and install all modules, run:

```s
mvn clean install
```

### Run

To execute a module, from the root directory of the project, run:

#### User

```s
mvn exec:java -pl User
```

#### Admin

```s
mvn exec:java -pl Admin
```

#### DistLedgerServer

```s
mvn exec:java -pl DistLedgerServer -Dexec.args="<port> <qual>"
```

Omitting `-Dexec.args` will run with the default arguments `"2001 A"`

#### NamingServer

```s
mvn exec:java -pl NamingServer
```

#### Running in Debug Mode

To execute any of the modules in debug mode, add `-Ddebug` to the previous commands

### Test

To test all modules, run:

```s
mvn clean verify
```

This will fail before running the tests if there are formatting errors, see [Format](#format).

#### Test Coverage

For each module, 3 JaCoCo coverage reports will be generated and saved on `<module>/target/site`:
- `target/site/jacoco` will have the coverage report of only Unit Tests.
- `target/site/jacoco-it` will have the coverage report of only Integration Tests (only created when there are Integration Tests).
- `target/site/jacoco-merged` will have the previous two reports merged.

Additionally, aggregated versions of each report (with the coverage of all the modules) will be generated and saved on `CoverageReport/target/site`.

To open a coverage report, open it directly with your browser, or run:

```s
xdg-open CoverageReport/target/site/jacoco-merged/index.html
```

### Format

To format all modules, run:

```s
mvn com.spotify.fmt:fmt-maven-plugin:format
```

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
