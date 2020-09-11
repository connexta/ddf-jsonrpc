package com.connexta.jsonrpc;

/** Represents an error object to be returned (instead of a result) when an error occurs */
public interface Error {

  int PARSE_ERROR = -32700;
  int INVALID_REQUEST = -32600;
  int METHOD_NOT_FOUND = -32601;
  int INVALID_PARAMS = 32602;
  int INTERNAL_ERROR = -32603;
  int NOT_LOGGED_IN_ERROR = -32000;

  /**
   * An error code defined by the jsonRpc spec. See {@link Error} for error codes
   *
   * @return an integer error code
   */
  int getCode();

  /** @return the error message for the error that happened */
  String getMessage();

  /**
   * Can be used to return more specific error data such as what part of the input data was invalid
   * or incorrect.
   *
   * @return An object containing additional data to be sent back with the error
   */
  Object getData();
}
