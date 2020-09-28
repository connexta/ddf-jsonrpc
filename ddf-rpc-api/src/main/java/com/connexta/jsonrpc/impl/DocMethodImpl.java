package com.connexta.jsonrpc.impl;

import com.connexta.jsonrpc.Method;
import com.connexta.jsonrpc.RpcMethod;
import java.util.Map;

public class DocMethodImpl implements RpcMethod {
  private final Method method;

  private final String docstring;

  public DocMethodImpl(Method method, String docstring) {
    this.method = method;
    this.docstring = docstring;
  }

  @Override
  public Object apply(Map<String, Object> arg) {
    return method.apply(arg);
  }

  @Override
  public String getDocstring() {
    return docstring;
  }
}
