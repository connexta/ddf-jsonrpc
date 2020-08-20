package com.connexta.jsonrpc;

public interface Error {
  int getCode();

  String getMessage();

  Object getData();
}
