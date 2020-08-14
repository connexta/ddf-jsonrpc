package com.connexta.ddf.catalog.direct;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.connexta.jsonrpc.RpcMethod;
import com.connexta.jsonrpc.impl.Error;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Security;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class ExtendedMethodsTest {

  private static final String ID = "anId";

  private static final String QUERY_ID = "queryId";

  private static final String QUERIES_ATTRIBUTE = "queries";

  private static final String CQL_ATTRIBUTE = "cql";

  private static final String QUERY_TAG = "query";

  private static final String QUERY_LITERAL = "this is a query";

  private static final String TITLE_LITERAL = "this is a title";

  private static final String WORKSPACE_TAG = "workspace";

  private static final String SOURCE_ID = "aSource";

  private ExtendedMethods extendedMethods;

  private static final List<String> CLONEABLE_ASSOCIATION_ATTRIBUTES =
      Collections.singletonList(QUERIES_ATTRIBUTE);

  private RpcMethod cloneMethod;

  @Mock private CreateResponse createResponse;

  @Mock private CatalogFramework catalogFramework;

  @Mock private QueryResponse queryResponse;

  @Before
  public void setUp() throws Exception {
    initMocks(this);

    Metacard metacard1 = generateWorkspaceMetacard();
    Metacard metacard2 = generateQueryMetacard();
    Metacard metacard3 = new MetacardImpl();
    metacard3.setSourceId(SOURCE_ID);

    List<Result> workspaceResultList = Collections.singletonList(new ResultImpl(metacard1));
    List<Result> queryResultList = Collections.singletonList(new ResultImpl(metacard2));

    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(queryResponse);
    when(catalogFramework.create(any(CreateRequest.class))).thenReturn(createResponse);

    when(queryResponse.getResults()).thenReturn(workspaceResultList).thenReturn(queryResultList);

    when(createResponse.getCreatedMetacards()).thenReturn(Collections.singletonList(metacard3));

    FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
    extendedMethods = new ExtendedMethods(filterBuilder, catalogFramework);
    extendedMethods.setCloneableAssociationAttributes(CLONEABLE_ASSOCIATION_ATTRIBUTES);

    cloneMethod = getCloneDocMethod();
  }

  @Test
  public void testCloneableAssociationAttributes() {
    assertThat(
        extendedMethods.getCloneableAssociationAttributes(), is(CLONEABLE_ASSOCIATION_ATTRIBUTES));
  }

  @Test
  public void testCloneWithoutId() {
    Map<String, Object> idMap = new HashMap<>();
    Object resultObject = cloneMethod.apply(idMap);
    assertThat(resultObject, instanceOf(Error.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCloneUnsupportedQueryException() throws Exception {
    when(catalogFramework.query(any(QueryRequest.class)))
        .thenThrow(UnsupportedQueryException.class);
    Map<String, Object> idMap = getIdMap();
    Object resultObject = cloneMethod.apply(idMap);
    assertThat(resultObject, instanceOf(Error.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCloneSourceUnavailableException() throws Exception {
    when(catalogFramework.query(any(QueryRequest.class)))
        .thenThrow(SourceUnavailableException.class);
    Map<String, Object> idMap = getIdMap();
    Object resultObject = cloneMethod.apply(idMap);
    assertThat(resultObject, instanceOf(Error.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCloneFederationException() throws Exception {
    when(catalogFramework.query(any(QueryRequest.class))).thenThrow(FederationException.class);
    Map<String, Object> idMap = getIdMap();
    Object resultObject = cloneMethod.apply(idMap);
    assertThat(resultObject, instanceOf(Error.class));
  }

  @Test
  public void testCloneNoMetacardFound() {
    when(queryResponse.getResults()).thenReturn(new ArrayList<>());
    Map<String, Object> idMap = getIdMap();
    Object resultObject = cloneMethod.apply(idMap);
    assertThat(resultObject, instanceOf(Error.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCloneIngestException() throws Exception {
    when(catalogFramework.create(any(CreateRequest.class))).thenThrow(IngestException.class);
    Map<String, Object> idMap = getIdMap();
    Object resultObject = cloneMethod.apply(idMap);
    assertThat(resultObject, instanceOf(Error.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCloneSourceUnavailableExceptionOnCreate() throws Exception {
    when(catalogFramework.create(any(CreateRequest.class)))
        .thenThrow(SourceUnavailableException.class);
    Map<String, Object> idMap = getIdMap();
    Object resultObject = cloneMethod.apply(idMap);
    assertThat(resultObject, instanceOf(Error.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCloneAttributesAreCleared() throws Exception {
    Map<String, Object> idMap = getIdMap();
    cloneMethod.apply(idMap);

    ArgumentCaptor<CreateRequest> createRequestArgumentCaptor =
        ArgumentCaptor.forClass(CreateRequest.class);

    verify(catalogFramework, times(2)).create(createRequestArgumentCaptor.capture());

    List<CreateRequest> createRequest = createRequestArgumentCaptor.getAllValues();
    List<Metacard> clonedMetacards = createRequest.get(1).getMetacards();

    assertThat(clonedMetacards, hasSize(1));
    Metacard clonedMetacard = clonedMetacards.get(0);
    assertThat(clonedMetacard.getAttribute(Core.ID), nullValue());
    assertThat(clonedMetacard.getAttribute(Security.ACCESS_INDIVIDUALS), nullValue());
    assertThat(clonedMetacard.getAttribute(Core.METACARD_TAGS).getValue(), is(WORKSPACE_TAG));

    List<Metacard> queryMetacards = createRequest.get(0).getMetacards();

    assertThat(queryMetacards, hasSize(1));
    Metacard queryMetacard = queryMetacards.get(0);
    assertThat(queryMetacard.getAttribute(Core.ID), nullValue());
    assertThat(queryMetacard.getAttribute(Core.METACARD_TAGS).getValue(), is(QUERY_TAG));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCloneAttributesAreCloned() throws Exception {
    Map<String, Object> idMap = getIdMap();
    cloneMethod.apply(idMap);

    ArgumentCaptor<CreateRequest> createRequestArgumentCaptor =
        ArgumentCaptor.forClass(CreateRequest.class);

    verify(catalogFramework, times(2)).create(createRequestArgumentCaptor.capture());

    List<CreateRequest> createRequest = createRequestArgumentCaptor.getAllValues();
    List<Metacard> clonedMetacards = createRequest.get(1).getMetacards();
    List<Metacard> queryMetacards = createRequest.get(0).getMetacards();

    assertThat(clonedMetacards, hasSize(1));
    assertThat(queryMetacards, hasSize(1));

    Metacard clonedMetacard = clonedMetacards.get(0);
    Metacard queryMetacard = queryMetacards.get(0);
    assertThat(queryMetacard.getAttribute(CQL_ATTRIBUTE).getValue(), is(QUERY_LITERAL));
    assertThat(clonedMetacard.getAttribute(Core.TITLE).getValue(), is(TITLE_LITERAL));
  }

  private Metacard generateWorkspaceMetacard() {
    Metacard metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl(Core.ID, ID));
    metacard.setAttribute(new AttributeImpl(Core.TITLE, TITLE_LITERAL));
    metacard.setAttribute(new AttributeImpl(QUERIES_ATTRIBUTE, QUERY_ID));
    metacard.setAttribute(new AttributeImpl(Core.METACARD_TAGS, WORKSPACE_TAG));
    metacard.setAttribute(new AttributeImpl(Security.ACCESS_INDIVIDUALS, "bob"));
    metacard.setSourceId(SOURCE_ID);
    return metacard;
  }

  private Metacard generateQueryMetacard() {
    Metacard metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl(Core.ID, QUERY_ID));
    metacard.setAttribute(new AttributeImpl(Core.METACARD_TAGS, QUERY_TAG));
    metacard.setAttribute(new AttributeImpl(CQL_ATTRIBUTE, QUERY_LITERAL));
    metacard.setSourceId(SOURCE_ID);
    return metacard;
  }

  private Map<String, Object> getIdMap() {
    Map<String, Object> idMap = new HashMap<>();
    idMap.put(Core.ID, ID);
    return idMap;
  }

  private RpcMethod getCloneDocMethod() {
    Map<String, RpcMethod> map = extendedMethods.getMethods();
    return map.get(ExtendedMethods.CLONE_KEY);
  }
}
