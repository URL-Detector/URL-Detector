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

import com.linkedin.urls.detection.UrlDetector;
import com.linkedin.urls.detection.UrlDetectorOptions;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Creating own Uri class since java.net.Uri would throw parsing exceptions
 * for URL's considered ok by browsers.
 *
 * Also to avoid further conflict, this does stuff that the normal Uri object doesn't do:
 * - Converts http://google.com/a/b/.//./../c to http://google.com/a/c
 * - Decodes repeatedly so that http://host/%2525252525252525 becomes http://host/%25 while normal decoders
 *     would make it http://host/%25252525252525 (one less 25)
 * - Removes tabs and new lines: http://www.google.com/foo\tbar\rbaz\n2 becomes "http://www.google.com/foobarbaz2"
 * - Converts IP addresses: http://3279880203/blah becomes http://195.127.0.11/blah
 * - Strips fragments (anything after #)
 *
 */
public class Url {

  private static final String DEFAULT_SCHEME = "http";
  private static final Map<String, Integer> SCHEME_PORT_MAP;
  static {
    SCHEME_PORT_MAP = new HashMap<String, Integer>();
    SCHEME_PORT_MAP.put("http", 80);
    SCHEME_PORT_MAP.put("https", 443);
    SCHEME_PORT_MAP.put("ftp", 21);
    SCHEME_PORT_MAP.put("fax", 1620);
    SCHEME_PORT_MAP.put("filesystem", 0);
    SCHEME_PORT_MAP.put("mailserver", 0);
    SCHEME_PORT_MAP.put("modem", 0);
    SCHEME_PORT_MAP.put("pack", 0);
    SCHEME_PORT_MAP.put("prospero", 1525);
    SCHEME_PORT_MAP.put("snews", 0);
    SCHEME_PORT_MAP.put("videotex", 0);
    SCHEME_PORT_MAP.put("wais", 210);
    SCHEME_PORT_MAP.put("wpid", 0);
    SCHEME_PORT_MAP.put("z39.50", 0);
    SCHEME_PORT_MAP.put("aaa", 0);
    SCHEME_PORT_MAP.put("aaas", 0);
    SCHEME_PORT_MAP.put("about", 0);
    SCHEME_PORT_MAP.put("acap", 674);
    SCHEME_PORT_MAP.put("acct", 0);
    SCHEME_PORT_MAP.put("cap", 0);
    SCHEME_PORT_MAP.put("cid", 0);
    SCHEME_PORT_MAP.put("coap", 0);
    SCHEME_PORT_MAP.put("coap+tcp", 0);
    SCHEME_PORT_MAP.put("coap+ws", 0);
    SCHEME_PORT_MAP.put("coaps", 0);
    SCHEME_PORT_MAP.put("coaps+tcp", 0);
    SCHEME_PORT_MAP.put("coaps+ws", 0);
    SCHEME_PORT_MAP.put("crid", 0);
    SCHEME_PORT_MAP.put("data", 0);
    SCHEME_PORT_MAP.put("dav", 0);
    SCHEME_PORT_MAP.put("dict", 2628);
    SCHEME_PORT_MAP.put("dns", 53);
    SCHEME_PORT_MAP.put("dtn", 0);
    SCHEME_PORT_MAP.put("example", 0);
    SCHEME_PORT_MAP.put("file", 0);
    SCHEME_PORT_MAP.put("geo", 0);
    SCHEME_PORT_MAP.put("go", 0);
    SCHEME_PORT_MAP.put("gopher", 70);
    SCHEME_PORT_MAP.put("h323", 0);
    SCHEME_PORT_MAP.put("iax", 0);
    SCHEME_PORT_MAP.put("icap", 0);
    SCHEME_PORT_MAP.put("im", 0);
    SCHEME_PORT_MAP.put("imap", 143);
    SCHEME_PORT_MAP.put("info", 0);
    SCHEME_PORT_MAP.put("ipn", 0);
    SCHEME_PORT_MAP.put("ipp", 631);
    SCHEME_PORT_MAP.put("ipps", 631);
    SCHEME_PORT_MAP.put("iris", 0);
    SCHEME_PORT_MAP.put("iris.beep", 0);
    SCHEME_PORT_MAP.put("iris.lwz", 0);
    SCHEME_PORT_MAP.put("iris.xpc", 0);
    SCHEME_PORT_MAP.put("iris.xpcs", 0);
    SCHEME_PORT_MAP.put("jabber", 5222);
    SCHEME_PORT_MAP.put("ldap", 389);
    SCHEME_PORT_MAP.put("leaptofrogans", 0);
    SCHEME_PORT_MAP.put("mailto", 0);
    SCHEME_PORT_MAP.put("mid", 0);
    SCHEME_PORT_MAP.put("msrp", 2855);
    SCHEME_PORT_MAP.put("msrps", 0);
    SCHEME_PORT_MAP.put("mtqp", 1038);
    SCHEME_PORT_MAP.put("mupdate", 0);
    SCHEME_PORT_MAP.put("news", 0);
    SCHEME_PORT_MAP.put("nfs", 111);
    SCHEME_PORT_MAP.put("ni", 0);
    SCHEME_PORT_MAP.put("nih", 0);
    SCHEME_PORT_MAP.put("nntp", 119);
    SCHEME_PORT_MAP.put("opaquelocktoken", 0);
    SCHEME_PORT_MAP.put("pkcs11", 0);
    SCHEME_PORT_MAP.put("pop", 110);
    SCHEME_PORT_MAP.put("pres", 0);
    SCHEME_PORT_MAP.put("reload", 0);
    SCHEME_PORT_MAP.put("rtsp", 554);
    SCHEME_PORT_MAP.put("rtsps", 322);
    SCHEME_PORT_MAP.put("rtspu", 5005);
    SCHEME_PORT_MAP.put("service", 0);
    SCHEME_PORT_MAP.put("session", 0);
    SCHEME_PORT_MAP.put("shttp", 80);
    SCHEME_PORT_MAP.put("sieve", 0);
    SCHEME_PORT_MAP.put("sip", 0);
    SCHEME_PORT_MAP.put("sips", 0);
    SCHEME_PORT_MAP.put("sms", 0);
    SCHEME_PORT_MAP.put("snmp", 161);
    SCHEME_PORT_MAP.put("soap.beep", 0);
    SCHEME_PORT_MAP.put("soap.beeps", 0);
    SCHEME_PORT_MAP.put("stun", 3478);
    SCHEME_PORT_MAP.put("stuns", 5349);
    SCHEME_PORT_MAP.put("tag", 0);
    SCHEME_PORT_MAP.put("tel", 0);
    SCHEME_PORT_MAP.put("telnet", 23);
    SCHEME_PORT_MAP.put("tftp", 69);
    SCHEME_PORT_MAP.put("thismessage", 0);
    SCHEME_PORT_MAP.put("tip", 0);
    SCHEME_PORT_MAP.put("tn3270", 23);
    SCHEME_PORT_MAP.put("turn", 3478);
    SCHEME_PORT_MAP.put("turns", 5349);
    SCHEME_PORT_MAP.put("tv", 0);
    SCHEME_PORT_MAP.put("urn", 0);
    SCHEME_PORT_MAP.put("vemmi", 575);
    SCHEME_PORT_MAP.put("vnc", 5900);
    SCHEME_PORT_MAP.put("ws", 80);
    SCHEME_PORT_MAP.put("wss", 443);
    SCHEME_PORT_MAP.put("xcon", 0);
    SCHEME_PORT_MAP.put("xcon-userid", 0);
    SCHEME_PORT_MAP.put("xmlrpc.beep", 602);
    SCHEME_PORT_MAP.put("xmlrpc.beeps", 602);
    SCHEME_PORT_MAP.put("xmpp", 5222);
    SCHEME_PORT_MAP.put("z39.50r", 210);
    SCHEME_PORT_MAP.put("z39.50s", 210);
    SCHEME_PORT_MAP.put("acd", 0);
    SCHEME_PORT_MAP.put("acr", 0);
    SCHEME_PORT_MAP.put("adiumxtra", 0);
    SCHEME_PORT_MAP.put("adt", 0);
    SCHEME_PORT_MAP.put("afp", 548);
    SCHEME_PORT_MAP.put("afs", 0);
    SCHEME_PORT_MAP.put("aim", 5190);
    SCHEME_PORT_MAP.put("amss", 0);
    SCHEME_PORT_MAP.put("android", 0);
    SCHEME_PORT_MAP.put("appdata", 0);
    SCHEME_PORT_MAP.put("apt", 80);
    SCHEME_PORT_MAP.put("ar", 1984);
    SCHEME_PORT_MAP.put("ark", 0);
    SCHEME_PORT_MAP.put("attachment", 0);
    SCHEME_PORT_MAP.put("aw", 7777);
    SCHEME_PORT_MAP.put("barion", 0);
    SCHEME_PORT_MAP.put("beshare", 0);
    SCHEME_PORT_MAP.put("bitcoin", 8332);
    SCHEME_PORT_MAP.put("bitcoincash", 8332);
    SCHEME_PORT_MAP.put("blob", 0);
    SCHEME_PORT_MAP.put("bolo", 0);
    SCHEME_PORT_MAP.put("browserext", 0);
    SCHEME_PORT_MAP.put("cabal", 13331);
    SCHEME_PORT_MAP.put("calculator", 0);
    SCHEME_PORT_MAP.put("callto", 0);
    SCHEME_PORT_MAP.put("cast", 0);
    SCHEME_PORT_MAP.put("casts", 0);
    SCHEME_PORT_MAP.put("chrome", 0);
    SCHEME_PORT_MAP.put("chrome-extension", 0);
    SCHEME_PORT_MAP.put("com-eventbrite-attendee", 0);
    SCHEME_PORT_MAP.put("content", 0);
    SCHEME_PORT_MAP.put("content-type", 0);
    SCHEME_PORT_MAP.put("cvs", 2401);
    SCHEME_PORT_MAP.put("dab", 0);
    SCHEME_PORT_MAP.put("dat", 0);
    SCHEME_PORT_MAP.put("diaspora", 0);
    SCHEME_PORT_MAP.put("did", 0);
    SCHEME_PORT_MAP.put("dis", 393);
    SCHEME_PORT_MAP.put("dlna-playcontainer", 0);
    SCHEME_PORT_MAP.put("dlna-playsingle", 0);
    SCHEME_PORT_MAP.put("dntp", 0);
    SCHEME_PORT_MAP.put("doi", 0);
    SCHEME_PORT_MAP.put("dpp", 8908);
    SCHEME_PORT_MAP.put("drm", 0);
    SCHEME_PORT_MAP.put("drop", 0);
    SCHEME_PORT_MAP.put("dtmi", 0);
    SCHEME_PORT_MAP.put("dvb", 3937);
    SCHEME_PORT_MAP.put("dvx", 0);
    SCHEME_PORT_MAP.put("dweb", 0);
    SCHEME_PORT_MAP.put("ed2k", 0);
    SCHEME_PORT_MAP.put("elsi", 0);
    SCHEME_PORT_MAP.put("embedded", 0);
    SCHEME_PORT_MAP.put("ens", 0);
    SCHEME_PORT_MAP.put("ethereum", 30303);
    SCHEME_PORT_MAP.put("facetime", 3478);
    SCHEME_PORT_MAP.put("feed", 0);
    SCHEME_PORT_MAP.put("feedready", 0);
    SCHEME_PORT_MAP.put("fido", 0);
    SCHEME_PORT_MAP.put("finger", 79);
    SCHEME_PORT_MAP.put("first-run-pen-experience", 0);
    SCHEME_PORT_MAP.put("fish", 0);
    SCHEME_PORT_MAP.put("fm", 0);
    SCHEME_PORT_MAP.put("fuchsia-pkg", 0);
    SCHEME_PORT_MAP.put("gg", 0);
    SCHEME_PORT_MAP.put("git", 9418);
    SCHEME_PORT_MAP.put("gizmoproject", 64064);
    SCHEME_PORT_MAP.put("graph", 0);
    SCHEME_PORT_MAP.put("gtalk", 0);
    SCHEME_PORT_MAP.put("ham", 0);
    SCHEME_PORT_MAP.put("hcap", 0);
    SCHEME_PORT_MAP.put("hcp", 0);
    SCHEME_PORT_MAP.put("hxxp", 80);
    SCHEME_PORT_MAP.put("hxxps", 443);
    SCHEME_PORT_MAP.put("hydrazone", 0);
    SCHEME_PORT_MAP.put("hyper", 0);
    SCHEME_PORT_MAP.put("icon", 0);
    SCHEME_PORT_MAP.put("iotdisco", 0);
    SCHEME_PORT_MAP.put("ipfs", 10001);
    SCHEME_PORT_MAP.put("ipns", 0);
    SCHEME_PORT_MAP.put("irc", 194);
    SCHEME_PORT_MAP.put("irc6", 194);
    SCHEME_PORT_MAP.put("ircs", 994);
    SCHEME_PORT_MAP.put("isostore", 0);
    SCHEME_PORT_MAP.put("itms", 0);
    SCHEME_PORT_MAP.put("jar", 0);
    SCHEME_PORT_MAP.put("jms", 5673);
    SCHEME_PORT_MAP.put("keyparc", 0);
    SCHEME_PORT_MAP.put("lastfm", 0);
    SCHEME_PORT_MAP.put("lbry", 0);
    SCHEME_PORT_MAP.put("ldaps", 636);
    SCHEME_PORT_MAP.put("lorawan", 0);
    SCHEME_PORT_MAP.put("lvlt", 0);
    SCHEME_PORT_MAP.put("magnet", 0);
    SCHEME_PORT_MAP.put("maps", 0);
    SCHEME_PORT_MAP.put("market", 0);
    SCHEME_PORT_MAP.put("matrix", 8448);
    SCHEME_PORT_MAP.put("message", 0);
    SCHEME_PORT_MAP.put("microsoft.windows.camera", 0);
    SCHEME_PORT_MAP.put("microsoft.windows.camera.multipicker", 0);
    SCHEME_PORT_MAP.put("microsoft.windows.camera.picker", 0);
    SCHEME_PORT_MAP.put("mms", 1755);
    SCHEME_PORT_MAP.put("mongodb", 27017);
    SCHEME_PORT_MAP.put("moz", 0);
    SCHEME_PORT_MAP.put("ms-access", 0);
    SCHEME_PORT_MAP.put("ms-appinstaller", 0);
    SCHEME_PORT_MAP.put("ms-browser-extension", 0);
    SCHEME_PORT_MAP.put("ms-calculator", 0);
    SCHEME_PORT_MAP.put("ms-drive-to", 0);
    SCHEME_PORT_MAP.put("ms-enrollment", 0);
    SCHEME_PORT_MAP.put("ms-excel", 0);
    SCHEME_PORT_MAP.put("ms-eyecontrolspeech", 0);
    SCHEME_PORT_MAP.put("ms-gamebarservices", 0);
    SCHEME_PORT_MAP.put("ms-gamingoverlay", 0);
    SCHEME_PORT_MAP.put("ms-getoffice", 0);
    SCHEME_PORT_MAP.put("ms-help", 0);
    SCHEME_PORT_MAP.put("ms-infopath", 0);
    SCHEME_PORT_MAP.put("ms-inputapp", 0);
    SCHEME_PORT_MAP.put("ms-lockscreencomponent-config", 0);
    SCHEME_PORT_MAP.put("ms-media-stream-id", 0);
    SCHEME_PORT_MAP.put("ms-meetnow", 0);
    SCHEME_PORT_MAP.put("ms-mixedrealitycapture", 0);
    SCHEME_PORT_MAP.put("ms-mobileplans", 0);
    SCHEME_PORT_MAP.put("ms-officeapp", 0);
    SCHEME_PORT_MAP.put("ms-people", 0);
    SCHEME_PORT_MAP.put("ms-project", 0);
    SCHEME_PORT_MAP.put("ms-powerpoint", 0);
    SCHEME_PORT_MAP.put("ms-publisher", 0);
    SCHEME_PORT_MAP.put("ms-restoretabcompanion", 0);
    SCHEME_PORT_MAP.put("ms-screenclip", 0);
    SCHEME_PORT_MAP.put("ms-screensketch", 0);
    SCHEME_PORT_MAP.put("ms-search", 0);
    SCHEME_PORT_MAP.put("ms-search-repair", 0);
    SCHEME_PORT_MAP.put("ms-secondary-screen-controller", 0);
    SCHEME_PORT_MAP.put("ms-secondary-screen-setup", 0);
    SCHEME_PORT_MAP.put("ms-settings", 0);
    SCHEME_PORT_MAP.put("ms-settings-airplanemode", 0);
    SCHEME_PORT_MAP.put("ms-settings-bluetooth", 0);
    SCHEME_PORT_MAP.put("ms-settings-camera", 0);
    SCHEME_PORT_MAP.put("ms-settings-cellular", 0);
    SCHEME_PORT_MAP.put("ms-settings-cloudstorage", 0);
    SCHEME_PORT_MAP.put("ms-settings-connectabledevices", 0);
    SCHEME_PORT_MAP.put("ms-settings-displays-topology", 0);
    SCHEME_PORT_MAP.put("ms-settings-emailandaccounts", 0);
    SCHEME_PORT_MAP.put("ms-settings-language", 0);
    SCHEME_PORT_MAP.put("ms-settings-location", 0);
    SCHEME_PORT_MAP.put("ms-settings-lock", 0);
    SCHEME_PORT_MAP.put("ms-settings-nfctransactions", 0);
    SCHEME_PORT_MAP.put("ms-settings-notifications", 0);
    SCHEME_PORT_MAP.put("ms-settings-power", 0);
    SCHEME_PORT_MAP.put("ms-settings-privacy", 0);
    SCHEME_PORT_MAP.put("ms-settings-proximity", 0);
    SCHEME_PORT_MAP.put("ms-settings-screenrotation", 0);
    SCHEME_PORT_MAP.put("ms-settings-wifi", 0);
    SCHEME_PORT_MAP.put("ms-settings-workplace", 0);
    SCHEME_PORT_MAP.put("ms-spd", 0);
    SCHEME_PORT_MAP.put("ms-stickers", 0);
    SCHEME_PORT_MAP.put("ms-sttoverlay", 0);
    SCHEME_PORT_MAP.put("ms-transit-to", 0);
    SCHEME_PORT_MAP.put("ms-useractivityset", 0);
    SCHEME_PORT_MAP.put("ms-virtualtouchpad", 0);
    SCHEME_PORT_MAP.put("ms-visio", 0);
    SCHEME_PORT_MAP.put("ms-walk-to", 0);
    SCHEME_PORT_MAP.put("ms-whiteboard", 0);
    SCHEME_PORT_MAP.put("ms-whiteboard-cmd", 0);
    SCHEME_PORT_MAP.put("ms-word", 0);
    SCHEME_PORT_MAP.put("msnim", 0);
    SCHEME_PORT_MAP.put("mss", 0);
    SCHEME_PORT_MAP.put("mt", 0);
    SCHEME_PORT_MAP.put("mumble", 64738);
    SCHEME_PORT_MAP.put("mvn", 0);
    SCHEME_PORT_MAP.put("notes", 0);
    SCHEME_PORT_MAP.put("num", 0);
    SCHEME_PORT_MAP.put("ocf", 0);
    SCHEME_PORT_MAP.put("oid", 0);
    SCHEME_PORT_MAP.put("onenote", 0);
    SCHEME_PORT_MAP.put("onenote-cmd", 0);
    SCHEME_PORT_MAP.put("openpgp4fpr", 11371);
    SCHEME_PORT_MAP.put("otpauth", 0);
    SCHEME_PORT_MAP.put("palm", 0);
    SCHEME_PORT_MAP.put("paparazzi", 0);
    SCHEME_PORT_MAP.put("payment", 0);
    SCHEME_PORT_MAP.put("payto", 0);
    SCHEME_PORT_MAP.put("platform", 0);
    SCHEME_PORT_MAP.put("proxy", 0);
    SCHEME_PORT_MAP.put("pwid", 0);
    SCHEME_PORT_MAP.put("psyc", 0);
    SCHEME_PORT_MAP.put("pttp", 0);
    SCHEME_PORT_MAP.put("qb", 0);
    SCHEME_PORT_MAP.put("query", 0);
    SCHEME_PORT_MAP.put("quic-transport", 0);
    SCHEME_PORT_MAP.put("redis", 6379);
    SCHEME_PORT_MAP.put("rediss", 6379);
    SCHEME_PORT_MAP.put("res", 0);
    SCHEME_PORT_MAP.put("resource", 0);
    SCHEME_PORT_MAP.put("rmi", 0);
    SCHEME_PORT_MAP.put("rsync", 873);
    SCHEME_PORT_MAP.put("rtmfp", 1935);
    SCHEME_PORT_MAP.put("rtmp", 1935);
    SCHEME_PORT_MAP.put("sarif", 0);
    SCHEME_PORT_MAP.put("secondlife", 0);
    SCHEME_PORT_MAP.put("secret-token", 0);
    SCHEME_PORT_MAP.put("sftp", 22);
    SCHEME_PORT_MAP.put("sgn", 0);
    SCHEME_PORT_MAP.put("shc", 0);
    SCHEME_PORT_MAP.put("simpleledger", 0);
    SCHEME_PORT_MAP.put("simplex", 0);
    SCHEME_PORT_MAP.put("skype", 5521);
    SCHEME_PORT_MAP.put("smb", 445);
    SCHEME_PORT_MAP.put("smp", 0);
    SCHEME_PORT_MAP.put("smtp", 25);
    SCHEME_PORT_MAP.put("soldat", 23073);
    SCHEME_PORT_MAP.put("spiffe", 0);
    SCHEME_PORT_MAP.put("spotify", 57621);
    SCHEME_PORT_MAP.put("ssb", 0);
    SCHEME_PORT_MAP.put("ssh", 22);
    SCHEME_PORT_MAP.put("steam", 4380);
    SCHEME_PORT_MAP.put("submit", 0);
    SCHEME_PORT_MAP.put("svn", 3690);
    SCHEME_PORT_MAP.put("swh", 0);
    SCHEME_PORT_MAP.put("swid", 0);
    SCHEME_PORT_MAP.put("swidpath", 0);
    SCHEME_PORT_MAP.put("teamspeak", 10011);
    SCHEME_PORT_MAP.put("teliaeid", 0);
    SCHEME_PORT_MAP.put("things", 0);
    SCHEME_PORT_MAP.put("tool", 0);
    SCHEME_PORT_MAP.put("udp", 0);
    SCHEME_PORT_MAP.put("unreal", 0);
    SCHEME_PORT_MAP.put("ut2004", 0);
    SCHEME_PORT_MAP.put("uuid-in-package", 0);
    SCHEME_PORT_MAP.put("v-event", 0);
    SCHEME_PORT_MAP.put("ventrilo", 3784);
    SCHEME_PORT_MAP.put("ves", 0);
    SCHEME_PORT_MAP.put("view-source", 0);
    SCHEME_PORT_MAP.put("vscode", 0);
    SCHEME_PORT_MAP.put("vscode-insiders", 0);
    SCHEME_PORT_MAP.put("vsls", 0);
    SCHEME_PORT_MAP.put("wcr", 0);
    SCHEME_PORT_MAP.put("webcal", 0);
    SCHEME_PORT_MAP.put("wifi", 0);
    SCHEME_PORT_MAP.put("wtai", 0);
    SCHEME_PORT_MAP.put("wyciwyg", 0);
    SCHEME_PORT_MAP.put("xfire", 0);
    SCHEME_PORT_MAP.put("xri", 0);
    SCHEME_PORT_MAP.put("ymsgr", 0);
  }
  private UrlMarker _urlMarker;
  private String _scheme;
  private String _username;
  private String _password;
  private String _host;
  private int _port = 0;
  private String _path;
  private String _query;
  private String _fragment;
  private String _originalUrl;

  protected Url(UrlMarker urlMarker) {
    _urlMarker = urlMarker;
    _originalUrl = urlMarker.getOriginalUrl();
  }

  /**
   * Returns a url given a single url.
   */
  public static Url create(String url) throws MalformedURLException {
    String formattedString = UrlUtil.removeSpecialSpaces(url.trim().replace(" ", "%20"));
    List<Url> urls = new UrlDetector(formattedString, UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN).detect();
    if (urls.size() == 1) {
      return urls.get(0);
    } else if (urls.size() == 0) {
      throw new MalformedURLException("We couldn't find any urls in string: " + url);
    } else {
      throw new MalformedURLException("We found more than one url in string: " + url);
    }
  }

  /**
   * Returns a normalized url given a url object
   */
  public NormalizedUrl normalize() {
    return new NormalizedUrl(_urlMarker);
  }

  @Override
  public String toString() {
    return this.getFullUrl();
  }

  /**
   * Note that this includes the fragment
   * @return Formats the url to: [scheme]://[username]:[password]@[host]:[port]/[path]?[query]#[fragment]
   */
  public String getFullUrl() {
    return getFullUrlWithoutFragment() + getFragment();
  }

  /**
   *
   * @return Formats the url to: [scheme]://[username]:[password]@[host]:[port]/[path]?[query]
   */
  public String getFullUrlWithoutFragment() {
    StringBuilder url = new StringBuilder();
    String scheme = getScheme();
    if (scheme != null && !scheme.isEmpty()) {
      url.append(scheme);
      url.append(":");
    }
    url.append("//");

    String username = getUsername();
    if (username != null && !username.isEmpty()) {
      url.append(username);
      String password = getPassword();
      if (password != null && !password.isEmpty()) {
        url.append(":");
        url.append(password);
      }
      url.append("@");
    }

    url.append(getHost());
    if (getPort() > 0 && getPort() != SCHEME_PORT_MAP.get(scheme)) {
      url.append(":");
      url.append(getPort());
    }

    url.append(getPath());
    url.append(getQuery());

    return url.toString();
  }

  public String getScheme() {
    if (_scheme == null) {
      if (exists(UrlPart.SCHEME)) {
        _scheme = getPart(UrlPart.SCHEME);
        int index = _scheme.indexOf(":");
        if (index != -1) {
          _scheme = _scheme.substring(0, index);
        }
      } else if (!_originalUrl.startsWith("//")) {
        _scheme = DEFAULT_SCHEME;
      }
    }
    return _scheme == null ? "" : _scheme;
  }

  public String getUsername() {
    if (_username == null) {
      populateUsernamePassword();
    }
    return _username == null ? "" : _username;
  }

  public String getPassword() {
    if (_password == null) {
      populateUsernamePassword();
    }
    return _password == null ? "" : _password;
  }

  public String getHost() {
    if (_host == null) {
      _host = getPart(UrlPart.HOST);
      if (exists(UrlPart.PORT)) {
        _host = _host.substring(0, _host.length() - 1);
      }
    }
    return _host;
  }

  /**
   * port = 0 means it hasn't been set yet. port = -1 means there is no port
   */
  public int getPort() {
    if (_port == 0) {
      String portString = getPart(UrlPart.PORT);
      if (portString != null && !portString.isEmpty()) {
        try {
          _port = Integer.parseInt(portString);
        } catch (NumberFormatException e) {
          _port = -1;
        }
      } else if (SCHEME_PORT_MAP.containsKey(getScheme())) {
        _port = SCHEME_PORT_MAP.get(getScheme());
      } else {
        _port = -1;
      }
    }
    return _port;
  }

  public String getPath() {
    if (_path == null) {
      _path = exists(UrlPart.PATH) ? getPart(UrlPart.PATH) : "/";
    }
    return _path;
  }

  public String getQuery() {
    if (_query == null) {
      _query = getPart(UrlPart.QUERY);
    }
    return _query == null ? "" : _query;
  }

  public String getFragment() {
    if (_fragment == null) {
      _fragment = getPart(UrlPart.FRAGMENT);
    }
    return _fragment == null ? "" : _fragment;
  }

  /**
   * Always returns null for non normalized urls.
   */
  public byte[] getHostBytes() {
    return null;
  }

  public String getOriginalUrl() {
    return _originalUrl;
  }

  private void populateUsernamePassword() {
    if (exists(UrlPart.USERNAME_PASSWORD)) {
      String usernamePassword = getPart(UrlPart.USERNAME_PASSWORD);
      String[] usernamePasswordParts = usernamePassword.substring(0, usernamePassword.length() - 1).split(":");
      if (usernamePasswordParts.length == 1) {
        _username = usernamePasswordParts[0];
      } else if (usernamePasswordParts.length == 2) {
        _username = usernamePasswordParts[0];
        _password = usernamePasswordParts[1];
      }
    }
  }

  /**
   * @param urlPart The url part we are checking for existence
   * @return Returns true if the part exists.
   */
  private boolean exists(UrlPart urlPart) {
    return urlPart != null && _urlMarker.indexOf(urlPart) >= 0;
  }

  /**
   * For example, in http://yahoo.com/lala/, nextExistingPart(UrlPart.HOST) would return UrlPart.PATH
   * @param urlPart The current url part
   * @return Returns the next part; if there is no existing next part, it returns null
   */
  private UrlPart nextExistingPart(UrlPart urlPart) {
    UrlPart nextPart = urlPart.getNextPart();
    if (exists(nextPart)) {
      return nextPart;
    } else if (nextPart == null) {
      return null;
    } else {
      return nextExistingPart(nextPart);
    }
  }

  /**
   * @param part The part that we want. Ex: host, path
   */
  private String getPart(UrlPart part) {
    if (!exists(part)) {
      return null;
    }

    UrlPart nextPart = nextExistingPart(part);
    if (nextPart == null) {
      return _originalUrl.substring(_urlMarker.indexOf(part));
    }
    return _originalUrl.substring(_urlMarker.indexOf(part), _urlMarker.indexOf(nextPart));
  }

  protected void setRawPath(String path) {
    _path = path;
  }

  protected void setRawHost(String host) {
    _host = host;
  }

  protected String getRawPath() {
    return _path;
  }

  protected String getRawHost() {
    return _host;
  }

  protected UrlMarker getUrlMarker() {
    return _urlMarker;
  }
}
