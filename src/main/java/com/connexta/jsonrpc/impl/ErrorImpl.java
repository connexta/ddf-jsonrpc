package com.connexta.jsonrpc.impl;

import com.connexta.jsonrpc.Error;
import java.util.Collections;

public class ErrorImpl implements Error {

  public final int code;

  public final String message;
  public final Object data;

  public ErrorImpl(int code, String message) {
    this(code, message, null);
  }

  public ErrorImpl(int code, String message, Object data) {
    this.code = code;
    this.message = message != null ? message : "";
    this.data = data != null ? data : Collections.emptyMap();
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public Object getData() {
    return data;
  }
}
