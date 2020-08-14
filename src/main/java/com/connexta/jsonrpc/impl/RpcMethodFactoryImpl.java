package com.connexta.jsonrpc.impl;

import com.connexta.jsonrpc.Method;
import com.connexta.jsonrpc.RpcMethod;
import com.connexta.jsonrpc.RpcMethodFactory;
import java.util.Map;

public class RpcMethodFactoryImpl implements RpcMethodFactory {

  @Override
  public RpcMethod createMethod(Method method, String description) {
    return new DocMethodImpl(method, description);
  }

  @Override
  public RpcMethod createMethod(Method method, Map<String, Object> metadata) {
    return null;
  }
}
