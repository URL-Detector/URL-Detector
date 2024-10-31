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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


class TestCharUtils {

  @ParameterizedTest
  @ValueSource(chars = { 'a', 'A', '0', '9' })
  void testCharUtilsIsHex(char a) {
      assertTrue(CharUtils.isHex(a));
  }

  @ParameterizedTest
  @ValueSource(chars = { '~', ';', 'Z', 'g' })
  void testCharUtilsIsNotHex(char a) {
      assertFalse(CharUtils.isHex(a));
  }

  @ParameterizedTest
  @ValueSource(chars = { '0', '4', '6', '9' })
  void testCharUtilsIsNumeric(char a) {
    assertTrue(CharUtils.isNumeric(a));
  }

  @ParameterizedTest
  @ValueSource(chars = { 'a', '~', 'A', 0 })
  void testCharUtilsIsNotNumeric(char a) {
    assertFalse(CharUtils.isNumeric(a));
  }

  @ParameterizedTest
  @ValueSource(chars = { 'a', 'Z', 'f', 'X' })
  void testCharUtilsIsAlpha(char a) {
    assertTrue(CharUtils.isAlpha(a));
  }

  @ParameterizedTest
  @ValueSource(chars = { '0', '9', '[', '~' })
  void testCharUtilsIsNotAlpha(char a) {
    assertFalse(CharUtils.isAlpha(a));
  }

  @ParameterizedTest
  @ValueSource(chars = { 'a', 'G', '3', '9' })
  void testCharUtilsIsAlphaNumeric(char a) {
    assertTrue(CharUtils.isAlphaNumeric(a));
  }

  @ParameterizedTest
  @ValueSource(chars = { '~', '-', '_', '\n' })
  void testCharUtilsIsNotAlphaNumeric(char a) {
    assertFalse(CharUtils.isAlphaNumeric(a));
  }

  @ParameterizedTest
  @ValueSource(chars = { '-', '.', 'a', '9', 'Z', '_', 'f' })
  void testCharUtilsIsUnreserved(char a) {
    assertTrue(CharUtils.isUnreserved(a));
  }

  @ParameterizedTest
  @ValueSource(chars = { ' ', '!', '(', '\n' })
  void testCharUtilsIsNotUnreserved(char a) {
    assertFalse(CharUtils.isUnreserved(a));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "192.168.1.1",
    "..",
    "192%2e168%2e1%2e1",
    "asdf",
    "192.39%2e1%2E1",
    "as\uFF61awe.a3r23.lkajsf0ijr....",
    "%2e%2easdf",
    "sdoijf%2e",
    "ksjdfh.asdfkj.we%2",
    "0xc0%2e0x00%2e0x02%2e0xeb",
    ""
  })
  void testSplitByDot(String stringToSplit) {
    assertArrayEquals(CharUtils.splitByDot(stringToSplit), stringToSplit.split("[\\.\u3002\uFF0E\uFF61]|%2e|%2E", -1));
  }

}
