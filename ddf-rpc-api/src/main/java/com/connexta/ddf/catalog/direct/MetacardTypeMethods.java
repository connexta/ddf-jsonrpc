package com.connexta.ddf.catalog.direct;

import static com.connexta.util.MapFactory.mapOf;

import com.connexta.jsonrpc.Error;
import com.connexta.jsonrpc.MethodSet;
import com.connexta.jsonrpc.RpcFactory;
import com.connexta.jsonrpc.RpcMethod;
import com.connexta.jsonrpc.impl.RpcFactoryImpl;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.InjectableAttribute;
import ddf.catalog.data.MetacardType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A class that extends normal catalog methods. Represented as a map where the key is the Json Rpc
 * <code>method</code> string (eg, <code>ddf.catalog/create
 * </code>). the value of the map is a Method that can be called to dispatch the corresponding
 * extended action to the <code>CatalogFramework</code>
 */
public class MetacardTypeMethods implements MethodSet {

  private static final String TYPE_KEY = "type";

  private static final String MULTIVALUED_KEY = "multivalued";

  private static final String ID_KEY = "id";

  private static final String ISINJECTED_KEY = "isInjected";

  private final Map<String, RpcMethod> methods;

  private final RpcFactory rpc = new RpcFactoryImpl();

  private final List<InjectableAttribute> injectableAttributes;

  private final AttributeRegistry attributeRegistry;

  private final List<MetacardType> metacardTypes;

  public MetacardTypeMethods(
      List<InjectableAttribute> injectableAttributes,
      AttributeRegistry attributeRegistry,
      List<MetacardType> metacardTypes) {
    this.injectableAttributes = injectableAttributes;
    this.attributeRegistry = attributeRegistry;
    this.metacardTypes = metacardTypes;

    Map<String, RpcMethod> builder = new HashMap<>();
    builder.put(
        "ddf.catalog/allMetacardTypes",
        rpc.method(
            this::getAllMetacardTypes,
            "Returns all current metacard types in the system (including injected attributes)"));
    builder.put(
        "ddf.catalog/metacardType",
        rpc.method(
            this::getMetacardType,
            "Returns requested metacardType (including injected attributes). Params: `metacardType`: String"));
    methods = builder;
  }

  @Override
  public Map<String, RpcMethod> getMethods() {
    return methods;
  }

  private Object getMetacardType(Map<String, Object> params) {
    if (!(params.get("metacardType") instanceof String)) {
      return rpc.error(
          Error.INVALID_PARAMS,
          "`metacardType` must be present and a string value",
          mapOf("path", Arrays.asList("params", "metacardType")));
    }
    String metacardTypeName = (String) params.get("metacardType");

    Optional<MetacardType> metacardType =
        metacardTypes.stream().filter(mt -> mt.getName().equals(metacardTypeName)).findFirst();

    if (!metacardType.isPresent()) {
      return rpc.error(
          Error.INVALID_REQUEST,
          "Could not find the requested metacardType",
          mapOf("irritant", metacardTypeName));
    }

    return getMetacardTypeMap(Collections.singletonList(metacardType.get()));
  }

  private Object getAllMetacardTypes(Map<String, Object> params) {
    return getMetacardTypeMap(metacardTypes);
  }

  public Map<String, Object> getMetacardTypeMap(List<MetacardType> metacardTypeList) {
    Map<String, Object> resultTypes = new HashMap<>();
    for (MetacardType metacardType : metacardTypeList) {
      Map<String, Object> attributes = new HashMap<>();
      for (AttributeDescriptor descriptor : metacardType.getAttributeDescriptors()) {
        Map<String, Object> attributeProperties = new HashMap<>();
        attributeProperties.put(TYPE_KEY, descriptor.getType().getAttributeFormat().name());
        attributeProperties.put(MULTIVALUED_KEY, descriptor.isMultiValued());
        attributeProperties.put(ID_KEY, descriptor.getName());
        attributeProperties.put(ISINJECTED_KEY, false);
        attributes.put(descriptor.getName(), attributeProperties);
      }
      resultTypes.put(metacardType.getName(), attributes);
    }
    for (InjectableAttribute attribute : injectableAttributes) {
      Optional<AttributeDescriptor> lookup = attributeRegistry.lookup(attribute.attribute());
      if (!lookup.isPresent()) {
        continue;
      }

      AttributeDescriptor descriptor = lookup.get();
      Map<String, Object> attributeProperties = new HashMap<>();
      attributeProperties.put(TYPE_KEY, descriptor.getType().getAttributeFormat().name());
      attributeProperties.put(MULTIVALUED_KEY, descriptor.isMultiValued());
      attributeProperties.put(ID_KEY, descriptor.getName());
      attributeProperties.put(ISINJECTED_KEY, true);
      Set<String> types =
          attribute.metacardTypes().isEmpty() ? resultTypes.keySet() : attribute.metacardTypes();

      types
          .stream()
          .filter(type -> isAttributeMissing(resultTypes, attribute, type))
          .forEach(
              type ->
                  mergeMetacardTypeIntoResults(resultTypes, attribute, attributeProperties, type));
    }

    return mapOf("metacardTypes", resultTypes);
  }

  @SuppressWarnings("unchecked")
  private boolean isAttributeMissing(
      Map<String, Object> resultTypes, InjectableAttribute attribute, String type) {
    if (!resultTypes.containsKey(type)) {
      return true;
    }

    Map<String, Object> attributes = (Map<String, Object>) resultTypes.get(type);

    return !attributes.containsKey(attribute.attribute());
  }

  @SuppressWarnings("unchecked")
  private void mergeMetacardTypeIntoResults(
      Map<String, Object> resultTypes,
      InjectableAttribute attribute,
      Map<String, Object> attributeProperties,
      String type) {
    Map<String, Object> attributes =
        (Map) resultTypes.getOrDefault(type, new HashMap<String, Object>());
    attributes.put(attribute.attribute(), attributeProperties);
    resultTypes.put(type, attributes);
  }
}
