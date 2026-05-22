/**
 * Copyright 2015 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package com.linkedin.urls.detection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link DomainNameReader} covering all logical branches:
 * readCurrent(), readDomainName(), checkDomainNameValid(), isValidIpv4(), isValidIpv6().
 */
class TestDomainNameReader {

  /** No-op character handler used for most tests. */
  private static final DomainNameReader.CharacterHandler NO_OP_HANDLER = character -> {};

  /** Java-8-compatible replacement for {@code String.repeat(n)}. */
  private static String repeat(char c, int n) {
    StringBuilder sb = new StringBuilder(n);
    for (int i = 0; i < n; i++) sb.append(c);
    return sb.toString();
  }

  /**
   * Helper: builds a reader whose "already-seen" text is {@code current} and whose
   * remaining stream is {@code remainder}.  The buffer is pre-populated with
   * {@code current} (matching what UrlDetector would have accumulated).
   */
  private DomainNameReader.ReaderNextState read(String current, String remainder, UrlDetectorOptions options) {
    StringBuilder buffer = new StringBuilder(current == null ? "" : current);
    InputTextReader reader = new InputTextReader(remainder);
    DomainNameReader dns = new DomainNameReader(reader, buffer, current, options, NO_OP_HANDLER);
    return dns.readDomainName();
  }

  private DomainNameReader.ReaderNextState read(String current, String remainder) {
    return read(current, remainder, UrlDetectorOptions.Default);
  }

  // -----------------------------------------------------------------------
  // readCurrent(): invalid "current" values
  // -----------------------------------------------------------------------

  @Test
  void testCurrentSingleDot_isInvalid() {
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read(".", "com"));
  }

  @Test
  void testCurrentHexEncodedDotOnly_isInvalid() {
    // "%2e" is a URL-encoded "." and should be rejected as the sole current value
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("%2e", "com"));
  }

  @Test
  void testCurrentLabelTooLong_isInvalid() {
    // A label of exactly 65 'a' chars exceeds MAX_LABEL_LENGTH (64)
    String longLabel = repeat('a', 65);
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read(longLabel, ".com"));
  }

  @Test
  void testCurrentWithInvalidCharCutsPrefix() {
    // "asdf%hello" – the '%' before 'h' (not a valid hex pair) is invalid, so readCurrent
    // cuts the buffer to "hello" (newStart=5). The reader then provides "world.com" which
    // continues the domain without a leading dot → valid domain "helloworld.com".
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("asdf%hello", "world.com"));
  }

  @Test
  void testCurrentWhereAllCharsInvalid_isInvalid() {
    // A single invalid char means newStart == current.length — entirely invalid
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("%", ".com"));
  }

  @Test
  void testCurrentCutResultsInDotOnly_isInvalid() {
    // After cutting, the remaining buffer is just "." which is also invalid
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("x%", "."));
  }

  @Test
  void testCurrentNull_isValid() {
    // null current means domain starts fresh from the reader
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read(null, "google.com"));
  }

  // -----------------------------------------------------------------------
  // readCurrent(): hex (0x) prefix in current
  // -----------------------------------------------------------------------

  @Test
  void testCurrentWithHexPrefix_treatedAsHex() {
    // "0xC0" is a valid hex literal; with 3 more dotted parts it forms a valid IPv4
    assertEquals(DomainNameReader.ReaderNextState.ReadPath, read("0xC0", ".0x00.0x02.0xEB/path"));
  }

  @Test
  void testCurrentWithHexEncodedDotInMiddle() {
    // A URL-encoded dot (%2e) acting as separator inside the current string
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("google%2ecom", ""));
  }

  // -----------------------------------------------------------------------
  // readDomainName(): terminator characters
  // -----------------------------------------------------------------------

  @Test
  void testSlashTerminator_returnsReadPath() {
    assertEquals(DomainNameReader.ReaderNextState.ReadPath, read("google", ".com/path"));
  }

  @Test
  void testColonTerminator_returnsReadPort() {
    assertEquals(DomainNameReader.ReaderNextState.ReadPort, read("google", ".com:8080"));
  }

  @Test
  void testQuestionMarkTerminator_returnsReadQueryString() {
    assertEquals(DomainNameReader.ReaderNextState.ReadQueryString, read("google", ".com?q=1"));
  }

  @Test
  void testHashTerminator_returnsReadFragment() {
    assertEquals(DomainNameReader.ReaderNextState.ReadFragment, read("google", ".com#section"));
  }

  @Test
  void testAtSign_returnsReadUserPass() {
    assertEquals(DomainNameReader.ReaderNextState.ReadUserPass, read("user", "@google.com"));
  }

  @Test
  void testEofAfterValidDomain_returnsValidDomainName() {
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("google", ".com"));
  }

  // -----------------------------------------------------------------------
  // readDomainName(): dot handling
  // -----------------------------------------------------------------------

  @Test
  void testDoubleDot_terminatesAndInvalid() {
    // "google..com" → double dot means _currentLabelLength < 1 after first dot → done=true
    // After stopping at the second dot the domain has only one label ("google"), which
    // has no TLD, so it is invalid.
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("google", "..com"));
  }

  @Test
  void testHexEncodedDotInRemainder() {
    // %2e is equivalent to '.' in the remaining stream
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("google", "%2ecom"));
  }

  @Test
  void testLabelExceedsMaxLengthAfterDot_isInvalid() {
    // After the dot the next label is 64 chars which equals MAX_LABEL_LENGTH → invalid (>= check)
    String longLabel = repeat('a', 64);
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("google", "." + longLabel));
  }

  // -----------------------------------------------------------------------
  // readDomainName(): dash and alphanumeric
  // -----------------------------------------------------------------------

  @Test
  void testDashInDomainName_isValid() {
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("my-host", ".example.com"));
  }

  @Test
  void testNumericSubdomain_isValid() {
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("192", ".168.1.1"));
  }

  // -----------------------------------------------------------------------
  // readDomainName(): URL-encoded chars (%xx)
  // -----------------------------------------------------------------------

  @Test
  void testUrlEncodedCharInRemainder_isValid() {
    // %41 = 'A' encoded; valid domain character so domain stays good
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("google", ".co%6d"));
  }

  // -----------------------------------------------------------------------
  // readDomainName(): international characters
  // -----------------------------------------------------------------------

  @Test
  void testInternationalChar_isValid() {
    // char value 192 (À) is >= INTERNATIONAL_CHAR_START and treated as valid domain char
    String international = "caf\u00e9";  // 'é' = 0xE9 = 233 >= 192
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read(international, ".fr"));
  }

  // -----------------------------------------------------------------------
  // readDomainName(): 0x prefix when current is empty/null (isAllHexSoFar)
  // -----------------------------------------------------------------------

  @Test
  void testHexAddressNullCurrent_isAlwaysInvalid() {
    // When current=null, _numeric is never initialised to true in readCurrent(),
    // so even a fully valid hex address read from the stream is not recognised as
    // IPv4 – the isAllHexSoFar branch in readDomainName() only guards _numeric from
    // being cleared, it never sets it.  Both a valid and an invalid range are thus invalid.
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read(null, "0x01010100"));
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read(null, "0x00000001"));
  }

  // -----------------------------------------------------------------------
  // checkDomainNameValid(): domain length / label count limits
  // -----------------------------------------------------------------------

  @Test
  void testDomainTooLong_isInvalid() {
    // Build a domain of > 255 chars: many short labels each 9 chars + dot
    // 26 labels * (9 chars + dot) = 260 chars
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 26; i++) {
      if (i > 0) sb.append(".");
      sb.append("abcdefghi");   // 9 chars
    }
    String domain = sb.toString();
    // Split into current (first label) and remainder (rest starting with '.')
    int dot = domain.indexOf('.');
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName,
        read(domain.substring(0, dot), domain.substring(dot)));
  }

  @Test
  void testTooManyLabels_isInvalid() {
    // 128 single-char labels → 128 "sections" which exceeds MAX_NUMBER_LABELS (127)
    StringBuilder remainder = new StringBuilder();
    for (int i = 0; i < 127; i++) {
      remainder.append(".a");
    }
    // current = "a", remainder has 127 more ".a" segments → 128 labels total
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("a", remainder.toString()));
  }

  // -----------------------------------------------------------------------
  // checkDomainNameValid(): TLD length
  // -----------------------------------------------------------------------

  @Test
  void testTldTooShort_isInvalid() {
    // TLD of 1 char is below MIN_TOP_LEVEL_DOMAIN (2)
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("google", ".c"));
  }

  @Test
  void testTldTooLong_isInvalid() {
    // TLD of 23 chars exceeds MAX_TOP_LEVEL_DOMAIN (22)
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName,
        read("google", "." + repeat('a', 23)));
  }

  @Test
  void testTldExactlyMaxLength_isValid() {
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName,
        read("google", "." + repeat('a', 22)));
  }

  @Test
  void testTldExactlyMinLength_isValid() {
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("google", ".uk"));
  }

  @Test
  void testXnPunycodeTld_isValid() {
    // xn-- prefix removes size restriction on TLD
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName,
        read("google", ".xn--p1ai")); // .рф in punycode
  }

  // -----------------------------------------------------------------------
  // checkDomainNameValid(): ALLOW_SINGLE_LEVEL_DOMAIN
  // -----------------------------------------------------------------------

  @Test
  void testSingleLevelDomainWithoutOption_isInvalid() {
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName,
        read("localhost", "", UrlDetectorOptions.Default));
  }

  @Test
  void testSingleLevelDomainWithOption_isValid() {
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName,
        read("localhost", "", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN));
  }

  @Test
  void testSingleLevelDomainWithSlash_isValid() {
    assertEquals(DomainNameReader.ReaderNextState.ReadPath,
        read("localhost", "/", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN));
  }

  // -----------------------------------------------------------------------
  // isValidIpv4(): no-dot formats
  // NOTE: _numeric is only set to true inside readCurrent() when current != null.
  //       For no-dot numeric addresses the value must be passed as `current`.
  // -----------------------------------------------------------------------

  @Test
  void testIpv4DecimalNoDot_validMin() {
    // MIN_NUMERIC_DOMAIN_VALUE = 16843008
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("16843008", ""));
  }

  @Test
  void testIpv4DecimalNoDot_validMax() {
    // MAX_NUMERIC_DOMAIN_VALUE = 4294967295 = 255.255.255.255
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("4294967295", ""));
  }

  @Test
  void testIpv4DecimalNoDot_belowMin_isInvalid() {
    // 16843007 < MIN_NUMERIC_DOMAIN_VALUE
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("16843007", ""));
  }

  @Test
  void testIpv4DecimalNoDot_aboveMax_isInvalid() {
    // 4294967296 > MAX_NUMERIC_DOMAIN_VALUE
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("4294967296", ""));
  }

  @Test
  void testIpv4HexNoDot_valid() {
    // 0x01010100 = 16843008 = MIN_NUMERIC_DOMAIN_VALUE; pass as current so _numeric is set
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("0x01010100", ""));
  }

  @Test
  void testIpv4OctalNoDot_valid() {
    // 0100200400 octal = 16843008 decimal = MIN_NUMERIC_DOMAIN_VALUE
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("0100200400", ""));
  }

  @Test
  void testIpv4OctalNoDot_invalidDigit() {
    // '8' is not a valid octal digit
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("08000000", ""));
  }

  // -----------------------------------------------------------------------
  // isValidIpv4(): dotted formats
  // -----------------------------------------------------------------------

  @Test
  void testIpv4DottedDecimal_valid() {
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("192", ".168.1.1"));
  }

  @Test
  void testIpv4DottedDecimal_partOutOfRange_isInvalid() {
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("256", ".168.1.1"));
  }

  @Test
  void testIpv4DottedDecimal_emptyPart_isInvalid() {
    // "192..168.1" produces an empty part
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("192", "..168.1.1"));
  }

  @Test
  void testIpv4DottedHex_valid() {
    // 0xC0.0x00.0x02.0xEB = 192.0.2.235
    assertEquals(DomainNameReader.ReaderNextState.ReadPath, read("0xC0", ".0x00.0x02.0xEB/"));
  }

  @Test
  void testIpv4DottedOctal_valid() {
    // 0300.0250.01.01 = 192.168.1.1
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("0300", ".0250.01.01"));
  }

  @Test
  void testIpv4DottedDecimal_withZeroPart_valid() {
    // "0" as a dotted part is classified as octal; parseLongSafe("0", startIndex=1, base=8)
    // hits the startIndex >= s.length() branch and returns 0 — valid since 0 ≤ 255.
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read("192", ".0.0.1"));
  }

  @Test
  void testIpv4DottedHex_partExceedsMaxIpPart_isInvalid() {
    // 0x100 = 256 > MAX_IP_PART (255); parseLongSafe detects overflow mid-loop and
    // returns maxValue+1, making the section check fail.
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("0x100", ".0.0.0"));
  }

  @Test
  void testIpv4WrongDotCount_isInvalid() {
    // Only 1 dot → _dots == 1, neither 0 nor 3 → invalid
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("192", ".168"));
  }

  @Test
  void testIpv4TwoDots_isInvalid() {
    // 2 dots → _dots == 2, not handled → invalid for purely numeric domain
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read("192", ".168.1"));
  }

  // -----------------------------------------------------------------------
  // isValidIpv6()
  // NOTE: IPv6 addresses must be in `remainder` (the reader stream).
  //       Passing them as `current` is broken because readCurrent() treats ':'
  //       as an invalid domain char and corrupts the buffer state.
  // -----------------------------------------------------------------------

  @Test
  void testIpv6Loopback_isValid() {
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read(null, "[::1]"));
  }

  @Test
  void testIpv6WithPort_returnsReadPort() {
    assertEquals(DomainNameReader.ReaderNextState.ReadPort, read(null, "[::1]:8080"));
  }

  @Test
  void testIpv6FullAddress_isValid() {
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName,
        read(null, "[2001:db8:85a3:0:0:8a2e:370:7334]"));
  }

  @Test
  void testIpv6WithDoubleColon_isValid() {
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName,
        read(null, "[2001:db8::1234]"));
  }

  @Test
  void testIpv6TooFewChars_isInvalid() {
    // "[:1]" – starts with "[:" but domainArray[2]='1' ≠ ':' → invalid per RFC
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read(null, "[:1]"));
  }

  @Test
  void testIpv6MissingClosingBracket_isInvalid() {
    // bracket was opened but never closed
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read(null, "[::1"));
  }

  @Test
  void testIpv6StartsWithSingleColon_isInvalid() {
    // [: followed by non-colon → invalid
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read(null, "[:8000:1::1]"));
  }

  @Test
  void testIpv6MultipleDoubleColons_isInvalid() {
    // [::1::2] has two "::" sequences
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read(null, "[::1::2]"));
  }

  @Test
  void testIpv6TooManySections_isInvalid() {
    // 9 groups → numSections > 8
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName,
        read(null, "[1:2:3:4:5:6:7:8:9]"));
  }

  @Test
  void testIpv6TooManyHexDigits_isInvalid() {
    // A group with 5 hex digits exceeds the 4-digit limit
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName,
        read(null, "[2001:db8::12345]"));
  }

  @Test
  void testIpv6SingleGroup_isInvalid() {
    // [adf] has numSections==1 and no doubleColon → invalid
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read(null, "[adf]"));
  }

  @Test
  void testIpv6WithZoneIndex_isValid() {
    // '%' starts zone index mode; subsequent unreserved chars (e.g. eth0) are OK
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read(null, "[fe80::1%eth0]"));
  }

  @Test
  void testIpv6WithInvalidZoneIndexChar_isInvalid() {
    // '!' is not an unreserved character; it also isn't a special terminator char, so it
    // causes reading to stop before the closing ']' → the buffer has no ']' → invalid.
    // (Note: '@' cannot be used here because it is caught earlier as a ReadUserPass trigger.)
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read(null, "[fe80::1%!bad]"));
  }

  @Test
  void testIpv6EmbeddedIpv4_isValid() {
    // [::ffff:192.168.1.1] — last section is parsed as IPv4
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName,
        read(null, "[::ffff:192.168.1.1]"));
  }

  @Test
  void testIpv6EmbeddedInvalidIpv4_isInvalid() {
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName,
        read(null, "[::ffff:999.999.999.999]"));
  }

  @Test
  void testIpv6DoubleColon_isValid() {
    // [::] spans all 128 bits with double-colon abbreviation
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read(null, "[::]"));
  }

  @Test
  void testIpv6SecondBracketRestartsReading() {
    // A second '[' inside an IPv6 address causes the reader to backtrack and return invalid
    assertEquals(DomainNameReader.ReaderNextState.InvalidDomainName, read(null, "[::1["));
  }

  @Test
  void testIpv6CompleteBracketFollowedByAlpha_returnsDone() {
    // [fe80::1] followed by www.google.com: reading stops after ']', domain is still valid
    assertEquals(DomainNameReader.ReaderNextState.ValidDomainName, read(null, "[fe80::1]www.google.com"));
  }
}

