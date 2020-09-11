package com.connexta.jsonrpc.impl;

import com.connexta.jsonrpc.Method;
import com.connexta.jsonrpc.RpcMethod;
import java.util.Map;

public class MetadataMethodImpl implements RpcMethod {
  private final Method method;
  private final Map<String, Object> metadata;

  public MetadataMethodImpl(Method method, Map<String, Object> metadata) {
    this.method = method;
    this.metadata = metadata;
  }

  @Override
  public Object apply(Map<String, Object> arg) {
    return method.apply(arg);
  }

  @Override
  public Map<String, Object> getMetadata() {
    return metadata;
  }
}
