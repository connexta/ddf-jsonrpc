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
