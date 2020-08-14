package com.connexta.jsonrpc;

import java.util.Map;

public interface RpcMethodFactory {

  RpcMethod createMethod(Method method, String description);

  RpcMethod createMethod(Method method, Map<String, Object> metadata);
}
