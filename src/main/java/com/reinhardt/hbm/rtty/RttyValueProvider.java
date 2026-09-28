package com.reinhardt.hbm.rtty;

/** A machine whose state can be published by an HBM RTTY reader. */
public interface RttyValueProvider extends RttyInfo {
    String provideRttyValue(String name);
}
