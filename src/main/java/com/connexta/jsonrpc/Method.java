package com.connexta.jsonrpc;

import java.util.Map;
import java.util.function.Function;

/**
 * A Method is effectively an alias for a {@code Function} that takes a map of input parameters and
 * returns an Object. That return object can either be a result value (Map, List, primitive, etc) or
 * an {@link Error} object.
 */
public interface Method extends Function<Map<String, Object>, Object> {}
