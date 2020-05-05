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

import com.linkedin.urls.detection.CharUtils;
import java.net.IDN;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import com.ibm.icu.text.IDNA;

/**
 * Normalizes the host by converting hex characters to the actual textual representation, changes ip addresses
 * to a formal format. Then re-encodes the final host name.
 */
public class HostNormalizer {
  private static final long MAX_NUMERIC_DOMAIN_VALUE = 4294967295L;
  private static final int MAX_IPV4_PART = 255;
  private static final int MIN_IP_PART = 0;
  private static final int MAX_IPV6_PART = 0xFFFF;
  private static final int IPV4_MAPPED_IPV6_START_OFFSET = 12;
  private static final int NUMBER_BYTES_IN_IPV4 = 4;

  private static boolean icu4jAvailable = isIcu4jAvailable();
  private static IDNA _uts46 = null;
  private static int _uts46Options = setUTS46DefaultOptions();

  private static boolean isIcu4jAvailable() {
    try {
      Class.forName("com.ibm.icu.text.IDNA");
      return true;
    } catch (ClassNotFoundException ex) {
      System.err.println("icu4j library JAR not found. Using fallback java.net.IDN.");
      return false;
    }
  }

  private static int setUTS46DefaultOptions() {
    return icu4jAvailable ? IDNA.NONTRANSITIONAL_TO_ASCII : 0;
  }

  private static IDNA getIcu4jUTS46Instance(int options) {
    return IDNA.getUTS46Instance(options);
  }

  /**
   * Sets the options of the IDNA instance which implements UTS #46.
   * For available options, see
   * https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/IDNA.html#getUTS46Instance-int-
   *
   * @param options Bit set to modify the processing and error checking.
   * @return true if icu4j is available, false otherwise.
   */
  public static boolean setUts46Options(int options) {
    icu4jAvailable = isIcu4jAvailable();
    if (icu4jAvailable) {
      _uts46Options = options;
      _uts46 = getIcu4jUTS46Instance(options);
      return true;
    }
    return false;
  }

  /**
   * The uts46 instance is cached for multiple operations, it can be deleted when done.
   */
  public static void deleteUTS46Instance() {
    _uts46 = null;
  }

  private byte[] _bytes;
  private String _host;
  private String _normalizedHost;

  public HostNormalizer(String host) {
    _host = host;
    _bytes = null;

    normalizeHost();
  }

  private void normalizeHost() {
    if (StringUtils.isEmpty(_host)) {
      return;
    }

    String host;
    try {
      //replace high unicode characters
      if (icu4jAvailable) {
        if (_uts46 == null) _uts46 = getIcu4jUTS46Instance(_uts46Options);
        host = _uts46.nameToASCII(_host, new StringBuilder(), new IDNA.Info()).toString();
      } else {
        host = IDN.toASCII(_host, IDN.ALLOW_UNASSIGNED);
      }
    } catch (IllegalArgumentException ex) {
      //occurs when the url is invalid. Just return
      return;
    }

    host = host.toLowerCase();
    host = UrlUtil.decode(host);

    _bytes = tryDecodeHostToIp(host);

    if (_bytes != null) {
      InetAddress address;
      try {
        address = InetAddress.getByAddress(_bytes);
        String ipAddress = address.getHostAddress();
        if (address instanceof Inet6Address) {
          host = "[" + ipAddress + "]";
        } else {
          host = ipAddress;
        }
      } catch (UnknownHostException e) {
        return;
      }
    }

    if (StringUtils.isEmpty(host)) {
      return;
    }

    host = UrlUtil.removeExtraDots(host);

    _normalizedHost = UrlUtil.encode(host).replace("\\x", "%");
  }

  /**
   * Checks if the host is an ip address. Returns the byte representation of it
   */
  private byte[] tryDecodeHostToIp(String host) {
    if (host.startsWith("[") && host.endsWith("]")) {
      return tryDecodeHostToIPv6(host);
    }
    return tryDecodeHostToIPv4(host);
  }

  /**
   * This covers cases like:
   * Hexadecimal:        0x1283983
   * Decimal:            12839273
   * Octal:              037362273110
   * Dotted Decimal:     192.168.1.1
   * Dotted Hexadecimal: 0xfe.0x83.0x18.0x1
   * Dotted Octal:       0301.00.046.00
   * Dotted Mixed:       0x38.168.077.1
   *
   * if ipv4 was found, _bytes is set to the byte representation of the ipv4 address
   */
  private byte[] tryDecodeHostToIPv4(String host) {
    String[] parts = CharUtils.splitByDot(host);
    int numParts = parts.length;

    if (numParts != 4 && numParts != 1) {
      return null;
    }

    byte[] bytes = new byte[16];

    //An ipv4 mapped ipv6 bytes will have the 11th and 12th byte as 0xff
    bytes[10] = (byte) 0xff;
    bytes[11] = (byte) 0xff;
    for (int i = 0; i < parts.length; i++) {
      String parsedNum;
      int base;
      if (parts[i].startsWith("0x")) { //hex
        parsedNum = parts[i].substring(2);
        base = 16;
      } else if (parts[i].startsWith("0")) { //octal
        parsedNum = parts[i].substring(1);
        base = 8;
      } else { //decimal
        parsedNum = parts[i];
        base = 10;
      }

      Long section;
      try {
        section = parsedNum.isEmpty() ? 0 : Long.parseLong(parsedNum, base);
      } catch (NumberFormatException e) {
        return null;
      }

      if (numParts == 4 && section > MAX_IPV4_PART || //This would look like 288.1.2.4
          numParts == 1 && section > MAX_NUMERIC_DOMAIN_VALUE || //This would look like 4294967299
          section < MIN_IP_PART) {
        return null;
      }
      //bytes 13->16 is where the ipv4 address of an ipv4-mapped-ipv6-address is stored.
      if (numParts == 4) {
        bytes[IPV4_MAPPED_IPV6_START_OFFSET + i] = section.byteValue();
      } else { //numParts == 1
        int index = IPV4_MAPPED_IPV6_START_OFFSET;
        bytes[index++] = (byte) ((section >> 24) & 0xFF);
        bytes[index++] = (byte) ((section >> 16) & 0xFF);
        bytes[index++] = (byte) ((section >> 8) & 0xFF);
        bytes[index] = (byte) (section & 0xFF);
        return bytes;
      }
    }

    return bytes;
  }

  /**
   * Recommendation for IPv6 Address Text Representation
   * http://tools.ietf.org/html/rfc5952
   *
   * if ipv6 was found, _bytes is set to the byte representation of the ipv6 address
   */
  private byte[] tryDecodeHostToIPv6(String host) {
    String ip = host.substring(1, host.length() - 1);
    List<String> parts = new ArrayList<String>(Arrays.asList(ip.split(":", -1)));
    if (parts.size() < 3) {
      return null;
    }

    //Check for embedded ipv4 address
    String lastPart = parts.get(parts.size() - 1);
    int zoneIndexStart = lastPart.lastIndexOf("%");
    String lastPartWithoutZoneIndex = zoneIndexStart == -1 ? lastPart : lastPart.substring(0, zoneIndexStart);
    byte[] ipv4Address = null;
    if (!isHexSection(lastPartWithoutZoneIndex)) {
      ipv4Address = tryDecodeHostToIPv4(lastPartWithoutZoneIndex);
    }

    byte[] bytes = new byte[16];
    //How many parts do we need to fill by the end of this for loop?
    int totalSize = ipv4Address == null ? 8 : 6;
    //How many zeroes did we fill in the case of double colons? Ex: [::1] will have numberOfFilledZeroes = 7
    int numberOfFilledZeroes = 0;
    //How many sections do we have to parse through? Ex: [fe80:ff::192.168.1.1] size = 3, another ex: [a:a::] size = 4
    int size = ipv4Address == null ? parts.size() : parts.size() - 1;
    for (int i = 0; i < size; i++) {
      int lenPart = parts.get(i).length();
      if (lenPart == 0 && i != 0 && i != parts.size() - 1) {
        numberOfFilledZeroes = totalSize - size;
        for (int k = i; k < numberOfFilledZeroes + i; k++) {
          System.arraycopy(sectionToTwoBytes(0), 0, bytes, k * 2, 2);
        }
      }
      Integer section;
      try {
        section = lenPart == 0 ? 0 : Integer.parseInt(parts.get(i), 16);
      } catch (NumberFormatException e) {
        return null;
      }
      if (section > MAX_IPV6_PART || section < MIN_IP_PART) {
        return null;
      }
      System.arraycopy(sectionToTwoBytes(section), 0, bytes, (numberOfFilledZeroes + i) * 2, 2);
    }

    if (ipv4Address != null) {
      System.arraycopy(ipv4Address, IPV4_MAPPED_IPV6_START_OFFSET, bytes, IPV4_MAPPED_IPV6_START_OFFSET,
          NUMBER_BYTES_IN_IPV4);
    }
    return bytes;
  }

  private static boolean isHexSection(String section) {
    for (int i = 0; i < section.length(); i++) {
      if (!CharUtils.isHex(section.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static byte[] sectionToTwoBytes(int section) {
    byte[] bytes = new byte[2];
    bytes[0] = (byte) ((section >> 8) & 0xff);
    bytes[1] = (byte) (section & 0xff);
    return bytes;
  }

  protected byte[] getBytes() {
    return _bytes;
  }

  protected String getNormalizedHost() {
    return _normalizedHost;
  }
}
