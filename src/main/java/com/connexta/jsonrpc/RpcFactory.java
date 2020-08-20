package com.connexta.jsonrpc;

import java.util.Map;

public interface RpcFactory {

  RpcMethod method(Method method, String description);

  RpcMethod method(Method method, Map<String, Object> metadata);

  Error error(int code, String message);

  Error error(int code, String message, Object data);

  Response response(Error error, Object id);

  Response response(Object result, Object id);
}
