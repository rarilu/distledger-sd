# A02 DistLedger Report

This solution supports any number of replicas, and replicas joining at any time.

## Server state

Each replica stores:

-   A ledger, with operations that can be unstable, stable or failed;
-   A `valueTS` that tracks the stabilized operations in the state;
-   A `replicaTS` that tracks how many operations have been received from the client and how many operations received in other replicas it knows of.

## Balance queries

When a query is received, we check if its `prevTS` "happened-before" or is equal to the receiving replica's `valueTS`.
We respond if the condition is true, otherwise we use `wait-notify` to wait for `valueTS` to increase enough.
In the response, we send the replica's `valueTS` to be merged with the client's TS.

## Client operations

When an operation is received from the client, the receiving replica's `replicaTS` is incremented, and it is added to the ledger as unstable.
Then, if its prevTS "happened-before" or is equal to the receiving replica's `valueTS`, it is executed, stabilized, and the replica's `valueTS` is merged with the TS (calculated as explained below). If not, it remains unstable.

When a replica's `valueTS` increases, we retry to stabilize every unstable operation in the ledger.

## Gossip

When propagating, we don't send the stable operations in the ledger that have previously been sent. Keeping track of the last stable operation sent.

To exclude duplicate operations, we only add to the log if the operation's TS (calculated as explained below) has not "happened-before" and isn't equal to the receiving replica's `replicaTS`.
After, the receiving replica's replicaTS is merged with the gossip message's `replicaTS`.

It is possible that an operation can't be executed on propagation (e.g. same account created in different replicas).
That operation would be flagged as failed after stabilization.

## Duplicates

We send the receiver's `replicaTS` and `replicaID` in each operation.
As such, sending TS stops being needed, as it can be computed from `prevTS`, `replicaTS` and `replicaID` on any replica.

`replicaTS` can be used to discard duplicate operations coming from gossip, because the clients communicate with only one replica for a given operation.
We know that if the incoming operation's `replicaTS` "happened-before" or is equal to the receiving replica's `replicaTS`, it is already present in the receiving replica's log.
Therefore, we can reject the operation, removing the need for an execution table.

## Limitations

The project's requirements cause some limitations.
For instance, if client 1 runs `createAccount A Alice` and client 2 runs `transferTo B broker Alice 100`,
the transferTo operation would fail on replica B, but once propagated from B to A, it would succeed on A.
This would lead to an inconsistent state between replicas.

At first, it might look like propagating whether the operation failed solves the issue.
However, it is possible that an operation that will fail is propagated before it is executed, so we can't know warn other replicas.
In our solution we don't execute propagated failed operations, but this latter situation can't be solved.
