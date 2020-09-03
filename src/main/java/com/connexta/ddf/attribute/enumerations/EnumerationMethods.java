package com.connexta.ddf.attribute.enumerations;

import static com.connexta.jsonrpc.impl.JsonRpc.INVALID_PARAMS;
import static com.connexta.util.MapFactory.mapOf;

import com.connexta.jsonrpc.MethodSet;
import com.connexta.jsonrpc.RpcFactory;
import com.connexta.jsonrpc.RpcMethod;
import com.connexta.jsonrpc.impl.RpcFactoryImpl;
import ddf.catalog.data.MetacardType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumerationMethods implements MethodSet {

  private final Map<String, RpcMethod> METHODS;

  private final RpcFactory rpc = new RpcFactoryImpl();

  {
    Map<String, RpcMethod> builder = new HashMap<>();
    builder.put("ddf.enumerations/all", rpc.method(this::getAllEnums, ""));
    builder.put(
        "ddf.enumerations/by-type",
        rpc.method(
            this::getEnumsByType,
            "Takes the specified parameters and calls EnumerationExtractor::getEnumerations as many times"
                + "as necessary. `params` takes: `types(Required, value:List(String))`"));
    METHODS = builder;
  }

  private List<MetacardType> metacardTypes;

  @Override
  public Map<String, RpcMethod> getMethods() {
    return METHODS;
  }

  private final EnumerationExtractor enumerationExtractor;

  public EnumerationMethods(EnumerationExtractor enumerationExtractor) {
    this.enumerationExtractor = enumerationExtractor;
  }

  private Object getEnumsByType(Map<String, Object> params) {
    Object types = params.get("types");
    if (!(types instanceof List)) {
      return rpc.error(INVALID_PARAMS, "invalid types param");
    }

    return mapOf("enumerations", getEnumsFromMetacardTypes((List<String>) types));
  }

  private Map<String, Set<String>> getEnumsFromMetacardTypes(List<String> types) {
    Map<String, Set<String>> enumerations = new HashMap<>();
    for (String type : types) {
      Map<String, Set<String>> typeEnumerations = enumerationExtractor.getEnumerations(type);
      for (String attribute : typeEnumerations.keySet()) {
        enumerations.put(attribute, typeEnumerations.get(attribute));
      }
    }
    return enumerations;
  }

  private Object getAllEnums(Map<String, Object> params) {
    // Get enumerations from metacardType definitions
    List<String> types =
        metacardTypes
            .stream()
            .map(metacardType -> metacardType.getName())
            .collect(Collectors.toList());
    Map<String, Set<String>> enumerations = getEnumsFromMetacardTypes(types);

    // TODO: Add enums from ConfigurationApplication

    return mapOf("enumerations", enumerations);
  }

  /** @param metacardTypes the metacardTypes to set */
  public void setMetacardTypes(List<MetacardType> metacardTypes) {
    this.metacardTypes = metacardTypes;
  }
}
