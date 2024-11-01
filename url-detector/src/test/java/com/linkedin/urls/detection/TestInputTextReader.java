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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


class TestInputTextReader {
  private static final String CONTENT = "HELLO WORLD";

  @Test
  void testSimpleRead() {
    InputTextReader reader = new InputTextReader(CONTENT);
    for (int i = 0; i < CONTENT.length(); i++) {
      assertEquals(reader.read(), CONTENT.charAt(i));
    }
  }

  @Test
  void testEOF() {
    InputTextReader reader = new InputTextReader(CONTENT);
    for (int i = 0; i < CONTENT.length() - 1; i++) {
      reader.read();
    }

    assertFalse(reader.eof());
    reader.read();
    assertTrue(reader.eof());
  }

  @Test
  void testGoBack() {
    InputTextReader reader = new InputTextReader(CONTENT);
    assertEquals(reader.read(), CONTENT.charAt(0));
    reader.goBack();
    assertEquals(reader.read(), CONTENT.charAt(0));
    assertEquals(reader.read(), CONTENT.charAt(1));
    assertEquals(reader.read(), CONTENT.charAt(2));
    reader.goBack();
    reader.goBack();
    assertEquals(reader.read(), CONTENT.charAt(1));
    assertEquals(reader.read(), CONTENT.charAt(2));
  }

  @Test
  void testSeek() {
    InputTextReader reader = new InputTextReader(CONTENT);
    reader.seek(4);
    assertEquals(reader.read(), CONTENT.charAt(4));

    reader.seek(1);
    assertEquals(reader.read(), CONTENT.charAt(1));
  }
}
