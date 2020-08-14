package com.connexta.jsonrpc;

import java.util.Map;

public interface MethodSet {
  Map<String, RpcMethod> getMethods();
}
