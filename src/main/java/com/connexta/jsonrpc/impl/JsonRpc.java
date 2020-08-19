package com.connexta.jsonrpc.impl;

import static com.connexta.util.MapFactory.mapOf;

import com.connexta.jsonrpc.Method;
import com.connexta.jsonrpc.MethodSet;
import com.connexta.jsonrpc.RpcMethod;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonRpc implements Method {

  public static final int PARSE_ERROR = -32700;
  public static final int INVALID_REQUEST = -32600;
  public static final int METHOD_NOT_FOUND = -32601;
  public static final int INVALID_PARAMS = 32602;
  public static final int INTERNAL_ERROR = -32603;
  public static final int NOT_LOGGED_IN_ERROR = -32000;

  public static final String VERSION = "2.0";
  private static final String METHOD = "method";
  private static final String ID = "id";
  private static final String PARAMS = "params";

  private final Map<String, RpcMethod> defaultMethods;
  private final Set<MethodSet> dynamicMethods = new HashSet<>();
  private Map<String, RpcMethod> methods = Collections.emptyMap();

  public JsonRpc(List<MethodSet> methodSets) {
    Map<String, RpcMethod> builder = new HashMap<>();
    builder.put("list-methods", new DocMethodImpl(this::listMethods, "list all available methods"));
    for (MethodSet methods : methodSets) {
      builder.putAll(methods.getMethods());
    }
    this.defaultMethods = builder;
    recompileMethodList();
  }

  @Override
  public Object apply(Map<String, Object> request) {
    return process(request);
  }

  public synchronized void bindMethods(MethodSet methodSet) {
    dynamicMethods.add(methodSet);
    recompileMethodList();
  }

  public synchronized void unbindMethods(MethodSet methodSet) {
    dynamicMethods.remove(methodSet);
    recompileMethodList();
  }

  private void recompileMethodList() {
    Map<String, RpcMethod> newMethodList = new HashMap<>(defaultMethods);

    newMethodList.putAll(
        dynamicMethods
            .stream()
            .flatMap(ms -> ms.getMethods().entrySet().stream())
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (oldVal, newVal) -> newVal)));

    methods = newMethodList;
  }

  private Object process(Map<String, Object> request) {
    Object id = request.get(ID);

    Instant start = Instant.now();
    Object response = dispatch(request);
    Duration duration = Duration.between(start, Instant.now());
    if (response instanceof Error) {
      Error error = (Error) response;
      if (error.data instanceof Map) {
        Map<Object, Object> builder = new HashMap<>((Map<Object, Object>) error.data);
        builder.put("request_duration_millis", duration.toMillis());
        error = new Error(error.code, error.message, builder);
      }
      return new Response(error, id);
    } else if (response instanceof Map) {
      Map<Object, Object> updatedResponse = new HashMap<>((Map<Object, Object>) response);
      updatedResponse.put("request_duration_millis", duration.toMillis());
      return new Response(updatedResponse, id);
    } else {
      return new Response(response, id);
    }
  }

  private Object dispatch(Map request) {
    if (request.get(ID) == null) {
      return new Error(INVALID_REQUEST, "Missing/Invalid id");
    }

    if (!request.containsKey(METHOD)) {
      return new Error(
          METHOD_NOT_FOUND,
          "Unknown method - use \"method\": \"list-methods\" to see available methods");
    }

    if (!(request.get(METHOD) instanceof String)) {
      return new Error(
          METHOD_NOT_FOUND,
          "Unknown method - use \"method\": \"list-methods\" to see available methods");
    }
    String method = (String) request.get(METHOD);

    RpcMethod target = methods.get(method);
    if (target == null) {
      return new Error(
          METHOD_NOT_FOUND,
          "Unknown method - use \"method\": \"list-methods\" to see available methods");
    }

    if (!(request.get(PARAMS) instanceof Map)) {
      return new Error(INVALID_PARAMS, "params were not a map");
    }
    Map<String, Object> params = (Map) request.get(PARAMS);

    try {
      return target.apply(params);
    } catch (RuntimeException e) {
      return new Error(INTERNAL_ERROR, "Error occured - " + e.getMessage());
    }
  }

  private Object listMethods(Map<String, Object> params) {
    return methods
        .entrySet()
        .stream()
        .map(e -> mapOf("method", e.getKey(), "docstring", e.getValue().getDocstring()))
        .collect(Collectors.toList());
  }
}
