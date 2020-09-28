package com.connexta.util;

public class StringUtils {
  public static boolean isBlank(String str) {
    if (str == null || str.isEmpty()) {
      return true;
    }
    return str.chars().allMatch(Character::isWhitespace);
  }
}
