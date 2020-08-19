package com.connexta.ddf.catalog.direct;

import static com.connexta.util.MapFactory.mapOf;

import com.connexta.ddf.transformer.RpcListHandler;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MetacardMap {

  public static final String LISTS = "lists";

  private final RpcListHandler listHandler;

  public MetacardMap(RpcListHandler rpcListHandler) {
    this.listHandler = rpcListHandler;
  }

  public Map<String, Object> convert(Metacard metacard) {
    Map<String, Object> build = new HashMap<>();
    build.put(CatalogMethods.ATTRIBUTES, metacardAttributes2map(metacard));
    build.put("metacardType", mapOf("name", metacard.getMetacardType().getName()));
    build.put("sourceId", metacard.getSourceId());
    return build;
  }

  private Map<String, Object> metacardAttributes2map(Metacard metacard) {
    Map<String, Object> builder = new HashMap<>();
    for (AttributeDescriptor ad : metacard.getMetacardType().getAttributeDescriptors()) {
      Attribute attribute = metacard.getAttribute(ad.getName());
      if (attribute == null) {
        continue;
      }

      Function<Object, Object> preprocessor = Function.identity();
      if (AttributeFormat.BINARY.equals(ad.getType().getAttributeFormat())) {
        preprocessor =
            preprocessor.andThen(
                input ->
                    new String(
                        Base64.getEncoder().encode((byte[]) input), Charset.defaultCharset()));
      }

      if (ad.isMultiValued()) {
        if (attribute.getName().equals(LISTS) && listHandler != null) {
          builder.put(
              attribute.getName(),
              listHandler
                  .listsXmlToMaps(attribute.getValues())
                  .stream()
                  .map(preprocessor)
                  .collect(Collectors.toList()));
        } else {
          builder.put(
              attribute.getName(),
              attribute.getValues().stream().map(preprocessor).collect(Collectors.toList()));
        }
      } else {
        builder.put(attribute.getName(), preprocessor.apply(attribute.getValue()));
      }
    }
    return builder;
  }
}
