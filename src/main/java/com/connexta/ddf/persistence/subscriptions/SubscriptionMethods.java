package com.connexta.ddf.persistence.subscriptions;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.persistence.PersistentStore.PersistenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriptionMethods {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionMethods.class);

  private static final String EMAIL_PROPERTY = "emails";

  private static final String SUBSCRIPTION_TYPE = PersistenceType.SUBSCRIPTION_TYPE.toString();

  private static final Lock LOCK = new ReentrantLock();

  private final PersistentStore persistentStore;

  private static final int START_INDEX = 0;

  private static final int PAGE_SIZE = 1000;

  private static final String ID = "id";

  public SubscriptionMethods(PersistentStore persistentStore) {
    this.persistentStore = persistentStore;
  }

  private List<Map<String, Object>> query(String email) throws PersistenceException {
    String queryableEmail = "\"" + EMAIL_PROPERTY + "\"=" + "'" + email + "'";
    LOCK.lock();
    try {
      List<Map<String, Object>> results =
          persistentStore.get(SUBSCRIPTION_TYPE, queryableEmail, START_INDEX, PAGE_SIZE);
      return results;
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
}
