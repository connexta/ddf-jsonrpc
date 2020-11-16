package com.connexta.ddf.platform.direct;

import static com.connexta.util.MapFactory.mapOf;
import static java.util.Arrays.asList;

import com.connexta.ddf.catalog.direct.CatalogMethods;
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
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlatformMethods implements MethodSet {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlatformMethods.class);
  private static final int ACTION_ID_QUERY_LIMIT = 500;

  private final RpcFactory rpc = new RpcFactoryImpl();

  private final Map<String, RpcMethod> methods;

  private CatalogMethods catalogMethods;

  private final FilterBuilder filterBuilder;

  private CatalogFramework catalogFramework;

  private ActionRegistry actionRegistry;

  public PlatformMethods(
      CatalogFramework catalogFramework,
      ActionRegistry actionRegistry,
      CatalogMethods catalogMethods,
      FilterBuilder filterBuilder) {
    this.catalogFramework = catalogFramework;
    this.actionRegistry = actionRegistry;
    this.catalogMethods = catalogMethods;
    this.filterBuilder = filterBuilder;

    Map<String, RpcMethod> builder = new HashMap<>();
    builder.put(
        "ddf.platform/getActions",
        rpc.method(
            this::getActionsByIds,
            "Takes either a list of metacard ids or a query in the form of `ddf.catalog/query`"
                + " and returns all actions available on the metacards. `params` takes: ONLY ONE OF `ids` "
                + "(value: List(String - valid metacard ids), max ids:"
                + ACTION_ID_QUERY_LIMIT
                + ") or `query` (See parameters available on `ddf.catalog/query`).  "));
    methods = builder;
  }

  @Override
  public Map<String, RpcMethod> getMethods() {
    return methods;
  }

  private Object getActionsByIds(Map<String, Object> params) {
    Optional<List<String>> ids =
        Optional.of(params)
            .map(p -> p.get("ids"))
            .filter(List.class::isInstance)
            .map(idList -> (List<String>) idList);
    Optional<Map<String, Object>> queryParams =
        Optional.of(params)
            .map(p -> p.get("query"))
            .filter(Map.class::isInstance)
            .map(paramMap -> (Map<String, Object>) paramMap);

    // Exclude cases where neither were sent or both were sent
    if ((!ids.isPresent() && !queryParams.isPresent())
        || (ids.isPresent() && queryParams.isPresent())) {
      return rpc.error(
          Error.INVALID_PARAMS,
          "Exactly one of `ids` or `query` must be present in request (at least one but not both)");
    }

    QueryRequest queryRequest;
    if (queryParams.isPresent()) {
      ImmutablePair<QueryRequest, Error> queryOrError =
          catalogMethods.getQueryRequest(queryParams.get());
      if (queryOrError.getRight() != null) {
        Error err = queryOrError.getRight();
        return rpc.error(
            Error.INVALID_PARAMS,
            "Issue encountered while getting query",
            mapOf("irritant", queryParams.get(), "path", asList("params", "query"), "cause", err));
      }
      queryRequest = queryOrError.getLeft();
    } else {
      if (ids.get().size() > ACTION_ID_QUERY_LIMIT) {
        return rpc.error(
            Error.INVALID_REQUEST,
            "No more than " + ACTION_ID_QUERY_LIMIT + " ids may be sent at a single time.",
            mapOf("path", asList("params", "ids")));
      }
      if (ids.get().isEmpty()) {
        return rpc.error(
            Error.INVALID_PARAMS,
            "Must specify at least one metacard id within `ids`",
            mapOf("path", asList("params", "ids")));
      }
      queryRequest = getIdRequest(ids.get());
    }
    QueryResponse queryResponse;
    try {
      queryResponse = catalogFramework.query(queryRequest);
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      return rpc.error(Error.INTERNAL_ERROR, e.getMessage());
    }

    // TODO (RCZ) - Send to action provider
    return mapOf(
        "actions",
        queryResponse
            .getResults()
            .stream()
            .map(Result::getMetacard)
            .collect(
                Collectors.toMap(
                    metacard -> metacard.getAttribute(Core.ID).getValue(), this::getActions)));
  }

  private QueryRequest getIdRequest(List<String> ids) {
    return new QueryRequestImpl(
        new QueryImpl(
            filterBuilder.anyOf(
                ids.stream()
                    .map(id -> filterBuilder.attribute(Core.ID).is().equalTo().text(id))
                    .collect(Collectors.toList())),
            1,
            ACTION_ID_QUERY_LIMIT,
            SortBy.NATURAL_ORDER,
            false,
            60_000));
  }

  private List<Action> getActions(Metacard metacard) {
    return actionRegistry
        .list(metacard)
        .stream()
        .map(
            action ->
                new ActionImpl(
                    action.getId(), action.getTitle(), action.getDescription(), action.getUrl()))
        .collect(Collectors.toList());
  }
}
