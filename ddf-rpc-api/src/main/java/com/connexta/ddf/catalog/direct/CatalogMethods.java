package com.connexta.ddf.catalog.direct;

import static com.connexta.jsonrpc.Error.INTERNAL_ERROR;
import static com.connexta.jsonrpc.Error.INVALID_PARAMS;
import static com.connexta.util.ImmutablePair.pairOf;
import static com.connexta.util.MapFactory.mapOf;
import static com.connexta.util.StringUtils.isBlank;
import static ddf.catalog.Constants.EXPERIMENTAL_FACET_RESULTS_KEY;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import com.connexta.ddf.persistence.subscriptions.SubscriptionMethods;
import com.connexta.ddf.transformer.RpcListHandler;
import com.connexta.jsonrpc.Error;
import com.connexta.jsonrpc.MethodSet;
import com.connexta.jsonrpc.RpcFactory;
import com.connexta.jsonrpc.RpcMethod;
import com.connexta.jsonrpc.impl.RpcFactoryImpl;
import com.connexta.util.ImmutablePair;
import ddf.action.Action;
import ddf.action.ActionRegistry;
import ddf.action.impl.ActionImpl;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.PropertyNameImpl;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.FacetAttributeResult;
import ddf.catalog.operation.FacetValueCount;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.FacetedQueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceInfoRequestSources;
import ddf.catalog.operation.impl.TermFacetPropertiesImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.geotools.filter.SortByImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that represents the set of methods that are callable on the <code>CatalogFramework</code>
 * as a map where the key is the Json Rpc <code>method</code> string (eg, <code>ddf.catalog/create
 * </code>). the value of the map is a Method that can be called to dispatch the corresponding
 * action to the <code>CatalogFramework</code>
 */
public class CatalogMethods implements MethodSet {
  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogMethods.class);

  private static final String ISO_8601_DATE_STRING =
      "[yyyyMMdd][yyyy-MM-dd][yyyy-DDD]['T'[HHmmss][HHmm][HH:mm:ss][HH:mm][.SSSSSSSSS][.SSSSSS][.SSS][.SS][.S]][OOOO][O][z][XXXXX][XXXX]['['VV']']";

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern(ISO_8601_DATE_STRING).withZone(ZoneOffset.UTC);

  public static final String ATTRIBUTES = "attributes";

  public static final String CREATE_KEY = "ddf.catalog/create";

  //  private final transient SimpleDateFormat dateFormat = new
  // SimpleDateFormat(ISO_8601_DATE_FORMAT);
  private final RpcFactory rpc = new RpcFactoryImpl();

  private final Map<String, RpcMethod> METHODS;

  {
    Map<String, RpcMethod> builder = new HashMap<>();
    builder.put(
        CREATE_KEY,
        rpc.method(
            this::create,
            "Takes the specified parameters (metacards) and calls"
                + " CatalogFramework::create.`params` takes: `metacards(Required, value:"
                + " List(Object(`metacardType`:string, `attributes`:Object(Required, `id`:"
                + " String))) "));
    builder.put(
        "ddf.catalog/query",
        rpc.method(
            this::query,
            "Takes the specified parameters and calls CatalogFramework::query. `params` takes:"
                + " `cql` (TemporarilyRequired, value: String of cql), `sourceIds` (Optional,"
                + " value: List of strings, Default: ['ddf.distribution']), `isEnterprise`"
                + " (Optional, value: boolean, default: false), `properties` (Not yet supported),"
                + " `startIndex` (Optional, value: integer, default: 1), `pageSize` (Optional,"
                + " value: integer, default: 250), `sortPolicy` (Optional, value:"
                + " Object(`propertyName`:String, `sortOrder`: String(ASC or DESC))"));

    builder.put(
        "ddf.catalog/update",
        rpc.method(
            this::update,
            "Takes the specified parameters and calls CatalogFramework::query. `params` takes:"
                + " `metacards(Required, value: List(Object(`metacardType`:string,"
                + " `attributes`:Object(Required, `id`: String)))"));
    builder.put(
        "ddf.catalog/delete",
        rpc.method(
            this::delete,
            "Takes the specified parameters and calls CatalogFramework::query. `params` takes:"
                + " `ids` (Required, value: List(String))"));

    builder.put("ddf.catalog/getSourceIds", rpc.method(this::getSourceIds, ""));
    builder.put("ddf.catalog/getSourceInfo", rpc.method(this::getSourceInfo, ""));
    METHODS = builder;
  }

  @Override
  public Map<String, RpcMethod> getMethods() {
    return METHODS;
  }

  private CatalogFramework catalogFramework;

  private List<MetacardType> metacardTypes;

  private AttributeRegistry attributeRegistry;

  private FilterBuilder filterBuilder;

  private ActionRegistry actionRegistry;

  private SubscriptionMethods subscription;

  private MetacardMap metacardMap;

  private RpcListHandler listHandler;

  public CatalogMethods(
      CatalogFramework catalogFramework,
      AttributeRegistry attributeRegistry,
      List<MetacardType> metacardTypes,
      FilterBuilder filterBuilder,
      ActionRegistry actionRegistry,
      SubscriptionMethods subscription,
      MetacardMap metacardMap,
      RpcListHandler listHandler) {
    this.catalogFramework = catalogFramework;
    this.attributeRegistry = attributeRegistry;
    this.metacardTypes = metacardTypes;
    this.filterBuilder = filterBuilder;
    this.actionRegistry = actionRegistry;
    this.subscription = subscription;
    this.metacardMap = metacardMap;
    this.listHandler = listHandler;
  }

  private Object getSourceIds(Map<String, Object> params) {
    return catalogFramework.getSourceIds();
  }

  private Object getSourceInfo(Map<String, Object> params) {
    Object includeContentTypes = params.getOrDefault("includeContentTypes", false);

    if (!(includeContentTypes instanceof Boolean)) {
      return rpc.error(INVALID_PARAMS, "missing param");
    }

    Object ids = params.get("ids");

    if (!(ids instanceof List)) {
      return rpc.error(INVALID_PARAMS, "invalid ids param");
    }

    SourceInfoRequestSources info =
        new SourceInfoRequestSources(
            (Boolean) includeContentTypes, new HashSet<>((List<String>) ids));
    try {
      return singletonMap("sourceInfo", catalogFramework.getSourceInfo(info).getSourceInfo());
    } catch (SourceUnavailableException e) {
      return rpc.error(INTERNAL_ERROR, e.getMessage());
    }
  }

  private Object delete(Map<String, Object> params) {
    if (!(params.get("ids") instanceof List)) {
      return rpc.error(INVALID_PARAMS, "ids not provided");
    }
    List<String> ids = (List<String>) params.get("ids");

    DeleteResponse deleteResponse;
    try {
      deleteResponse = catalogFramework.delete(new DeleteRequestImpl(ids.toArray(new String[] {})));
    } catch (IngestException | SourceUnavailableException e) {
      return rpc.error(INTERNAL_ERROR, e.getMessage());
    }

    return mapOf(
        "deletedMetacards",
        deleteResponse
            .getDeletedMetacards()
            .stream()
            .map(metacardMap::convert)
            .collect(Collectors.toList()));
  }

  private Object update(Map<String, Object> params) {
    if (!(params.get("metacards") instanceof List)) {
      return rpc.error(INVALID_PARAMS, "params were not a list");
    }
    List<Map> metacards = (List<Map>) params.get("metacards");

    List<Metacard> updateList = new ArrayList<>(metacards.size());
    for (int i = 0; i < metacards.size(); i++) {
      Map m = metacards.get(i);
      ImmutablePair<Metacard, String> res = map2Metacard(m);
      if (res.getRight() != null) {
        return rpc.error(
            Error.PARSE_ERROR,
            res.getRight(),
            mapOf("irritant", m, "path", asList("params", "metacards", i)));
      }
      if (isBlank(res.getLeft().getId())) {
        return rpc.error(
            INVALID_PARAMS,
            "id for metacard can not be blank/empty",
            mapOf("irritant", m, "path", asList("params", "metacards", i)));
      }
      updateList.add(res.getLeft());
    }

    String[] ids =
        updateList
            .stream()
            .map(Metacard::getId)
            .collect(Collectors.toList())
            .toArray(new String[0]);
    UpdateResponse updateResponse;
    try {
      updateResponse = catalogFramework.update(new UpdateRequestImpl(ids, updateList));
    } catch (IngestException | SourceUnavailableException e) {
      return rpc.error(INTERNAL_ERROR, e.getMessage());
    }

    return mapOf(
        "updatedMetacards",
        updateResponse
            .getUpdatedMetacards()
            .stream()
            .map(Update::getNewMetacard)
            .map(metacardMap::convert)
            .collect(Collectors.toList()));
  }

  private Object getSortBy(Map<String, Object> rawSortPolicy) {
    if (!(rawSortPolicy.get("propertyName") instanceof String)) {
      return rpc.error(
          INVALID_PARAMS,
          "propertyName was not a string or was missing",
          mapOf("irritant", asList("params", "sortPolicy", "propertyName")));
    }
    String propertyName = (String) rawSortPolicy.get("propertyName");

    if (!(rawSortPolicy.get("sortOrder") instanceof String)) {
      return rpc.error(
          INVALID_PARAMS,
          "sortOrder was not a string or was missing",
          mapOf("irritant", asList("params", "sortPolicy", "sortOrder")));
    }
    String sortOrderString = (String) rawSortPolicy.get("sortOrder");
    SortOrder sortOrder;
    if ("ascending".equalsIgnoreCase(sortOrderString) || "asc".equalsIgnoreCase(sortOrderString)) {
      sortOrder = SortOrder.ASCENDING;
    } else if ("descending".equalsIgnoreCase(sortOrderString)
        || "desc".equalsIgnoreCase(sortOrderString)) {
      sortOrder = SortOrder.DESCENDING;
    } else {
      return rpc.error(
          INVALID_PARAMS,
          "sortOrder was not asc[ending] or desc[ending]",
          mapOf("irritant", asList("params", "sortPolicy", "sortOrder")));
    }
    SortBy sortPolicy = new SortByImpl(new PropertyNameImpl(propertyName), sortOrder);
    return sortPolicy;
  }

  private Map getProperties(Map<String, Object> properties) {
    Map<String, Object> result = new HashMap<>();
    if (properties.containsKey("additional-sort-bys")) {
      SortBy[] additionalSortBys =
          (SortBy[])
              ((List) properties.get("additional-sort-bys"))
                  .stream()
                  .map(sort -> getSortBy((Map) sort))
                  .toArray(SortBy[]::new);
      result.put("additional-sort-bys", additionalSortBys);
    }

    return result;
  }

  private Object query(Map<String, Object> params) {
    Filter filter = null;
    if (params.containsKey("cql") && params.containsKey("query")) {
      return rpc.error(INVALID_PARAMS, "cannot have both query and cql present");
    }

    if (params.containsKey("cql")) {
      String cql = (String) params.get("cql");
      try {
        filter = ECQL.toFilter(cql);
      } catch (CQLException e) {
        return rpc.error(INVALID_PARAMS, "could not parse cql", mapOf("cql", cql));
      }
    }

    if (params.containsKey("query")) {
      return rpc.error(INVALID_PARAMS, "query is not supported yet");
      //    Map root = (Map) params.get("query");
      //    try {
      //    Filter filter = recur(root);
      //    } catch (FilterTreeParseException e) {
      //      //todo
      //    }
    }
    if (filter == null) {
      return rpc.error(INVALID_PARAMS, "params must have query or cql");
    }

    int startIndex = 1;
    if (params.containsKey("startIndex")) {
      if (!(params.get("startIndex") instanceof Number)) {
        return rpc.error(
            INVALID_PARAMS,
            "startIndex was not a number",
            mapOf("irritant", params.get("startIndex"), "path", asList("params", "startIndex")));
      }

      startIndex = ((Number) params.get("startIndex")).intValue();
    }

    int pageSize = 200;
    if (params.containsKey("pageSize")) {
      if (!(params.get("pageSize") instanceof Number)) {
        return rpc.error(
            INVALID_PARAMS,
            "pageSize was not a number",
            mapOf("irritant", asList("params", "pageSize")));
      }

      pageSize = ((Number) params.get("pageSize")).intValue();
    }

    SortBy sortPolicy = SortBy.NATURAL_ORDER;
    if (params.containsKey("sortPolicy")) {
      if (params.get("sortPolicy") instanceof Map) {
        Map<String, Object> rawSortPolicy = (Map) params.get("sortPolicy");
        sortPolicy = (SortBy) getSortBy(rawSortPolicy);
      }
    }

    // TODO (RCZ) - Not configurable for now. is this safe to let clients config?
    boolean requestTotalResultsCount = true;

    // TODO (RCZ) - Not configurable for now. Is this safe to let clients config?
    // TODO (RCZ) - What should the default timeout be?
    long timeoutMillis = 0;

    boolean isEnterprise = false;
    if (params.containsKey("isEnterprise")) {
      isEnterprise = (boolean) params.get("isEnterprise");
    }

    List<String> sourceIds = new ArrayList<>();
    if (params.containsKey("sourceIds")) {
      if (!(params.get("sourceIds") instanceof List)) {
        return rpc.error(
            Error.INVALID_PARAMS,
            "sourceIds was not a List",
            mapOf("path", asList("params", "sourceIds")));
      }
      sourceIds = (List<String>) params.get("sourceIds");
    }

    Map<String, Serializable> properties = new HashMap<>();
    if (params.containsKey("properties")) {
      properties = getProperties((Map) params.get("properties"));
    }
    QueryResponse queryResponse;

    QueryRequestImpl queryRequest =
        new QueryRequestImpl(
            new QueryImpl(
                filter, startIndex, pageSize, sortPolicy, requestTotalResultsCount, timeoutMillis),
            isEnterprise,
            sourceIds,
            properties);

    if (params.containsKey("facets")) {
      if (!(params.get("facets") instanceof Collection)) {
        return rpc.error(
            Error.INVALID_PARAMS,
            "facets was not a Collection",
            mapOf("path", asList("params", "facets")));
      }

      queryRequest =
          new FacetedQueryRequest(
              queryRequest.getQuery(),
              queryRequest.isEnterprise(),
              queryRequest.getSourceIds(),
              queryRequest.getProperties(),
              new TermFacetPropertiesImpl(new HashSet((Collection) params.get("facets"))));
    }
    try {
      queryResponse = catalogFramework.query(queryRequest);
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      return rpc.error(
          INTERNAL_ERROR, "An error occured while running your query - " + e.getMessage());
    }
    return mapOf(
        "results",
        getResults(queryResponse),
        "status",
        getQueryInfo(queryResponse),
        "facets",
        getFacetResults(queryResponse.getPropertyValue(EXPERIMENTAL_FACET_RESULTS_KEY)),
        "properties",
        queryResponse.getProperties());
  }

  private boolean isSubscribed(Metacard metacard, List<String> ids) {
    return ids.contains(metacard.getId());
  }

  private List<Action> getMetacardActions(Metacard metacard) {
    return this.actionRegistry
        .list(metacard)
        .stream()
        .map(
            action ->
                new ActionImpl(
                    action.getId(), action.getTitle(), action.getDescription(), action.getUrl()))
        .collect(Collectors.toList());
  }

  private boolean isWorkspace(Metacard metacard) {
    return metacard.getAttribute("metacard-tags") != null
        && metacard.getAttribute("metacard-tags").getValues().contains("workspace");
  }

  private Map<String, Object> getMetacardInfo(Metacard metacard) {
    return isWorkspace(metacard)
        ? mapOf(
            "metacard", metacardMap.convert(metacard),
            "actions", getMetacardActions(metacard))
        : mapOf(
            "metacard", metacardMap.convert(metacard),
            "actions", getMetacardActions(metacard));
  }

  private List<Object> getResults(QueryResponse queryResponse) {
    return queryResponse
        .getResults()
        .stream()
        .map(Result::getMetacard)
        .map(metacard -> getMetacardInfo(metacard))
        .collect(Collectors.toList());
  }

  private Map<String, Integer> getQueryInfo(QueryResponse queryResponse) {
    return mapOf(
        "hits", Math.toIntExact(queryResponse.getHits()),
        "count", queryResponse.getResults().size());
  }

  private Map<String, List<FacetValueCount>> getFacetResults(Serializable facetResults) {
    if (!(facetResults instanceof List)) {
      return Collections.emptyMap();
    }
    List<Object> list = (List<Object>) facetResults;
    return list.stream()
        .filter(result -> result instanceof FacetAttributeResult)
        .map(result -> (FacetAttributeResult) result)
        .collect(
            Collectors.toMap(
                FacetAttributeResult::getAttributeName,
                FacetAttributeResult::getFacetValues,
                (a, b) -> b));
  }

  //  private Filter recur(Map tree) throws FilterTreeParseException {
  //    String type = (String) tree.get("type");
  //
  //    // recursive types
  //    if ("AND".equals(type)) {
  //      List<Map> filters = (List) tree.get("filters");
  //      if (filters.size() != 1) {
  //        throw new FilterTreeParseException();
  //      }
  //      return
  // filterBuilder.allOf(filters.stream().map(this::recur).collect(Collectors.toList()));
  //    } else if ("OR".equals(type)) {
  //      List<Map> filters = (List) tree.get("filters");
  //      if (filters.size() != 1) {
  //        throw new FilterTreeParseException();
  //      }
  //      return
  // filterBuilder.anyOf(filters.stream().map(this::recur).collect(Collectors.toList()));
  //
  //    } else if ("NOT".equals(type)) {
  //      List<Map> filters = (List) tree.get("filters");
  //      if (filters.size() != 1) {
  //        throw new FilterTreeParseException();
  //      }
  //      return filterBuilder.not(recur(filters.get(0)));
  //    }
  //
  //    // literal types
  //    if ("equalTo".equals(type) || "=".equals(type)) {
  //      String property = (String) tree.get("property");
  //      Object value = tree.get("value");
  //      return filterBuilder.attribute(property).equalTo();
  //    }
  //  }

  private Object create(Map<String, Object> params) {
    if (!(params.get("metacards") instanceof List)) {
      return rpc.error(INVALID_PARAMS, "params were not a list");
    }
    List<Map> metacards = (List<Map>) params.get("metacards");

    // TODO (RCZ) - need to define semantics (best effort, fail all, try all..)
    List<Metacard> createList = new ArrayList<>(metacards.size());
    for (int i = 0; i < metacards.size(); i++) {
      Map m = metacards.get(i);
      ImmutablePair<Metacard, String> res = map2Metacard(m);
      if (res.getRight() != null) {
        return rpc.error(
            Error.PARSE_ERROR,
            res.getRight(),
            mapOf("irritant", m, "path", asList("params", "metacards", i)));
      }
      createList.add(res.getLeft());
    }

    // TODO (RCZ) - No properties until we have a white/blacklist so you can't sneak in security
    // attributes
    CreateResponse createResponse;
    try {
      createResponse = catalogFramework.create(new CreateRequestImpl(createList));
    } catch (IngestException | SourceUnavailableException e) {
      return rpc.error(INTERNAL_ERROR, e.getMessage());
    }

    return mapOf(
        "createdMetacards",
        createResponse
            .getCreatedMetacards()
            .stream()
            .map(metacardMap::convert)
            .collect(Collectors.toList()));
  }

  private ImmutablePair<Metacard, String> map2Metacard(Map metacard) {
    if (!(metacard.get(ATTRIBUTES) instanceof Map)) {
      return pairOf(null, "attributes not exist for params");
    }
    Map<String, Object> attributes = (Map) metacard.get(ATTRIBUTES);

    Object rawType = metacard.get("metacardType");
    String desiredMetacardType = rawType instanceof String ? String.valueOf(rawType) : null;
    MetacardType metacardType =
        metacardTypes
            .stream()
            .filter(mt -> mt.getName().equals(desiredMetacardType))
            .findFirst()
            .orElse(MetacardImpl.BASIC_METACARD);

    Metacard result = new MetacardImpl(metacardType);

    for (Entry<String, Object> entry : attributes.entrySet()) {
      if (entry.getKey().equals(MetacardMap.LISTS)) {
        result.setAttribute(
            new AttributeImpl(entry.getKey(), listHandler.listMetacardsToXml(entry.getValue())));
        continue;
      }
      ImmutablePair<Attribute, String> res = getAttribute(entry.getKey(), entry.getValue());
      if (res.getRight() != null) {
        return pairOf(null, res.getRight());
      }
      result.setAttribute(res.getLeft());
    }
    return pairOf(result, null);
  }

  private ImmutablePair<Attribute, String> getAttribute(String name, Object value) {

    AttributeDescriptor ad =
        attributeRegistry
            .lookup(name)
            .orElseGet(
                () ->
                    new AttributeDescriptorImpl(
                        name, true, true, false, true, BasicTypes.STRING_TYPE));

    if (value == null) {
      return pairOf(new AttributeImpl(name, (Serializable) null), null);
    }
    switch (ad.getType().getAttributeFormat()) {
      case BINARY:
        return pairOf(new AttributeImpl(name, Base64.getDecoder().decode((String) value)), null);
      case DATE:
        try {
          return pairOf(new AttributeImpl(name, parseDate(value.toString())), null);
        } catch (DateTimeParseException e) {
          return pairOf(
              null,
              String.format(
                  "Could not parse '%s' as iso8601 string".format(String.valueOf(value))));
        }
      case GEOMETRY:
      case STRING:
      case XML:
        return value instanceof List
            ? pairOf(new AttributeImpl(name, ((List) value)), null)
            : pairOf(new AttributeImpl(name, value.toString()), null);
      case BOOLEAN:
        return pairOf(new AttributeImpl(name, Boolean.parseBoolean(value.toString())), null);
      case SHORT:
        try {
          return pairOf(new AttributeImpl(name, Short.parseShort(value.toString())), null);
        } catch (NumberFormatException e) {
          return pairOf(
              null, String.format("Could not convert value for '%s'. \n%s", name, e.toString()));
        }
      case INTEGER:
        try {
          return pairOf(new AttributeImpl(name, Integer.parseInt(value.toString())), null);
        } catch (NumberFormatException e) {
          return pairOf(
              null, String.format("Could not convert value for '%s'. \n%s", name, e.toString()));
        }
      case LONG:
        try {
          return pairOf(new AttributeImpl(name, Long.parseLong(value.toString())), null);
        } catch (NumberFormatException e) {
          return pairOf(
              null, String.format("Could not convert value for '%s'. \n%s", name, e.toString()));
        }
      case FLOAT:
        try {
          return pairOf(new AttributeImpl(name, Float.parseFloat(value.toString())), null);
        } catch (NumberFormatException e) {
          return pairOf(
              null, String.format("Could not convert value for '%s'. \n%s", name, e.toString()));
        }
      case DOUBLE:
        try {
          return pairOf(new AttributeImpl(name, Double.parseDouble(value.toString())), null);
        } catch (NumberFormatException e) {
          return pairOf(
              null, String.format("Could not convert value for '%s'. \n%s", name, e.toString()));
        }
      default:
        return pairOf(new AttributeImpl(name, String.valueOf(value)), null);
    }
  }

  private Date parseDate(Serializable date) {
    TemporalAccessor temporalAccessor = DATE_FORMATTER.parse(date.toString());
    return new Date(ZonedDateTime.from(temporalAccessor).toInstant().toEpochMilli());
  }

  private static class FilterTreeParseException extends RuntimeException {

    private FilterTreeParseException(String msg) {
      super(msg);
    }

    private FilterTreeParseException() {}
  }
}
