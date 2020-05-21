package com.connexta.ddf.transformer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
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

  private CatalogFramework catalogFramework;

  public RpcListHandler(
      InputTransformer inputTransformer,
      List<MetacardType> metacardTypes,
      CatalogFramework catalogFramework) {
    this.inputTransformer = inputTransformer;
    this.listMetacardType =
        metacardTypes
            .stream()
            .filter(mt -> mt.getName().equals(listMetacardTypeName))
            .findFirst()
            .orElse(null);
    this.catalogFramework = catalogFramework;
  }

  public List<Map<String, Object>> listsXmlToMaps(List<Serializable> listsXml) {
    return listsXml
        .stream()
        .map(String.class::cast)
        .map(this::listXmlToMetacard)
        .map(this::listMetacardToMap)
        .collect(Collectors.toList());
  }

  public List<Serializable> listMetacardsToXml(Object lists) {
    if (lists instanceof List) {
      List<Map<String, Object>> metacards = (List<Map<String, Object>>) lists;
      return metacards
          .stream()
          .map(this::mapToListMetacard)
          .map(this::listMetacardToXml)
          .collect(Collectors.toList());
    }
    return null;
  }

  private Metacard mapToListMetacard(Map<String, Object> listMap) {
    Metacard listMetacard = new MetacardImpl(listMetacardType);

    for (Map.Entry<String, Object> entry : listMap.entrySet()) {
      Attribute listAttribute =
          entry.getValue() instanceof List
              ? new AttributeImpl(entry.getKey(), (List) entry.getValue())
              : new AttributeImpl(entry.getKey(), entry.getValue().toString());
      listMetacard.setAttribute(listAttribute);
    }
    return listMetacard;
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
      throw new ListTransformationException(ex);
    }
  }

  private String listMetacardToXml(Metacard metacard) {
    try (InputStream stream = catalogFramework.transform(metacard, "xml", null).getInputStream()) {
      return IOUtils.toString(stream, Charset.defaultCharset());
    } catch (IOException | CatalogTransformerException e) {
      throw new ListTransformationException(e);
    }
  }
}
