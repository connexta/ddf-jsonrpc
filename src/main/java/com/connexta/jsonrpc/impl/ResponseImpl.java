package com.connexta.jsonrpc.impl;

import static com.connexta.jsonrpc.impl.JsonRpc.VERSION;

import com.connexta.jsonrpc.Response;

public class ResponseImpl implements Response {
  public final String jsonrpc = VERSION;
  public final Object result;

  public final ErrorImpl error;

  public final Object id;

  public ResponseImpl(ErrorImpl error, Object id) {
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
  public ErrorImpl getError() {
    return error;
  }

  @Override
  public Object getId() {
    return id;
  }
}
