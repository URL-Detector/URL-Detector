/**
 * Copyright 2014 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package com.linkedin.urls.detection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.linkedin.urls.Url;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;


class TestUriDetection {

  @Test
  void testBasicString() {
    runTest("hello world", UrlDetectorOptions.Default);
  }

  @Test
  void testBasicDetect() {
    runTest("this is a link: www.google.com", UrlDetectorOptions.Default, "www.google.com");
  }

  @Test
  void testExtendedIANAFailure() {
    UrlDetector parser = new UrlDetector("telnet://example.com", UrlDetectorOptions.Default);
    List<Url> found = parser.detect();
    for (Url url : found) {
      assertEquals(url.getScheme(),"http"); // Should still find the link but will assume it's HTTP under default options
    }
  }

  @Test
  void testExtendedIANASuccess() {
    UrlDetector parser = new UrlDetector("telnet://example.com", UrlDetectorOptions.EXTENDED_IANA_DETECTION);
    List<Url> found = parser.detect();
    for (Url url : found) {
      assertEquals(url.getScheme(),"telnet"); // Should find the link and set the scheme to the correct "telnet" value
    }
  }

  @Test
  void testEmailAndNormalUrl() {
    runTest("my email is vshlosbe@linkedin.com and my site is http://www.linkedin.com/vshlos",
        UrlDetectorOptions.Default, "vshlosbe@linkedin.com", "http://www.linkedin.com/vshlos");
  }

  @Test
  void testTwoBasicUrls() {
    runTest("the url google.com is a lot better then www.google.com.", UrlDetectorOptions.Default, "google.com",
        "www.google.com.");
  }

  @Test
  void testLongUrl() {
    runTest("google.com.google.com is kind of a valid url", UrlDetectorOptions.Default, "google.com.google.com");
  }

  @Test
  void testInternationalUrls() {
    runTest("this is an international domain: http://\u043F\u0440\u0438\u043c\u0435\u0440.\u0438\u0441\u043f\u044b"
        + "\u0442\u0430\u043d\u0438\u0435 so is this: \u4e94\u7926\u767c\u5c55.\u4e2d\u570b.",
        UrlDetectorOptions.Default,
        "http://\u043F\u0440\u0438\u043c\u0435\u0440.\u0438\u0441\u043f\u044b\u0442\u0430\u043d\u0438\u0435",
        "\u4e94\u7926\u767c\u5c55.\u4e2d\u570b.");
  }

  @Test
  void testInternationalUrlsInHtml() {
    runTest(
        "<a rel=\"nofollow\" class=\"external text\" href=\"http://xn--mgbh0fb.xn--kgbechtv/\">http://\u1605\u1579\u1575\u1604.\u1573\u1582\u1578\u1576\u1575\u1585</a>",
        UrlDetectorOptions.HTML, "http://xn--mgbh0fb.xn--kgbechtv/",
        "http://\u1605\u1579\u1575\u1604.\u1573\u1582\u1578\u1576\u1575\u1585");
  }

  @Test
  void testDomainWithUsernameAndPassword() {
    runTest("domain with username is http://username:password@www.google.com/site/1/2", UrlDetectorOptions.Default,
        "http://username:password@www.google.com/site/1/2");
  }

  @Test
  void testFTPWithUsernameAndPassword() {
    runTest("ftp with username is ftp://username:password@www.google.com", UrlDetectorOptions.Default,
        "ftp://username:password@www.google.com");
  }

  @Test
  void testUncommonFormatUsernameAndPassword() {
    runTest("weird url with username is username:password@www.google.com", UrlDetectorOptions.Default,
        "username:password@www.google.com");
  }

  @Test
  void testEmailAndLinkWithUserPass() {
    runTest("email and username is hello@test.google.com or hello@www.google.com hello:password@www.google.com",
        UrlDetectorOptions.Default, "hello@test.google.com", "hello@www.google.com", "hello:password@www.google.com");
  }

  @Test
  void testWrongSpacingInSentence() {
    runTest("I would not like to work at salesforce.com, it looks like a crap company.and not cool!",
        UrlDetectorOptions.Default, "salesforce.com", "company.and");
  }

  @Test
  void testNumbersAreNotDetected() {
    //make sure pure numbers don't work, but domains with numbers do.
    runTest("Do numbers work? such as 3.1415 or 4.com", UrlDetectorOptions.Default, "4.com");
  }

  @Test
  void testNewLinesAndTabsAreDelimiters() {
    runTest(
        "Do newlines and tabs break? google.com/hello/\nworld www.yahoo.com\t/stuff/ yahoo.com/\thello news.ycombinator.com\u0000/hello world",
        UrlDetectorOptions.Default,

        "google.com/hello/", "www.yahoo.com", "yahoo.com/", "news.ycombinator.com");
  }

  @Test
  void testIpAddressFormat() {
    runTest(
        "How about IP addresses? fake: 1.1.1 1.1.1.1.1 0.0.0.256 255.255.255.256 real: 1.1.1.1 192.168.10.1 1.1.1.1.com 255.255.255.255",
        UrlDetectorOptions.Default, "1.1.1.1", "192.168.10.1", "1.1.1.1.com", "255.255.255.255");
  }

  @Test
  void testNumericIpAddress() {
    runTest("http://3232235521/helloworld", UrlDetectorOptions.Default, "http://3232235521/helloworld");
  }

  @Test
  void testNumericIpAddressWithPort() {
    runTest("http://3232235521:8080/helloworld", UrlDetectorOptions.Default, "http://3232235521:8080/helloworld");
  }

  @Test
  void testDomainAndLabelSizeConstraints() {
    //Really long addresses testing rules about total length of domain name and number of labels in a domain and size of each label.
    runTest(
        "This will work: 1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.a.b.c.d.e.ly "
            + "This will not work:  1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.a.b.c.d.e.f.ly "
            + "This should as well: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc.dddddddddddddddddddddddddddddddddddddddddddddddddddddd.bit.ly "
            + "But this wont: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc.dddddddddddddddddddddddddddddddddddddddddddddddddddddd.bit.ly.dbl.spamhaus.org",
        UrlDetectorOptions.Default,
        "1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.a.b.c.d.e.ly",
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc.dddddddddddddddddddddddddddddddddddddddddddddddddddddd.bit.ly");
  }

  @Test
  void testBasicHtml() {
    runTest(
        "<script type=\"text/javascript\">var a = 'http://www.abc.com', b=\"www.def.com\"</script><a href=\"http://www.google.com\">google.com</a>",
        UrlDetectorOptions.HTML, "http://www.google.com", "http://www.abc.com", "www.def.com", "google.com");
  }

  @Test
  void testLongUrlWithInheritedScheme() {
    runTest(
        "<link rel=\"stylesheet\" href=\"//bits.wikimedia.org/en.wikipedia.org/load.php?debug=false&amp;lang=en&amp;modules=ext.gadget.DRN-wizard%2CReferenceTooltips%2Ccharinsert%2Cteahouse%7Cext.wikihiero%7Cmediawiki.legacy.commonPrint%2Cshared%7Cmw.PopUpMediaTransform%7Cskins.vector&amp;only=styles&amp;skin=vector&amp;*\" />",
        UrlDetectorOptions.HTML,
        "//bits.wikimedia.org/en.wikipedia.org/load.php?debug=false&amp;lang=en&amp;modules=ext.gadget.DRN-wizard%2CReferenceTooltips%2Ccharinsert%2Cteahouse%7Cext.wikihiero%7Cmediawiki.legacy.commonPrint%2Cshared%7Cmw.PopUpMediaTransform%7Cskins.vector&amp;only=styles&amp;skin=vector&amp;*");
  }

  @Test
  void testQuoteMatching() {
    //test quote matching with no html
    runTest(
        "my website is \"www.google.com\" but my email is \"vshlos@gmail.com\" \" www.abcd.com\" \" hello.com \"www.abc.com\"",
        UrlDetectorOptions.QUOTE_MATCH, "www.google.com", "vshlos@gmail.com", "www.abcd.com", "hello.com",
        "www.abc.com");
  }

  @Test
  void testIncorrectParsingHtmlWithBadOptions() {
    runTest("<a href=\"http://www.google.com/\">google.com</a>", UrlDetectorOptions.Default,
        "http://www.google.com/\">google.com</a>");
  }

  @Test
  void testBracketMatching() {
    runTest(
        "MY url (www.google.com) is very cool. the domain [www.google.com] is popular and when written like this {www.google.com} it looks like code",
        UrlDetectorOptions.BRACKET_MATCH, "www.google.com", "www.google.com", "www.google.com");
  }

  @Test
  void testParseJson() {
    runTest("{\"url\": \"www.google.com\", \"hello\": \"world\", \"anotherUrl\":\"http://www.yahoo.com\"}",
        UrlDetectorOptions.JSON, "www.google.com", "http://www.yahoo.com");
  }

  @Test
  void testParseJavascript() {
    runTest("var url = 'www.abc.com';\n" + "var url = \"www.def.com\";", UrlDetectorOptions.JAVASCRIPT, "www.abc.com",
        "www.def.com");
  }

  @Test
  void testParseXml() {
    runTest("<url attr=\"www.def.com\">www.abc.com</url><url href=\"hello.com\" />", UrlDetectorOptions.XML,
        "www.abc.com", "www.def.com", "hello.com");
  }

  @Test
  void testNonStandardDots() {
    runTest(
        "www\u3002google\u3002com username:password@www\uFF0Eyahoo\uFF0Ecom http://www\uFF61facebook\uFF61com http://192\u3002168\uFF0E0\uFF611/",
        UrlDetectorOptions.Default,

        "www\u3002google\u3002com", "username:password@www\uFF0Eyahoo\uFF0Ecom", "http://www\uFF61facebook\uFF61com",
        "http://192\u3002168\uFF0E0\uFF611/");
  }

  @Test
  void testInvalidPartsUrl() {
    runTest("aksdhf http://asdf#asdf.google.com", UrlDetectorOptions.Default, "asdf.google.com");
    runTest("00:41.<google.com/>", UrlDetectorOptions.HTML, "google.com/");
  }

  @Test
  void testNonStandardDotsBacktracking() {
    runTest("\u9053 \u83dc\u3002\u3002\u3002\u3002", UrlDetectorOptions.Default);
  }

  @Test
  void testBacktrackingStrangeFormats() {
    runTest("http:http:http://www.google.com www.www:yahoo.com yahoo.com.br hello.hello..hello.com",
        UrlDetectorOptions.Default, "www.www", "hello.hello.", "http://www.google.com", "yahoo.com", "yahoo.com.br",
        "hello.com");
  }

  @Test
  void testBacktrackingUsernamePassword() {
    runTest("check out my url:www.google.com", UrlDetectorOptions.Default, "www.google.com");
    runTest("check out my url:www.google.com ", UrlDetectorOptions.Default, "www.google.com");
  }

  @Test
  void testBacktrackingEmptyDomainName() {
    runTest("check out my http:///hello", UrlDetectorOptions.Default);
    runTest("check out my http://./hello", UrlDetectorOptions.Default);
  }

  @Test
  void testDoubleScheme() {
    runTest("http://http://", UrlDetectorOptions.Default);
    runTest("hello http://http://", UrlDetectorOptions.Default);
  }

  @Test
  void testMultipleSchemes() {
    runTest("http://http://www.google.com", UrlDetectorOptions.Default, "http://www.google.com");
    runTest("make sure it's right here http://http://www.google.com", UrlDetectorOptions.Default,
        "http://www.google.com");
    runTest("http://http://http://www.google.com", UrlDetectorOptions.Default, "http://www.google.com");
    runTest("make sure it's right here http://http://http://www.google.com", UrlDetectorOptions.Default,
        "http://www.google.com");
    runTest("http://ftp://https://www.google.com", UrlDetectorOptions.Default, "https://www.google.com");
    runTest("make sure its right here http://ftp://https://www.google.com", UrlDetectorOptions.Default,
        "https://www.google.com");
  }

  @Test
  void testDottedHexIpAddress() {
    runTest("http://0xc0.0x00.0xb2.0xEB", UrlDetectorOptions.Default, "http://0xc0.0x00.0xb2.0xEB");
    runTest("http://0xc0.0x0.0xb2.0xEB", UrlDetectorOptions.Default, "http://0xc0.0x0.0xb2.0xEB");
    runTest("http://0x000c0.0x00000.0xb2.0xEB", UrlDetectorOptions.Default, "http://0x000c0.0x00000.0xb2.0xEB");
    runTest("http://0xc0.0x00.0xb2.0xEB/bobo", UrlDetectorOptions.Default, "http://0xc0.0x00.0xb2.0xEB/bobo");
    runTest("ooh look i can find it in text http://0xc0.0x00.0xb2.0xEB/bobo like this", UrlDetectorOptions.Default,
        "http://0xc0.0x00.0xb2.0xEB/bobo");
    runTest("noscheme look 0xc0.0x00.0xb2.0xEB/bobo", UrlDetectorOptions.Default, "0xc0.0x00.0xb2.0xEB/bobo");
    runTest("no scheme 0xc0.0x00.0xb2.0xEB or path", UrlDetectorOptions.Default, "0xc0.0x00.0xb2.0xEB");
  }

  @Test
  void testDottedOctalIpAddress() {
    runTest("http://0301.0250.0002.0353", UrlDetectorOptions.Default, "http://0301.0250.0002.0353");
    runTest("http://0301.0250.0002.0353/bobo", UrlDetectorOptions.Default, "http://0301.0250.0002.0353/bobo");
    runTest("http://192.168.017.015/", UrlDetectorOptions.Default, "http://192.168.017.015/");
    runTest("ooh look i can find it in text http://0301.0250.0002.0353/bobo like this", UrlDetectorOptions.Default,
        "http://0301.0250.0002.0353/bobo");
    runTest("noscheme look 0301.0250.0002.0353/bobo", UrlDetectorOptions.Default, "0301.0250.0002.0353/bobo");
    runTest("no scheme 0301.0250.0002.0353 or path", UrlDetectorOptions.Default, "0301.0250.0002.0353");
  }

  @Test
  void testHexIpAddress() {
    runTest("http://0xC00002EB/hello", UrlDetectorOptions.Default, "http://0xC00002EB/hello");
    runTest("http://0xC00002EB.com/hello", UrlDetectorOptions.Default, "http://0xC00002EB.com/hello");
    runTest("still look it up as a normal url http://0xC00002EXsB.com/hello", UrlDetectorOptions.Default,
        "http://0xC00002EXsB.com/hello");
    runTest("ooh look i can find it in text http://0xC00002EB/bobo like this", UrlDetectorOptions.Default,
        "http://0xC00002EB/bobo");
    runTest("browsers dont support this without a scheme look 0xC00002EB/bobo", UrlDetectorOptions.Default);
  }

  @Test
  void testOctalIpAddress() {
    runTest("http://030000001353/bobobo", UrlDetectorOptions.Default, "http://030000001353/bobobo");
    runTest("ooh look i can find it in text http://030000001353/bobo like this", UrlDetectorOptions.Default,
        "http://030000001353/bobo");
    runTest("browsers dont support this without a scheme look 030000001353/bobo", UrlDetectorOptions.Default);
  }

  @Test
  void testUrlWithEmptyPort() {
    runTest("http://wtfismyip.com://foo.html", UrlDetectorOptions.Default, "http://wtfismyip.com://foo.html");
    runTest("make sure its right here http://wtfismyip.com://foo.html", UrlDetectorOptions.Default,
        "http://wtfismyip.com://foo.html");
  }

  @Test
  void testUrlEncodedDot() {
    runTest("hello www%2ewtfismyip%2ecom", UrlDetectorOptions.Default, "www%2ewtfismyip%2ecom");
    runTest("hello wtfismyip%2ecom", UrlDetectorOptions.Default, "wtfismyip%2ecom");
    runTest("http://wtfismyip%2ecom", UrlDetectorOptions.Default, "http://wtfismyip%2ecom");
    runTest("make sure its right here http://wtfismyip%2ecom", UrlDetectorOptions.Default, "http://wtfismyip%2ecom");
  }

  @Test
  void testUrlEncodedBadPath() {
    runTest("%2ewtfismyip", UrlDetectorOptions.Default);
    runTest("wtfismyip%2e", UrlDetectorOptions.Default);
    runTest("wtfismyip%2ecom%2e", UrlDetectorOptions.Default, "wtfismyip%2ecom%2e");
    runTest("wtfismyip%2ecom.", UrlDetectorOptions.Default, "wtfismyip%2ecom.");
    runTest("%2ewtfismyip%2ecom", UrlDetectorOptions.Default, "wtfismyip%2ecom");
  }

  @Test
  void testUrlEncodedColon() {
    runTest("http%3A//google.com", UrlDetectorOptions.Default, "http%3A//google.com");
    runTest("hello http%3A//google.com", UrlDetectorOptions.Default, "http%3A//google.com");
  }

  @Test
  void testIncompleteBracketSet() {
    runTest("[google.com", UrlDetectorOptions.BRACKET_MATCH, "google.com");
    runTest("lalla [google.com", UrlDetectorOptions.Default, "google.com");
  }

  @Test
  void testDetectUrlEncoded() {
    runTest("%77%77%77%2e%67%75%6d%62%6c%61%72%2e%63%6e", UrlDetectorOptions.Default,
        "%77%77%77%2e%67%75%6d%62%6c%61%72%2e%63%6e");
    runTest(" asdf  %77%77%77%2e%67%75%6d%62%6c%61%72%2e%63%6e", UrlDetectorOptions.Default,
        "%77%77%77%2e%67%75%6d%62%6c%61%72%2e%63%6e");
    runTest("%77%77%77%2e%67%75%6d%62%6c%61%72%2e%63%6e%2e", UrlDetectorOptions.Default,
        "%77%77%77%2e%67%75%6d%62%6c%61%72%2e%63%6e%2e");
  }

  @Test
  void testSingleLevelDomain() {
    runTest("localhost:9000/lalala hehe", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, "localhost:9000/lalala");
    runTest("http://localhost lasdf", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, "http://localhost");
    runTest("localhost:9000/lalala", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, "localhost:9000/lalala");
    runTest("192.168.1.1/lalala", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, "192.168.1.1/lalala");
    runTest("http://localhost", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, "http://localhost");
    runTest("//localhost", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, "//localhost");
    runTest("asf//localhost", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, "asf//localhost");
    runTest("hello/", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, "hello/");
    runTest("go/", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, "go/");
    runTest("hello:password@go//", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, "hello:password@go//");
    runTest("hello:password@go", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, "hello:password@go");
    runTest("hello:password@go lala", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, "hello:password@go");
    runTest("hello.com..", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN, "hello.com.");
    runTest("a/", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN);
    runTest("asdflocalhost aksdjfhads", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN);
    runTest("/", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN);
    runTest("////", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN);
    runTest("hi:", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN);
    runTest("http://localhost", UrlDetectorOptions.Default);
    runTest("localhost:9000/lalala", UrlDetectorOptions.Default);
  }

  @Test
  void testIncompleteIpAddresses() {
    runTest("hello 10...", UrlDetectorOptions.Default);
    runTest("hello 10...1", UrlDetectorOptions.Default);
    runTest("hello 10..1.", UrlDetectorOptions.Default);
    runTest("hello 10..1.1", UrlDetectorOptions.Default);
    runTest("hello 10.1..1", UrlDetectorOptions.Default);
    runTest("hello 10.1.1.", UrlDetectorOptions.Default);
    runTest("hello .192..", UrlDetectorOptions.Default);
    runTest("hello .192..1", UrlDetectorOptions.Default);
    runTest("hello .192.1.", UrlDetectorOptions.Default);
    runTest("hello .192.1.1", UrlDetectorOptions.Default);
    runTest("hello ..3.", UrlDetectorOptions.Default);
    runTest("hello ..3.1", UrlDetectorOptions.Default);
    runTest("hello ...1", UrlDetectorOptions.Default);
  }

  @Test
  void testIPv4EncodedDot() {
    runTest("hello 192%2e168%2e1%2e1", UrlDetectorOptions.Default, "192%2e168%2e1%2e1");
    runTest("hello 192.168%2e1%2e1/lalala", UrlDetectorOptions.Default, "192.168%2e1%2e1/lalala");
  }

  @Test
  void testIPv4HexEncodedDot() {
    runTest("hello 0xee%2e0xbb%2e0x1%2e0x1", UrlDetectorOptions.Default, "0xee%2e0xbb%2e0x1%2e0x1");
    runTest("hello 0xee%2e0xbb.0x1%2e0x1/lalala", UrlDetectorOptions.Default, "0xee%2e0xbb.0x1%2e0x1/lalala");
  }

  //IPv6 Tests

  @ParameterizedTest
  @ValueSource(strings = {
    "[fe80:aaaa:aaaa:aaaa:3dd0:7f8e:57b7:34d5]",
    "[bcad::aaaa:aaaa:3dd0:7f8e:222.168.1.1]",
    "[bcad::aaaa:aaaa:3dd0:7f8e:57b7:34d5]",
    "[dead::85a3:0:0:8a2e:370:7334]",
    "[::BEEF:0:8a2e:370:7334]",
    "[::beEE:EeEF:0:8a2e:370:7334]",
    "[::]",
    "[0::]",
    "[::1]",
    "[0::1]"
  })
  void testIPv6Colons(String testString) {
    runTest(testString, UrlDetectorOptions.Default, testString);
    runTest(" " + testString + " ", UrlDetectorOptions.Default, testString);
    runTest("bobo" + testString + " ", UrlDetectorOptions.Default, testString);
    runTest("bobo" + testString + "bobo", UrlDetectorOptions.Default, testString);
    runTest("bobo " + testString, UrlDetectorOptions.Default, testString);
    runTest("alkfs:afef:" + testString, UrlDetectorOptions.Default, testString);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "[fe80:aaaa:aaaa:aaaa:3dd0:7f8e:57b7:34d5f]",
    "[bcad::kkkk:aaaa:3dd0:7f8e:57b7:34d5]",
    "[:BAD:BEEF:0:8a2e:370:7334",
    "[:::]",
    "[lalala:we]",
    "[:0]",
    "[:0:]",
    "::]",
    "[:",
    "fe80:22:]3123:[adf]",
    "[][123[][ae][fae][de][:a][d]aef:E][f",
    "[]]]:d]",
    "[fe80:aaaa:aaaa:aaaa:3dd0:7f8e:57b7:34d5:addd:addd:adee]",
    "[][][]2[d][]][]]]:d][[[:d[e][aee:]af:",
    "[adf]",
    "[adf:]",
    "[adf:0]",
    "[:adf]",
    "[]"
  })
  void testIpv6BadUrls(String text) {
    runTest(text, UrlDetectorOptions.Default);
  }

  @Test
  void testIpv6BadWithGoodUrls() {
    runTest("[:::] [::] [bacd::]", UrlDetectorOptions.Default, "[::]", "[bacd::]");
    runTest("[:0][::]", UrlDetectorOptions.Default, "[::]");
    runTest("[:0:][::afaf]", UrlDetectorOptions.Default, "[::afaf]");
    runTest("::] [fe80:aaaa:aaaa:aaaa::]", UrlDetectorOptions.Default, "[fe80:aaaa:aaaa:aaaa::]");
    runTest("fe80:22:]3123:[adf] [fe80:aaaa:aaaa:aaaa::]", UrlDetectorOptions.Default, "[fe80:aaaa:aaaa:aaaa::]");
    runTest("[][123[][ae][fae][de][:a][d]aef:E][f", UrlDetectorOptions.Default);
    runTest("[][][]2[d][]][]]]:d][[[:d[e][aee:]af:", UrlDetectorOptions.Default);
  }

  @Test
  void testIpv6BadWithGoodUrlsEmbedded() {
    runTest("[fe80:aaaa:aaaa:aaaa:[::]3dd0:7f8e:57b7:34d5f]", UrlDetectorOptions.Default, "[::]");
    runTest("[b[::7f8e]:55]akjef[::]", UrlDetectorOptions.Default, "[::7f8e]:55", "[::]");
    runTest("[bcad::kkkk:aaaa:3dd0[::7f8e]:57b7:34d5]akjef[::]", UrlDetectorOptions.Default, "[::7f8e]:57", "[::]");
  }

  @Test
  void testIpv6BadWithGoodUrlsWeirder() {
    runTest("[:[::]", UrlDetectorOptions.Default, "[::]");
    runTest("[:] [feed::]", UrlDetectorOptions.Default, "[feed::]");
    runTest(":[::feee]:]", UrlDetectorOptions.Default, "[::feee]");
    runTest(":[::feee]:]]", UrlDetectorOptions.Default, "[::feee]");
    runTest("[[:[::feee]:]", UrlDetectorOptions.Default, "[::feee]");
  }

  @Test
  void testIpv6ConsecutiveGoodUrls() {
    runTest("[::afaf][eaea::][::]", UrlDetectorOptions.Default, "[::afaf]", "[eaea::]", "[::]");
    runTest("[::afaf]www.google.com", UrlDetectorOptions.Default, "[::afaf]", "www.google.com");
    runTest("[lalala:we][::]", UrlDetectorOptions.Default, "[::]");
    runTest("[::fe][::]", UrlDetectorOptions.Default, "[::fe]", "[::]");
    runTest("[aaaa::][:0:][::afaf]", UrlDetectorOptions.Default, "[::afaf]", "[aaaa::]");
  }

  @Test
  void testIpv6BacktrackingUsernamePassword() {
    runTest("check out my url:google.com", UrlDetectorOptions.Default, "google.com");
    runTest("check out my url:[::BAD:DEAD:BEEF:2e80:0:0]", UrlDetectorOptions.Default, "[::BAD:DEAD:BEEF:2e80:0:0]");
    runTest("check out my url:[::BAD:DEAD:BEEF:2e80:0:0] ", UrlDetectorOptions.Default, "[::BAD:DEAD:BEEF:2e80:0:0]");
  }

  @Test
  void testIpv6BacktrackingEmptyDomainName() {
    runTest("check out my http:///[::2e80:0:0]", UrlDetectorOptions.Default, "[::2e80:0:0]");
    runTest("check out my http://./[::2e80:0:0]", UrlDetectorOptions.Default, "[::2e80:0:0]");
  }

  @Test
  void testIpv6DoubleSchemeWithDomain() {
    runTest("http://http://[::2e80:0:0]", UrlDetectorOptions.Default, "http://[::2e80:0:0]");
    runTest("make sure its right here http://http://[::2e80:0:0]", UrlDetectorOptions.Default, "http://[::2e80:0:0]");
  }

  @Test
  void testIpv6MultipleSchemes() {
    runTest("http://http://http://[::2e80:0:0]", UrlDetectorOptions.Default, "http://[::2e80:0:0]");
    runTest("make sure its right here http://http://[::2e80:0:0]", UrlDetectorOptions.Default, "http://[::2e80:0:0]");
    runTest("http://ftp://https://[::2e80:0:0]", UrlDetectorOptions.Default, "https://[::2e80:0:0]");
    runTest("make sure its right here http://ftp://https://[::2e80:0:0]", UrlDetectorOptions.Default,
        "https://[::2e80:0:0]");
  }

  @Test
  void testIpv6FtpWithUsernameAndPassword() {
    runTest("ftp with username is ftp://username:password@[::2e80:0:0]", UrlDetectorOptions.Default,
        "ftp://username:password@[::2e80:0:0]");
  }

  @Test
  void testIpv6NewLinesAndTabsAreDelimiters() {
    runTest(
        "Do newlines and tabs break? [::2e80:0:0]/hello/\nworld [::BEEF:ADD:BEEF]\t/stuff/ [AAbb:AAbb:AAbb::]/\thello [::2e80:0:0\u0000]/hello world",
        UrlDetectorOptions.Default,

        "[::2e80:0:0]/hello/", "[::BEEF:ADD:BEEF]", "[AAbb:AAbb:AAbb::]/");
  }

  @Test
  void testIpv6WithPort() {
    runTest("http://[AAbb:AAbb:AAbb::]:8080/helloworld", UrlDetectorOptions.Default,
        "http://[AAbb:AAbb:AAbb::]:8080/helloworld");
  }

  @Test
  void testIpv6BasicHtml() {
    runTest(
        "<script type=\"text/javascript\">var a = '[AAbb:AAbb:AAbb::]', b=\"[::bbbb:]\"</script><a href=\"[::cccc:]\">[::ffff:]</a>",
        UrlDetectorOptions.HTML, "[AAbb:AAbb:AAbb::]", "[::bbbb:]", "[::cccc:]", "[::ffff:]");
  }

  @Test
  void testIpv6LongUrlWithInheritedScheme() {
    runTest(
        "<link rel=\"stylesheet\" href=\"//[AAbb:AAbb:AAbb::]/en.wikipedia.org/load.php?debug=false&amp;lang=en&amp;modules=ext.gadget.DRN-wizard%2CReferenceTooltips%2Ccharinsert%2Cteahouse%7Cext.wikihiero%7Cmediawiki.legacy.commonPrint%2Cshared%7Cmw.PopUpMediaTransform%7Cskins.vector&amp;only=styles&amp;skin=vector&amp;*\" />",
        UrlDetectorOptions.HTML,
        "//[AAbb:AAbb:AAbb::]/en.wikipedia.org/load.php?debug=false&amp;lang=en&amp;modules=ext.gadget.DRN-wizard%2CReferenceTooltips%2Ccharinsert%2Cteahouse%7Cext.wikihiero%7Cmediawiki.legacy.commonPrint%2Cshared%7Cmw.PopUpMediaTransform%7Cskins.vector&amp;only=styles&amp;skin=vector&amp;*");
  }

  @Test
  void testIpv6QuoteMatching() {
    runTest(
        "my website is \"[AAbb:AAbb:AAbb::]\" but my email is \"vshlos@[AAbb:AAbb:AAbb::]\" \" [::AAbb:]\" \" [::] \"www.abc.com\"",
        UrlDetectorOptions.QUOTE_MATCH, "[AAbb:AAbb:AAbb::]", "vshlos@[AAbb:AAbb:AAbb::]", "[::AAbb:]", "[::]",
        "www.abc.com");
  }

  @Test
  void testIpv6IncorrectParsingHtmlWithBadOptions() {
    runTest("<a href=\"http://[::AAbb:]/\">google.com</a>", UrlDetectorOptions.Default,
        "http://[::AAbb:]/\">google.com</a>");
  }

  @Test
  void testIpv6BracketMatching() {
    runTest(
        "MY url ([::AAbb:] ) is very cool. the domain [[::ffff:]] is popular and when written like this {[::BBBe:]} it looks like code",
        UrlDetectorOptions.BRACKET_MATCH, "[::AAbb:]", "[::ffff:]", "[::BBBe:]");
  }

  @Test
  void testIpv6EmptyPort() {
    runTest("http://[::AAbb:]://foo.html", UrlDetectorOptions.Default, "http://[::AAbb:]://foo.html");
    runTest("make sure its right here http://[::AAbb:]://foo.html", UrlDetectorOptions.Default,
        "http://[::AAbb:]://foo.html");
  }

  @Test
  void testIpv6UrlEncodedColon() {
    runTest("http%3A//[::AAbb:]", UrlDetectorOptions.Default, "http%3A//[::AAbb:]");
    runTest("hello http%3A//[::AAbb:]", UrlDetectorOptions.Default, "http%3A//[::AAbb:]");
  }

  static Stream<Arguments> getIPv6Ipv4AddressTestStrings() {
    return Stream.of(
        arguments("[fe80:aaaa:aaaa:aaaa:3dd0:7f8e:192.168.1.1]", "[fe80:aaaa:aaaa:aaaa:3dd0:7f8e:192.168.1.1]"),
        arguments("[bcad::aaaa:aaaa:3dd0:7f8e:222.168.1.1]", "[bcad::aaaa:aaaa:3dd0:7f8e:222.168.1.1]"),
        arguments("[dead::85a3:0:0:8a2e:192.168.1.1]", "[dead::85a3:0:0:8a2e:192.168.1.1]"),
        arguments("[::BEEF:0:8a2e:192.168.1.1]", "[::BEEF:0:8a2e:192.168.1.1]"),
        arguments("[:BAD:BEEF:0:8a2e:192.168.1.1]", "192.168.1.1"),
        arguments("[::beEE:EeEF:0:8a2e:192.168.1.1]", "[::beEE:EeEF:0:8a2e:192.168.1.1]"),
        arguments("[::192.168.1.1]", "[::192.168.1.1]"),
        arguments("[0::192.168.1.1]", "[0::192.168.1.1]"),
        arguments("[::ffff:192.168.1.1]", "[::ffff:192.168.1.1]"),
        arguments("[0::ffff:192.168.1.1]", "[0::ffff:192.168.1.1]"),
        arguments("[0:ffff:192.168.1.1::]", "192.168.1.1")
    );
  }

  @ParameterizedTest
  @MethodSource("getIPv6Ipv4AddressTestStrings")
  void testIPv6Ipv4Addresses(String testString, String expectedString) {
    runTest(testString, UrlDetectorOptions.Default, expectedString);
  }

  @ParameterizedTest
  @MethodSource("getIPv6Ipv4AddressTestStrings")
  void testIPv6Ipv4AddressesWithSpaces(String testString, String expectedString) {
    testString += " ";
    runTest(testString, UrlDetectorOptions.Default, expectedString);
    testString = " " + testString;
    runTest(testString, UrlDetectorOptions.Default, expectedString);
  }

  static Stream<Arguments> getHexOctalIpAddresses() {
    return Stream.of(
        arguments("http://[::ffff:0xC0.0x00.0x02.0xEB]", "%251"),
        arguments("http://[::0301.0250.0002.0353]", "%251"),
        arguments("http://[0::ffff:0xC0.0x00.0x02.0xEB]", "%223"),
        arguments("http://[0::0301.0250.0002.0353]", "%2lalal-a."),
        arguments("http://[::bad:ffff:0xC0.0x00.0x02.0xEB]", "%---"),
        arguments("http://[::bad:ffff:0301.0250.0002.0353]", "%-.-.-.-....-....--")
    );
  }

  @ParameterizedTest
  @MethodSource("getHexOctalIpAddresses")
  void testIpv6HexOctalIpAddress(String validUrl, String zoneIndex) {
    //these are supported by chrome and safari but not firefox;
    //chrome supports without scheme and safari does not without scheme

    runTest(validUrl, UrlDetectorOptions.Default, validUrl);
  }

  @ParameterizedTest
  @MethodSource("getHexOctalIpAddresses")
  void testIpv6ZoneIndices(String address, String zoneIndex) {
    //these are not supported by common browsers, but earlier versions of firefox do and future versions may support this
    String validUrl = address.substring(0, address.length() - 1) + zoneIndex + ']';
    runTest(validUrl, UrlDetectorOptions.Default, validUrl);
  }

  @ParameterizedTest
  @MethodSource("getHexOctalIpAddresses")
  void testIpv6ZoneIndicesWithUrlEncodedDots(String address, String zoneIndex) {
    //these are not supported by common browsers, but earlier versions of firefox do and future versions may support this
    String tmp = address.replace(".", "%2e");
    String validUrl = tmp.substring(0, tmp.length() - 1) + zoneIndex + ']';
    runTest(validUrl, UrlDetectorOptions.Default, validUrl);
  }

  @Test
  void testBacktrackInvalidUsernamePassword() {
    runTest("http://hello:asdf.com", UrlDetectorOptions.Default, "asdf.com");
  }
  
  /*
   * https://github.com/linkedin/URL-Detector/issues/12
   */
  @Test
  void testIssue12() {
    runTest("http://user:pass@host.com host.com", UrlDetectorOptions.Default, "http://user:pass@host.com", "host.com");
  }

  /*
   * https://github.com/linkedin/URL-Detector/issues/13
   */
  @ParameterizedTest
  @CsvSource({
    "user@github.io/page, user@github.io/page",
    "name@gmail.com, name@gmail.com",
    "name.lastname@gmail.com, name.lastname@gmail.com",
    "gmail.com@gmail.com, gmail.com@gmail.com",
    "first.middle.reallyreallyreallyreallyreallyreallyreallyreallyreallyreallylonglastname@gmail.com, first.middle.reallyreallyreallyreallyreallyreallyreallyreallyreallyreallylonglastname@gmail.com"
  })
  void testIssue13(String text, String expected) {
    runTest(text, UrlDetectorOptions.Default, expected);
  }
  
  /*
   * https://github.com/linkedin/URL-Detector/issues/15
   */
  @Test
  void testIssue15() {
    runTest(".............:::::::::::;;;;;;;;;;;;;;;::...............................................:::::::::::::::::::::::::::::....................", UrlDetectorOptions.Default);
  }

  /*
   * https://github.com/linkedin/URL-Detector/issues/16
   */
  @Test
  void testIssue16() {
    runTest("://VIVE MARINE LE PEN//:@.", UrlDetectorOptions.Default);
  }

  /*
   * https://github.com/URL-Detector/URL-Detector/issues/5
   */
  @Test
  void testDalesKillerString3() {
    // kills loop in UrlDetector.readDefault()  
    runTest(" :u ", UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN);
  }

  static Stream<Arguments> getUrlsForSchemaDetectionInHtml() {
    String domain = "linkedin.com";
    return Stream.of("http://", "https://", "ftp://", "ftps://", "http%3a//", "https%3a//", "ftp%3a//", "ftps%3a//")
      .map(validScheme -> new Object[][]{
        {"<a href=" + validScheme + domain + ">link</a>", validScheme + domain},
        {"<a href=\"" + validScheme + domain + "\">link</a>", validScheme + domain},
      })
      .flatMap(Arrays::stream)
      .map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("getUrlsForSchemaDetectionInHtml")
  void testSchemaDetectionInHtml(String text, String expected) {
    runTest(text, UrlDetectorOptions.HTML, expected);
  }

  @Test
  void testColonWithoutSlashes() {
    UrlDetector parser = new UrlDetector("ftp:example.com", UrlDetectorOptions.ALLOW_COLON_WITHOUT_SLASHES);
    List<Url> found = parser.detect();
    for (Url url : found) {
      assertEquals(url.getScheme(), "ftp"); // Should be detected as if it was ftp://
      assertEquals(url.getHost(), "example.com");
    }
  }

  @Test
  void testColonWithoutSlashesFail() {
    UrlDetector parser = new UrlDetector("ftp:example.com", UrlDetectorOptions.Default);
    List<Url> found = parser.detect();
    for (Url url : found) {
      assertEquals(url.getScheme(), "http"); // Should be detected as a username now and set to default http://
      assertEquals(url.getHost(), "example.com");
    }
  }

  private void runTest(String text, UrlDetectorOptions options, String... expected) {
    //do the detection
    UrlDetector parser = new UrlDetector(text, options);
    List<Url> found = parser.detect();
    String[] foundArray = new String[found.size()];
    for (int i = 0; i < foundArray.length; i++) {
      foundArray[i] = found.get(i).getOriginalUrl();
    }

    assertThat(foundArray, Matchers.arrayContainingInAnyOrder(expected));
  }
  
}
