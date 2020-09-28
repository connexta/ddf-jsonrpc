package com.connexta.jsonrpc;

import java.util.Map;

/** A factory for creating objects associated with the jsonrpc service. */
public interface RpcFactory {

  /**
   * Creates a method to be a part of a {@link MethodSet}
   *
   * @param method
   * @param description
   * @return an RpcMethod representing the method and its associated description
   */
  RpcMethod method(Method method, String description);

  /**
   * Creates a method to be a part of a {@link MethodSet}
   *
   * @param method
   * @param metadata A map of metadata describing the method
   * @return an RpcMethod representing the method and its associated map of metadata
   */
  RpcMethod method(Method method, Map<String, Object> metadata);

  /**
   * Creates an error to be returned from an {@link RpcMethod} on failure
   *
   * @param code
   * @param message
   * @return an error with a specific error code and a message describing the error
   */
  Error error(int code, String message);

  /**
   * Creates an error to be returned from an {@link RpcMethod} on failure
   *
   * @param code
   * @param message
   * @param data A map of additional information to be returned along with the error
   * @return an error
   */
  Error error(int code, String message, Object data);
}
