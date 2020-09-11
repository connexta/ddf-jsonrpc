package com.connexta.jsonrpc;

import java.util.Map;

/**
 * Represents that a class has a set of methods that can respond to rpc calls. The key of the map is
 * the unique identifier to which the method call is dispatched on (the name of the method). The
 * value is the {@link RpcMethod} that contains the functionality
 */
public interface MethodSet {
  Map<String, RpcMethod> getMethods();
}
