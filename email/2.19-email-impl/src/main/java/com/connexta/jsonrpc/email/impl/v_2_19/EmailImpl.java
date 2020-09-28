package com.connexta.jsonrpc.email.impl.v_2_19;

import com.connexta.jsonrpc.email.EmailResolver;
import ddf.security.SubjectUtils;
import org.apache.shiro.SecurityUtils;

public class EmailImpl implements EmailResolver {
  @Override
  public String getCurrentSubjectEmail() {
    return SubjectUtils.getEmailAddress(SecurityUtils.getSubject());
  }
}
