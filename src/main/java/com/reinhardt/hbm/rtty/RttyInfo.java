package com.reinhardt.hbm.rtty;

/**
 * Introspection shared by HBM's redstone-over-radio components.
 *
 * <p>The prefixes are part of the 1.7.10 on-air protocol and therefore must
 * remain stable.  They are deliberately independent from computer-mod APIs.</p>
 */
public interface RttyInfo {
    String PREFIX_VALUE = "VAL:";
    String PREFIX_FUNCTION = "FUN:";

    String[] rttyFunctionInfo();
}
