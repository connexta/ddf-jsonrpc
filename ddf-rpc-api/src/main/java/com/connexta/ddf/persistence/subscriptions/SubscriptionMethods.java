package com.connexta.ddf.persistence.subscriptions;

import com.connexta.jsonrpc.Error;
import com.connexta.jsonrpc.MethodSet;
import com.connexta.jsonrpc.RpcFactory;
import com.connexta.jsonrpc.RpcMethod;
import com.connexta.jsonrpc.email.EmailResolver;
import com.connexta.util.MapFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.persistence.PersistentStore.PersistenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriptionMethods implements MethodSet {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionMethods.class);

  private static final String EMAIL_PROPERTY = "emails";

  private static final String SUBSCRIPTION_TYPE = PersistenceType.SUBSCRIPTION_TYPE.toString();

  private static final Lock LOCK = new ReentrantLock();

  private static final int START_INDEX = 0;

  private static final int PAGE_SIZE = 1000;

  private static final String ID = "id";

  public static final String IDS = "ids";

  private final PersistentStore persistentStore;

  private final EmailResolver emailResolver;

  private final Map<String, RpcMethod> methods;

  private final RpcFactory rpc;

  public SubscriptionMethods(
      PersistentStore persistentStore, EmailResolver emailResolver, RpcFactory rpcFactory) {
    this.persistentStore = persistentStore;
    this.emailResolver = emailResolver;
    this.rpc = rpcFactory;

    Map<String, RpcMethod> builder = new HashMap<>();
    builder.put(
        "subscriptions/isSubscribed",
        rpc.method(
            this::getSubscriptions,
            "Gets subscriptions for the current user for each of the metacard id parameters. `params` takes: `ids` (Required, value: List<String>). (Note: Maximum of 500 `ids` may be sent per request)"));
    builder.put(
        "subscriptions/getAll",
        rpc.method(this::getAllSubscriptions, "Gets all subscriptions for current user."));
    methods = builder;
  }

  public Object getAllSubscriptions(Map<String, Object> params) {
    return MapFactory.mapOf(
        "subscriptions", getSubscriptions(emailResolver.getCurrentSubjectEmail()));
  }

  public Object getSubscriptions(Map<String, Object> params) {

    if (!(params.get(IDS) instanceof List)) {
      return rpc.error(
          Error.INVALID_PARAMS,
          "Did not have a key `ids` that was a list of ids",
          MapFactory.mapOf("path", IDS));
    }
    List<String> ids = (List) params.get(IDS);

    if (ids.size() > 500) {
      return rpc.error(
          Error.INVALID_PARAMS,
          "Too many `ids` sent. Maximum of 500 `ids` per request",
          MapFactory.mapOf("path", IDS, "irritant", params.get(IDS)));
    }

    Set<String> subscriptions =
        new HashSet<>(getSubscriptions(emailResolver.getCurrentSubjectEmail()));

    Map<String, Boolean> resultMap =
        ids.stream()
            .collect(
                Collectors.toMap(Function.identity(), subscriptions::contains, (l, r) -> l || r));
    return MapFactory.mapOf("isSubscribed", resultMap);
  }

  private List<Map<String, Object>> query(String email) throws PersistenceException {
    String queryableEmail = "\"" + EMAIL_PROPERTY + "\"=" + "'" + email + "'";
    LOCK.lock();
    try {
      return persistentStore.get(SUBSCRIPTION_TYPE, queryableEmail, START_INDEX, PAGE_SIZE);
    } finally {
      LOCK.unlock();
    }
  }

  public List<String> getSubscriptions(String email) {
    if (email == null || email.trim().isEmpty()) {
      LOGGER.trace("No email given for subscriptions");
      return Collections.emptyList();
    }
    LOCK.lock();
    try {
      List<Map<String, Object>> results = query(email);
      List<String> ids =
          results
              .stream()
              .map(PersistentItem::stripSuffixes)
              .filter(result -> result.containsKey(ID))
              .map(result -> result.get(ID))
              .map(id -> id.toString())
              .collect(Collectors.toList());
      return ids;
    } catch (PersistenceException e) {
      LOGGER.warn("Unable to get workspace subscriptions for email: {}", email, e);
    } finally {
      LOCK.unlock();
    }

    return Collections.emptyList();
  }

  @Override
  public Map<String, RpcMethod> getMethods() {
    return methods;
  }
}
