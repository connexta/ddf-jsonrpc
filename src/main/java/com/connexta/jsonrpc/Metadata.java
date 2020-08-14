package com.connexta.jsonrpc;

import java.util.Collections;
import java.util.Map;

public interface Metadata {
  default Map<String, Object> getMetadata() {
    return Collections.emptyMap();
  }
}
