package com.connexta.jsonrpc.impl;

import com.connexta.jsonrpc.Error;
import com.connexta.jsonrpc.Method;
import com.connexta.jsonrpc.Response;
import com.connexta.jsonrpc.RpcFactory;
import com.connexta.jsonrpc.RpcMethod;
import java.util.Map;

public class RpcFactoryImpl implements RpcFactory {

  @Override
  public RpcMethod method(Method method, String description) {
    return new DocMethodImpl(method, description);
  }

  @Override
  public RpcMethod method(Method method, Map<String, Object> metadata) {
    return new MetadataMethodImpl(method, metadata);
  }

  @Override
  public Error error(int code, String message) {
    return new ErrorImpl(code, message);
  }

  @Override
  public Error error(int code, String message, Object data) {
    return new ErrorImpl(code, message, data);
  }

  @Override
  public Response response(Error error, Object id) {
    return new ResponseImpl(error, id);
  }

  @Override
  public Response response(Object object, Object id) {
    return new ResponseImpl(object, id);
  }
}
