package com.connexta.jsonrpc;

public interface Response {
  Object getResult();

  Error getError();

  Object getId();
}
