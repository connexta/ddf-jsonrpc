package com.connexta.jsonrpc.impl;

import static com.connexta.jsonrpc.impl.JsonRpc.VERSION;

import com.connexta.jsonrpc.Error;
import com.connexta.jsonrpc.Response;

public class ResponseImpl implements Response {
  private final Object result;

  private final Error error;

  private final Object id;

  private final String jsonrpc = VERSION;

  public ResponseImpl(Error error, Object id) {
    this.result = null;
    this.error = error;
    this.id = id;
  }

  public ResponseImpl(Object result, Object id) {
    this.result = result;
    this.error = null;
    this.id = id;
  }

  public String getJsonrpc() {
    return jsonrpc;
  }

  @Override
  public Object getResult() {
    return result;
  }

  @Override
  public Error getError() {
    return error;
  }

  @Override
  public Object getId() {
    return id;
  }
}
