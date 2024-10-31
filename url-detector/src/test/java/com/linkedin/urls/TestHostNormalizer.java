/**
 * Copyright 2015 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package com.linkedin.urls;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;


class TestHostNormalizer {

  @ParameterizedTest
  @CsvSource({
    "[fefe::],                           [fefe:0:0:0:0:0:0:0]",
    "[::ffff],                           [0:0:0:0:0:0:0:ffff]",
    "[::255.255.255.255],                [0:0:0:0:0:0:ffff:ffff]",
    "[::],                               [0:0:0:0:0:0:0:0]",
    "[::1],                              [0:0:0:0:0:0:0:1]",
    "[aAaA::56.7.7.5],                   [aaaa:0:0:0:0:0:3807:705]",
    "[BBBB:ab:f78F:f:DDDD:bab:56.7.7.5], [bbbb:ab:f78f:f:dddd:bab:3807:705]",
    "[Aaaa::1],                          [aaaa:0:0:0:0:0:0:1]",
    "[::192.167.2.2],                    [0:0:0:0:0:0:c0a7:202]",
    "[0:ffff::077.0x22.222.11],          [0:ffff:0:0:0:0:3f22:de0b]",
    "[0::ffff:077.0x22.222.11],          63.34.222.11",
    "192.168.1.1,                        192.168.1.1",
    "0x92.168.1.1,                       146.168.1.1",
    "3279880203,                         195.127.0.11"
  })
  void testIpHostNormalizationAndGetBytes(String original, String expectedHost) throws UnknownHostException {
    HostNormalizer hostNormalizer = new HostNormalizer(original);
    assertEquals(hostNormalizer.getNormalizedHost(), expectedHost);

    InetAddress address = InetAddress.getByName(expectedHost);
    byte[] expectedBytes;
    if (address instanceof Inet4Address) {
      expectedBytes = new byte[16];
      expectedBytes[10] = (byte) 0xff;
      expectedBytes[11] = (byte) 0xff;
      System.arraycopy(address.getAddress(), 0, expectedBytes, 12, 4);
    } else {
      expectedBytes = address.getAddress();
    }
    assertArrayEquals(hostNormalizer.getBytes(), expectedBytes);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "sALes.com",
    "33r.nEt",
    "173839.com",
    "192.168.-3.1",
    "[::-34:50]",
    "[-34::192.168.34.-3]"
  })
  void testSanityAddresses(String host) {
    HostNormalizer hostNormalizer = new HostNormalizer(host);
    assertEquals(hostNormalizer.getNormalizedHost(), host.toLowerCase());
    assertNull(hostNormalizer.getBytes());
  }
}
