package com.connexta.ddf.transformer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;

public class RpcListHandler {
  private InputTransformer inputTransformer;

  private static final String listMetacardTypeName = "metacard.list";

  private MetacardType listMetacardType;

  public RpcListHandler(InputTransformer inputTransformer, List<MetacardType> metacardTypes) {
    this.inputTransformer = inputTransformer;
    this.listMetacardType =
        metacardTypes
            .stream()
            .filter(mt -> mt.getName().equals(listMetacardTypeName))
            .findFirst()
            .orElse(null);
  }

  public List<Map<String, Object>> listsXmlToMaps(List<Serializable> listsXml) {
    return listsXml
        .stream()
        .map(String.class::cast)
        .map(this::listXmlToMetacard)
        .map(this::listMetacardToMap)
        .collect(Collectors.toList());
  }

  private Map<String, Object> listMetacardToMap(Metacard listMetacard) {
    Builder<String, Object> listMap = new ImmutableMap.Builder<>();
    for (AttributeDescriptor ad : listMetacardType.getAttributeDescriptors()) {
      Attribute attribute = listMetacard.getAttribute(ad.getName());
      if (attribute == null) {
        continue;
      }
      if (ad.isMultiValued()) {
        listMap.put(attribute.getName(), attribute.getValues());
      } else {
        listMap.put(attribute.getName(), attribute.getValue());
      }
    }
    return listMap.build();
  }

  private Metacard listXmlToMetacard(String xml) {
    try {
      InputStream inputStream = IOUtils.toInputStream(xml, Charset.defaultCharset());
      return inputTransformer.transform(inputStream);
    } catch (IOException | CatalogTransformerException ex) {
      throw new RuntimeException(ex);
    }
  }
}
