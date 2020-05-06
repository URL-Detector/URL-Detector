/**
 * Copyright 2015 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package com.linkedin.urls.detection;

/**
 * The options to use when detecting urls. This enum is used as a bit mask to be able to set multiple options at once.
 */
public class UrlDetectorOptions {

  /**
   * Default options, no special checks.
   */
  public static UrlDetectorOptions Default = new UrlDetectorOptions(0);

  /**
   * Matches quotes in the beginning and end of string.
   * If a string starts with a quote, then the ending quote will be eliminated. For example,
   * "http://linkedin.com" will pull out just 'http://linkedin.com' instead of 'http://linkedin.com"'
   */
  public static UrlDetectorOptions QUOTE_MATCH = new UrlDetectorOptions(1); // 00000001

  /**
   * Matches single quotes in the beginning and end of a string.
   */
  public static UrlDetectorOptions SINGLE_QUOTE_MATCH = new UrlDetectorOptions(2); // 00000010

  /**
   * Matches brackets and closes on the second one.
   * Same as quote matching but works for brackets such as (), {}, [].
   */
  public static UrlDetectorOptions BRACKET_MATCH = new UrlDetectorOptions(4); // 000000100

  /**
   * Checks for bracket characters and more importantly quotes to start and end strings.
   */
  public static UrlDetectorOptions JSON = new UrlDetectorOptions(5); //00000101

  /**
   * Checks JSON format or but also looks for a single quote.
   */
  public static UrlDetectorOptions JAVASCRIPT = new UrlDetectorOptions(7); //00000111

  /**
   * Checks for xml characters and uses them as ending characters as well as quotes.
   * This also includes quote_matching.
   */
  public static UrlDetectorOptions XML = new UrlDetectorOptions(9); //00001001

  /**
   * Checks all of the rules besides brackets. This is XML but also can contain javascript.
   */
  public static UrlDetectorOptions HTML = new UrlDetectorOptions(27); //00011011

  /**
   * Checks for single level domains as well. Ex: go/, http://localhost
   */
  public static UrlDetectorOptions ALLOW_SINGLE_LEVEL_DOMAIN = new UrlDetectorOptions(32); //00100000

  /**
   * Validates top level domains against IANA list
   * https://data.iana.org/TLD/tlds-alpha-by-domain.txt
   */
  public static UrlDetectorOptions VALIDATE_TOP_LEVEL_DOMAIN = new UrlDetectorOptions(64); //01000000

  /**
   * Compose two or more options, e.g. HTML and VALIDATE_TOP_LEVEL_DOMAIN.
   * ALLOW_SINGLE_LEVEL_DOMAIN can only be mixed with VALIDATE_TOP_LEVEL_DOMAIN.
   * @param options Two or more UrlDetectorOptions
   * @return A new UrlDetectorOptions as the bitwise or of the options.
   */
  public static UrlDetectorOptions compose(UrlDetectorOptions... options) {
    int composition = 0;
    boolean formattingOptionPresent = false;
    boolean allowSingleLevelDomainOptionPresent = false;

    for (UrlDetectorOptions value : options) {
      int option = value.getValue();

      if (option < ALLOW_SINGLE_LEVEL_DOMAIN.getValue()) formattingOptionPresent = true;
      if (option == ALLOW_SINGLE_LEVEL_DOMAIN.getValue()) allowSingleLevelDomainOptionPresent = true;
      if (formattingOptionPresent && allowSingleLevelDomainOptionPresent)
        throw new IllegalArgumentException(
          "ALLOW_SINGLE_LEVEL_DOMAIN cannot be mixed with formatting options (e.g. HTML)");

      composition |= option;
    };
    return new UrlDetectorOptions(composition);
  }
  /**
   * The numeric value.
   */
  private final int _value;

  /**
   * Creates a new Options enum
   * @param value The numeric value of the enum
   */
  private UrlDetectorOptions(int value) {
    this._value = value;
  }

  /**
   * Checks if the current options have the specified flag.
   * @param flag The flag to check for.
   * @return True if this flag is active, else false.
   */
  public boolean hasFlag(UrlDetectorOptions flag) {
    return (_value & flag._value) == flag._value;
  }

  /**
   * Gets the numeric value of the enum
   * @return The numeric value of the enum
   */
  public int getValue() {
    return _value;
  }
}
