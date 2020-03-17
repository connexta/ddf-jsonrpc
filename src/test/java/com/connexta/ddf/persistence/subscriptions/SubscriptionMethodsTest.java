package com.connexta.ddf.persistence.subscriptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class SubscriptionMethodsTest {
  private static SubscriptionMethods testSubscription;

  private static List<Map<String, Object>> testSubscriptions;

  @Mock private PersistentStore persistentStore;

  private static List<String> resultingSubscriptionList =
      new ArrayList<String>(Arrays.asList("0", "1", "2", "3", "4", "5"));

  @Before
  public void setUp() throws Exception {
    testSubscriptions = new ArrayList<>();
    initMocks(this);
    testSubscription = new SubscriptionMethods(persistentStore);
    for (int i = 0; i < 6; i++) {
      testSubscriptions.add(
          new ImmutableMap.Builder<String, Object>().put("id", new Integer(i)).build());
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
