## DDF [JSON RPC](https://www.jsonrpc.org/specification) API

This part of the system is intended to expose DDF capabilities to the
network. It should have the following properties:

- **Direct.** No embedded Intrigue specific business logic, just
  **expose** DDF APIs.
- **Simple.** Methods should do one thing and let consumers compose
  methods together to enable more complex behaviors.
- **Transport agnostic.** Should be independent of HTTP as it allows us to
  run the API over multiple transports (HTTP/WS/...).
- **Batchable.** [batching](https://www.jsonrpc.org/specification#batch)
  to enable more efficient communication when possible.
- **Documented.** The [list-methods](https://localhost:8993/direct) rpc
  method should list out all known methods.

## Installing on different versions

Currently there is support for 2.19 and 2.25+ (this may run on versions between, but has not been tested).
In order to install a specific version into a running distribution, build or install these artifacts locally, then run:

`feature:repo-add mvn:com.connexta.jsonrpc/jsonrpc-features/0.7-SNAPSHOT/xml/features`


This will add the two features for the previously mentioned features. Run `feature:install jsonrpc-2.19` or `feature:install jsonrpc-2.25` depending on which version you need.
