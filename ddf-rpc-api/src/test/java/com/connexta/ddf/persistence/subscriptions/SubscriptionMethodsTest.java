package com.connexta.ddf.persistence.subscriptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.connexta.jsonrpc.RpcFactory;
import com.connexta.jsonrpc.email.EmailResolver;
import com.connexta.jsonrpc.impl.RpcFactoryImpl;
import com.connexta.util.MapFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionMethodsTest {
  private static SubscriptionMethods testSubscription;

  private static List<Map<String, Object>> testSubscriptions;

  RpcFactory rpcFactory = new RpcFactoryImpl();

  @Mock private PersistentStore persistentStore;

  @Mock private EmailResolver emailResolver;

  private static List<String> resultingSubscriptionList =
      new ArrayList<String>(Arrays.asList("0", "1", "2", "3", "4", "5"));

  @BeforeEach
  public void setUp() {
    testSubscriptions = new ArrayList<>();
    initMocks(this);
    testSubscription = new SubscriptionMethods(persistentStore, emailResolver, rpcFactory);
    for (int i = 0; i < 6; i++) {
      testSubscriptions.add(MapFactory.mapOf("id", i));
    }
  }

  @Test
  public void testEmptyEmail() {
    List<String> results = testSubscription.getSubscriptions("");
    assertThat(results, is(Collections.emptyList()));
  }

  @Test
  public void testNoSubscriptions() throws PersistenceException {
    when(persistentStore.get(any(String.class), any(String.class), anyInt(), anyInt()))
        .thenReturn(Collections.emptyList());
    List<String> results = testSubscription.getSubscriptions("test@email.com");
    assertThat(results, is(Collections.emptyList()));
  }

  @Test
  public void testGetSubscriptions() throws PersistenceException {
    when(persistentStore.get(any(String.class), any(String.class), anyInt(), anyInt()))
        .thenReturn(testSubscriptions);
    List<String> results = testSubscription.getSubscriptions("test@email.com");
    assertThat(results, is(resultingSubscriptionList));
  }
}
