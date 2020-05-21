package com.connexta.ddf.catalog.direct;

import com.connexta.ddf.transformer.RpcListHandler;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import java.nio.charset.Charset;
import java.util.Base64;
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
    return ImmutableMap.<String, Object>builder()
        .put(CatalogMethods.ATTRIBUTES, metacardAttributes2map(metacard))
        .put("metacardType", ImmutableMap.of("name", metacard.getMetacardType().getName()))
        .put("sourceId", metacard.getSourceId())
        .build();
  }

  private Map<String, Object> metacardAttributes2map(Metacard metacard) {
    Builder<String, Object> builder = ImmutableMap.builder();
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
    return builder.build();
  }
}
