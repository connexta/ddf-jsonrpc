package com.connexta.jsonrpc;

import java.util.Collections;
import java.util.Map;

/** Represents an object that contains a map of metadata describing an object */
public interface Metadata {

  /** @return The map of metadata describing this object */
  default Map<String, Object> getMetadata() {
    return Collections.emptyMap();
  }
}
