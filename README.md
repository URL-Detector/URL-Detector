__NOTE__: This is a fork of the original https://github.com/linkedin/URL-Detector, which seems to be abandoned.  This fork was created to allow continued maintenance by the OS community.

# URL Detector

The url detector is a library created by the Linkedin Security Team to detect and extract urls in a long piece of text.

It is able to find and detect any urls such as:

* __HTML 5 Scheme__   - //www.linkedin.com
* __Usernames__       - user:pass@linkedin.com
* __Email__           - fred@linkedin.com
* __IPv4 Address__    - 192.168.1.1/hello.html
* __IPv4 Octets__     - 0x00.0x00.0x00.0x00
* __IPv4 Decimal__    - http://123123123123/
* __IPv6 Address__    - ftp://[::]/hello
* __IPv4-mapped IPv6 Address__  - http://[fe30:4:3:0:192.3.2.1]/

_Note: Keep in mind that for security purposes, its better to overdetect urls and check more against blacklists than to not detect a url that was submitted. As such, some things that we detect might not be urls but somewhat look like urls. Also, instead of complying with RFC 3986 (http://www.ietf.org/rfc/rfc3986.txt), we try to detect based on browser behavior, optimizing detection for urls that are visitable through the address bar of Chrome, Firefox, Internet Explorer, and Safari._

It is also able to identify the parts of the identified urls. For example, for the url: `http://user@linkedin.com:39000/hello?boo=ff#frag`

* Scheme   - "http"
* Username - "user"
* Password - null
* Host     - "linkedin.com"
* Port     - 39000
* Path     - "/hello"
* Query    - "?boo=ff"
* Fragment - "#frag"

---
## How to Use:

Using the URL detector library is simple. Simply import the UrlDetector object and give it some options. In response, you will get a list of urls which were detected.

For example, the following code will find the url `linkedin.com`

```java

    UrlDetector parser = new UrlDetector("hello this is a url Linkedin.com", UrlDetectorOptions.Default);
    List<Url> found = parser.detect();

    for(Url url : found) {
        System.out.println("Scheme: " + url.getScheme());
        System.out.println("Host: " + url.getHost());
        System.out.println("Path: " + url.getPath());
    }
```

### URL normalization and domain name conversion into its ASCII form for DNS lookup (including emojis)
There are three main standards for converting [internationalized domain names (IDNA)](
https://en.wikipedia.org/wiki/Internationalized_domain_name) into their ASCII forms:
IDNA 2003, IDNA2008, and [UTS #46](http://www.unicode.org/reports/tr46/).

As of May 2020, major web browsers employ UTS #46. This is a sort of more lenient,
tradeoff standard between IDNA2003 and IDNA2008, that tries to accommodate both IDNA versions.
However, this is not completely possible 
(see [comparison table](http://www.unicode.org/reports/tr46/#Table_IDNA_Comparisons)).
For this reason, the UTS #46 algorithm has several 
[processing flags](http://www.unicode.org/reports/tr46/#Processing). 

Example:
```java
    Url url = Url.create("https://i❤.ws/");
    System.out.println("Normalized URL: " + url.normalize());
```

Optionally, UTS #46 Ascii [normalization options](
(https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/IDNA.html#getUTS46Instance-int-))
can be set. By default, we use `IDNA.NONTRANSITIONAL_TO_ASCII`.

```java
    import com.ibm.icu.text.IDNA;
    HostNormalizer.setUts46Options(IDNA.NONTRANSITIONAL_TO_ASCII);
```


### Quote Matching and HTML
Depending on your input string, you may want to handle certain characters in a special way. For example if you are
parsing HTML, you probably want to break out of things like quotes and brackets. For example, if your input looks like

> &lt;a href="http://linkedin.com/abc"&gt;linkedin.com&lt;/a&gt;

You probably want to make sure that the quotes and brackets are extracted. For that reason, using UrlDetectorOptions
will allow you to change the sensitivity level of detection based on your expected input type. This way you can detect
`linkedin.com` instead of `linkedin.com</a>`.

In code this looks like:

```java

    UrlDetector parser = new UrlDetector("<a href="linkedin.com/abc">linkedin.com</a>", UrlDetectorOptions.HTML);
    List<Url> found = parser.detect();

```

### Maven Usage:

To use the latest release, add the following dependency to your pom.xml:

```xml
    <dependency>
        <groupId>io.github.url-detector</groupId>
        <artifactId>url-detector</artifactId>
        <version>0.1.23-SNAPSHOT</version>
    </dependency>
```

The icu4j library is optional, given its size. Add it in you pom.xml as follows.

```xml
    <dependency>
        <groupId>com.ibm.icu</groupId>
        <artifactId>icu4j</artifactId>
        <version>67.1</version>
    </dependency>
```

If icu4j is not loaded, we fall back to `java.net.IDN.toASCII(host, IDN.ALLOW_UNASSIGNED)`,
which only supports IDNA2003.

---
## About:

This library was written by the security team and Linkedin when other options did not exist. Some of the primary authors are:

* Vlad Shlosberg (vshlosbe@linkedin.com)
* Tzu-Han Jan (tjan@linkedin.com)
* Yulia Astakhova (jastakho@linkedin.com)

---
## Third Party Dependencies

### TestNG
* http://testng.org/
* Copyright © 2004-2014 Cédric Beust
* License: Apache 2.0

### Apache CommonsLang3: org.apache.commons:commons-lang3:3.1
* http://commons.apache.org/proper/commons-lang/
* Copyright © 2001-2014 The Apache Software Foundation
* License: Apache 2.0

### Optional, Unicode Icu4j: com.ibm.icu:icu4j:67.1
* http://site.icu-project.org/home
* Copyright © 1991-2020 Unicode, Inc. All rights reserved.
* License: http://www.unicode.org/copyright.html#License

---
## License

Copyright 2015 LinkedIn Corp. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the license at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

