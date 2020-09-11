package com.connexta.jsonrpc;

/**
 * Represents that an object supports returning a docstring to be used in developer documentation
 */
public interface Docstring {
  default String getDocstring() {
    return "";
  }
}
