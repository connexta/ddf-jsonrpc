package com.connexta.util;

public class ImmutablePair<L, R> {
  public final L left;
  public final R right;

  public static <L, R> ImmutablePair<L, R> pairOf(L left, R right) {
    return new ImmutablePair<L, R>(left, right);
  }

  public ImmutablePair(L left, R right) {
    this.left = left;
    this.right = right;
  }

  public L getLeft() {
    return left;
  }

  public R getRight() {
    return right;
  }
}
