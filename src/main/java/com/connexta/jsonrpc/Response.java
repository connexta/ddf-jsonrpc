package com.connexta.jsonrpc;

/**
 * The Response of a rpc call that conforms to the jsonrpc spec. <bold>This is not currently meant
 * to be used by developers</bold>
 */
public interface Response {

  /**
   * The result object of the rpc call
   *
   * @return the result object or null (null if there is an error)
   */
  Object getResult();

  /**
   * The error that occured for the rpc call
   *
   * @return the error object or null (null if there was no error and has a valid response
   */
  Error getError();

  /** @return the unique id that matches the request id */
  Object getId();
}
