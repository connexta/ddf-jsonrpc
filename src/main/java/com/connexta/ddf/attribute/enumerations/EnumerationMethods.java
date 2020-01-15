package com.connexta.ddf.attribute.enumerations;

import static com.connexta.jsonrpc.JsonRpc.INVALID_PARAMS;

import com.connexta.jsonrpc.DocMethod;
import com.connexta.jsonrpc.Error;
import com.connexta.jsonrpc.MethodSet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import ddf.catalog.data.MetacardType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumerationMethods implements MethodSet {

  private final Map<String, DocMethod> METHODS;

  {
    Builder<String, DocMethod> builder = ImmutableMap.builder();
    builder.put("ddf.enumerations/all", new DocMethod(this::getAllEnums, ""));
    builder.put(
        "ddf.enumerations/by-type",
        new DocMethod(
            this::getEnumsByType,
            "Takes the specified parameters and calls EnumerationExtractor::getEnumerations as many times"
                + "as necessary. `params` takes: `types(Required, value:List(String))`"));
    METHODS = builder.build();
  }

  private List<MetacardType> metacardTypes;

  @Override
  public Map<String, DocMethod> getMethods() {
    return METHODS;
  }

  private final EnumerationExtractor enumerationExtractor;

  public EnumerationMethods(EnumerationExtractor enumerationExtractor) {
    this.enumerationExtractor = enumerationExtractor;
  }

  private Object getEnumsByType(Map<String, Object> params) {
    Object types = params.get("types");
    if (!(types instanceof List)) {
      return new Error(INVALID_PARAMS, "invalid types param");
    }

    return ImmutableMap.of("enumerations", getEnumsFromMetacardTypes((List<String>) types));
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

    return ImmutableMap.of("enumerations", enumerations);
  }

  /** @param metacardTypes the metacardTypes to set */
  public void setMetacardTypes(List<MetacardType> metacardTypes) {
    this.metacardTypes = metacardTypes;
  }
}
