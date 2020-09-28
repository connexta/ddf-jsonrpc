package com.connexta.jsonrpc.email.impl.v_2_26;

import com.connexta.jsonrpc.email.EmailResolver;
import ddf.security.SubjectOperations;
import org.apache.shiro.SecurityUtils;

public class EmailImpl implements EmailResolver {

  private final SubjectOperations subjectOperations;

  public EmailImpl(SubjectOperations subjectOperations) {

    this.subjectOperations = subjectOperations;
  }

  @Override
  public String getCurrentSubjectEmail() {
    return subjectOperations.getEmailAddress(SecurityUtils.getSubject());
  }
}
