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

import com.linkedin.urls.Url;
import com.linkedin.urls.UrlMarker;
import com.linkedin.urls.UrlPart;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class UrlDetector {
  /**
   * Contains the string to check for and remove if the scheme is this.
   */
  private static final String HTML_MAILTO = "mailto:";

  /**
   * Valid protocol schemes.
   */
  private static final Set<String> VALID_SCHEMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
      "http://", "https://", "ftp://", "ftps://", "http%3a//", "https%3a//", "ftp%3a//", "ftps%3a//")));

  /**
   * Valid protocol schemes as defined by IANA for enhanced detection.
   */
  private static final Set<String> VALID_IANA_SCHEMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
    "fax://", "fax%3a//", "filesystem://", "filesystem%3a//", "mailserver://", "mailserver%3a//", "modem://", "modem%3a//", "pack://", "pack%3a//", "prospero://", "prospero%3a//", "snews://", "snews%3a//", "videotex://", "videotex%3a//", "wais://", "wais%3a//", "wpid://", "wpid%3a//", "z39.50://", "z39.50%3a//", "aaa://", "aaa%3a//", "aaas://", "aaas%3a//", "about://", "about%3a//", "acap://", "acap%3a//", "acct://", "acct%3a//", "cap://", "cap%3a//", "cid://", "cid%3a//", "coap://", "coap%3a//", "coap+tcp://", "coap+tcp%3a//", "coap+ws://", "coap+ws%3a//", "coaps://", "coaps%3a//", "coaps+tcp://", "coaps+tcp%3a//", "coaps+ws://", "coaps+ws%3a//", "crid://", "crid%3a//", "data://", "data%3a//", "dav://", "dav%3a//", "dict://", "dict%3a//", "dns://", "dns%3a//", "dtn://", "dtn%3a//", "example://", "example%3a//", "file://", "file%3a//", "ftp://", "ftp%3a//", "geo://", "geo%3a//", "go://", "go%3a//", "gopher://", "gopher%3a//", "h323://", "h323%3a//", "http://", "http%3a//", "https://", "https%3a//", "iax://", "iax%3a//", "icap://", "icap%3a//", "im://", "im%3a//", "imap://", "imap%3a//", "info://", "info%3a//", "ipn://", "ipn%3a//", "ipp://", "ipp%3a//", "ipps://", "ipps%3a//", "iris://", "iris%3a//", "iris.beep://", "iris.beep%3a//", "iris.lwz://", "iris.lwz%3a//", "iris.xpc://", "iris.xpc%3a//", "iris.xpcs://", "iris.xpcs%3a//", "jabber://", "jabber%3a//", "ldap://", "ldap%3a//", "leaptofrogans://", "leaptofrogans%3a//", "mailto://", "mailto%3a//", "mid://", "mid%3a//", "msrp://", "msrp%3a//", "msrps://", "msrps%3a//", "mtqp://", "mtqp%3a//", "mupdate://", "mupdate%3a//", "news://", "news%3a//", "nfs://", "nfs%3a//", "ni://", "ni%3a//", "nih://", "nih%3a//", "nntp://", "nntp%3a//", "opaquelocktoken://", "opaquelocktoken%3a//", "pkcs11://", "pkcs11%3a//", "pop://", "pop%3a//", "pres://", "pres%3a//", "reload://", "reload%3a//", "rtsp://", "rtsp%3a//", "rtsps://", "rtsps%3a//", "rtspu://", "rtspu%3a//", "service://", "service%3a//", "session://", "session%3a//", "shttp://", "shttp%3a//", "(OBSOLETE)://", "(OBSOLETE)%3a//", "sieve://", "sieve%3a//", "sip://", "sip%3a//", "sips://", "sips%3a//", "sms://", "sms%3a//", "snmp://", "snmp%3a//", "soap.beep://", "soap.beep%3a//", "soap.beeps://", "soap.beeps%3a//", "stun://", "stun%3a//", "stuns://", "stuns%3a//", "tag://", "tag%3a//", "tel://", "tel%3a//", "telnet://", "telnet%3a//", "tftp://", "tftp%3a//", "thismessage://", "thismessage%3a//", "tip://", "tip%3a//", "tn3270://", "tn3270%3a//", "turn://", "turn%3a//", "turns://", "turns%3a//", "tv://", "tv%3a//", "urn://", "urn%3a//", "vemmi://", "vemmi%3a//", "vnc://", "vnc%3a//", "ws://", "ws%3a//", "wss://", "wss%3a//", "xcon://", "xcon%3a//", "xcon-userid://", "xcon-userid%3a//", "xmlrpc.beep://", "xmlrpc.beep%3a//", "xmlrpc.beeps://", "xmlrpc.beeps%3a//", "xmpp://", "xmpp%3a//", "z39.50r://", "z39.50r%3a//", "z39.50s://", "z39.50s%3a//", "acd://", "acd%3a//", "acr://", "acr%3a//", "adiumxtra://", "adiumxtra%3a//", "adt://", "adt%3a//", "afp://", "afp%3a//", "afs://", "afs%3a//", "aim://", "aim%3a//", "amss://", "amss%3a//", "android://", "android%3a//", "appdata://", "appdata%3a//", "apt://", "apt%3a//", "ar://", "ar%3a//", "ark://", "ark%3a//", "attachment://", "attachment%3a//", "aw://", "aw%3a//", "barion://", "barion%3a//", "beshare://", "beshare%3a//", "bitcoin://", "bitcoin%3a//", "bitcoincash://", "bitcoincash%3a//", "blob://", "blob%3a//", "bolo://", "bolo%3a//", "browserext://", "browserext%3a//", "cabal://", "cabal%3a//", "calculator://", "calculator%3a//", "callto://", "callto%3a//", "cast://", "cast%3a//", "casts://", "casts%3a//", "chrome://", "chrome%3a//", "chrome-extension://", "chrome-extension%3a//", "com-eventbrite-attendee://", "com-eventbrite-attendee%3a//", "content://", "content%3a//", "content-type://", "content-type%3a//", "cvs://", "cvs%3a//", "dab://", "dab%3a//", "dat://", "dat%3a//", "diaspora://", "diaspora%3a//", "did://", "did%3a//", "dis://", "dis%3a//", "dlna-playcontainer://", "dlna-playcontainer%3a//", "dlna-playsingle://", "dlna-playsingle%3a//", "dntp://", "dntp%3a//", "doi://", "doi%3a//", "dpp://", "dpp%3a//", "drm://", "drm%3a//", "drop://", "drop%3a//", "dtmi://", "dtmi%3a//", "dvb://", "dvb%3a//", "dvx://", "dvx%3a//", "dweb://", "dweb%3a//", "ed2k://", "ed2k%3a//", "elsi://", "elsi%3a//", "embedded://", "embedded%3a//", "ens://", "ens%3a//", "ethereum://", "ethereum%3a//", "facetime://", "facetime%3a//", "feed://", "feed%3a//", "feedready://", "feedready%3a//", "fido://", "fido%3a//", "finger://", "finger%3a//", "first-run-pen-experience://", "first-run-pen-experience%3a//", "fish://", "fish%3a//", "fm://", "fm%3a//", "fuchsia-pkg://", "fuchsia-pkg%3a//", "gg://", "gg%3a//", "git://", "git%3a//", "gizmoproject://", "gizmoproject%3a//", "graph://", "graph%3a//", "gtalk://", "gtalk%3a//", "ham://", "ham%3a//", "hcap://", "hcap%3a//", "hcp://", "hcp%3a//", "hxxp://", "hxxp%3a//", "hxxps://", "hxxps%3a//", "hydrazone://", "hydrazone%3a//", "hyper://", "hyper%3a//", "icon://", "icon%3a//", "iotdisco://", "iotdisco%3a//", "ipfs://", "ipfs%3a//", "ipns://", "ipns%3a//", "irc://", "irc%3a//", "irc6://", "irc6%3a//", "ircs://", "ircs%3a//", "isostore://", "isostore%3a//", "itms://", "itms%3a//", "jar://", "jar%3a//", "jms://", "jms%3a//", "keyparc://", "keyparc%3a//", "lastfm://", "lastfm%3a//", "lbry://", "lbry%3a//", "ldaps://", "ldaps%3a//", "lorawan://", "lorawan%3a//", "lvlt://", "lvlt%3a//", "magnet://", "magnet%3a//", "maps://", "maps%3a//", "market://", "market%3a//", "matrix://", "matrix%3a//", "message://", "message%3a//", "microsoft.windows.camera://", "microsoft.windows.camera%3a//", "microsoft.windows.camera.multipicker://", "microsoft.windows.camera.multipicker%3a//", "microsoft.windows.camera.picker://", "microsoft.windows.camera.picker%3a//", "mms://", "mms%3a//", "mongodb://", "mongodb%3a//", "moz://", "moz%3a//", "ms-access://", "ms-access%3a//", "ms-appinstaller://", "ms-appinstaller%3a//", "ms-browser-extension://", "ms-browser-extension%3a//", "ms-calculator://", "ms-calculator%3a//", "ms-drive-to://", "ms-drive-to%3a//", "ms-enrollment://", "ms-enrollment%3a//", "ms-excel://", "ms-excel%3a//", "ms-eyecontrolspeech://", "ms-eyecontrolspeech%3a//", "ms-gamebarservices://", "ms-gamebarservices%3a//", "ms-gamingoverlay://", "ms-gamingoverlay%3a//", "ms-getoffice://", "ms-getoffice%3a//", "ms-help://", "ms-help%3a//", "ms-infopath://", "ms-infopath%3a//", "ms-inputapp://", "ms-inputapp%3a//", "ms-lockscreencomponent-config://", "ms-lockscreencomponent-config%3a//", "ms-media-stream-id://", "ms-media-stream-id%3a//", "ms-meetnow://", "ms-meetnow%3a//", "ms-mixedrealitycapture://", "ms-mixedrealitycapture%3a//", "ms-mobileplans://", "ms-mobileplans%3a//", "ms-officeapp://", "ms-officeapp%3a//", "ms-people://", "ms-people%3a//", "ms-project://", "ms-project%3a//", "ms-powerpoint://", "ms-powerpoint%3a//", "ms-publisher://", "ms-publisher%3a//", "ms-restoretabcompanion://", "ms-restoretabcompanion%3a//", "ms-screenclip://", "ms-screenclip%3a//", "ms-screensketch://", "ms-screensketch%3a//", "ms-search://", "ms-search%3a//", "ms-search-repair://", "ms-search-repair%3a//", "ms-secondary-screen-controller://", "ms-secondary-screen-controller%3a//", "ms-secondary-screen-setup://", "ms-secondary-screen-setup%3a//", "ms-settings://", "ms-settings%3a//", "ms-settings-airplanemode://", "ms-settings-airplanemode%3a//", "ms-settings-bluetooth://", "ms-settings-bluetooth%3a//", "ms-settings-camera://", "ms-settings-camera%3a//", "ms-settings-cellular://", "ms-settings-cellular%3a//", "ms-settings-cloudstorage://", "ms-settings-cloudstorage%3a//", "ms-settings-connectabledevices://", "ms-settings-connectabledevices%3a//", "ms-settings-displays-topology://", "ms-settings-displays-topology%3a//", "ms-settings-emailandaccounts://", "ms-settings-emailandaccounts%3a//", "ms-settings-language://", "ms-settings-language%3a//", "ms-settings-location://", "ms-settings-location%3a//", "ms-settings-lock://", "ms-settings-lock%3a//", "ms-settings-nfctransactions://", "ms-settings-nfctransactions%3a//", "ms-settings-notifications://", "ms-settings-notifications%3a//", "ms-settings-power://", "ms-settings-power%3a//", "ms-settings-privacy://", "ms-settings-privacy%3a//", "ms-settings-proximity://", "ms-settings-proximity%3a//", "ms-settings-screenrotation://", "ms-settings-screenrotation%3a//", "ms-settings-wifi://", "ms-settings-wifi%3a//", "ms-settings-workplace://", "ms-settings-workplace%3a//", "ms-spd://", "ms-spd%3a//", "ms-stickers://", "ms-stickers%3a//", "ms-sttoverlay://", "ms-sttoverlay%3a//", "ms-transit-to://", "ms-transit-to%3a//", "ms-useractivityset://", "ms-useractivityset%3a//", "ms-virtualtouchpad://", "ms-virtualtouchpad%3a//", "ms-visio://", "ms-visio%3a//", "ms-walk-to://", "ms-walk-to%3a//", "ms-whiteboard://", "ms-whiteboard%3a//", "ms-whiteboard-cmd://", "ms-whiteboard-cmd%3a//", "ms-word://", "ms-word%3a//", "msnim://", "msnim%3a//", "mss://", "mss%3a//", "mt://", "mt%3a//", "mumble://", "mumble%3a//", "mvn://", "mvn%3a//", "notes://", "notes%3a//", "num://", "num%3a//", "ocf://", "ocf%3a//", "oid://", "oid%3a//", "onenote://", "onenote%3a//", "onenote-cmd://", "onenote-cmd%3a//", "openpgp4fpr://", "openpgp4fpr%3a//", "otpauth://", "otpauth%3a//", "palm://", "palm%3a//", "paparazzi://", "paparazzi%3a//", "payment://", "payment%3a//", "payto://", "payto%3a//", "platform://", "platform%3a//", "proxy://", "proxy%3a//", "pwid://", "pwid%3a//", "psyc://", "psyc%3a//", "pttp://", "pttp%3a//", "qb://", "qb%3a//", "query://", "query%3a//", "quic-transport://", "quic-transport%3a//", "redis://", "redis%3a//", "rediss://", "rediss%3a//", "res://", "res%3a//", "resource://", "resource%3a//", "rmi://", "rmi%3a//", "rsync://", "rsync%3a//", "rtmfp://", "rtmfp%3a//", "rtmp://", "rtmp%3a//", "sarif://", "sarif%3a//", "secondlife://", "secondlife%3a//", "secret-token://", "secret-token%3a//", "sftp://", "sftp%3a//", "sgn://", "sgn%3a//", "shc://", "shc%3a//", "simpleledger://", "simpleledger%3a//", "simplex://", "simplex%3a//", "skype://", "skype%3a//", "smb://", "smb%3a//", "smp://", "smp%3a//", "smtp://", "smtp%3a//", "soldat://", "soldat%3a//", "spiffe://", "spiffe%3a//", "spotify://", "spotify%3a//", "ssb://", "ssb%3a//", "ssh://", "ssh%3a//", "steam://", "steam%3a//", "submit://", "submit%3a//", "svn://", "svn%3a//", "swh://", "swh%3a//", "swid://", "swid%3a//", "swidpath://", "swidpath%3a//", "teamspeak://", "teamspeak%3a//", "teliaeid://", "teliaeid%3a//", "things://", "things%3a//", "tool://", "tool%3a//", "udp://", "udp%3a//", "unreal://", "unreal%3a//", "ut2004://", "ut2004%3a//", "uuid-in-package://", "uuid-in-package%3a//", "v-event://", "v-event%3a//", "ventrilo://", "ventrilo%3a//", "ves://", "ves%3a//", "view-source://", "view-source%3a//", "vscode://", "vscode%3a//", "vscode-insiders://", "vscode-insiders%3a//", "vsls://", "vsls%3a//", "wcr://", "wcr%3a//", "webcal://", "webcal%3a//", "wifi://", "wifi%3a//", "wtai://", "wtai%3a//", "wyciwyg://", "wyciwyg%3a//", "xfire://", "xfire%3a//", "xri://", "xri%3a//", "ymsgr://", "ymsgr%3a//")));

  /**
   * The response of character matching.
   */
  private enum CharacterMatch {
    /**
     * The character was not matched.
     */
    CharacterNotMatched,
    /**
     * A character was matched with requires a stop.
     */
    CharacterMatchStop,
    /**
     * The character was matched which is a start of parentheses.
     */
    CharacterMatchStart
  }

  /**
   * Stores options for detection.
   */
  private final UrlDetectorOptionsList _options;

  /**
   * The input stream to read.
   */
  private final InputTextReader _reader;

  /**
   * Buffer to store temporary urls inside of.
   */
  private StringBuilder _buffer = new StringBuilder();

  /**
   * Has the scheme been found in this iteration?
   */
  private boolean _hasScheme = false;

  /**
   * If the first character in the url is a quote, then look for matching quote at the end.
   */
  private boolean _quoteStart = false;

  /**
   * If the first character in the url is a single quote, then look for matching quote at the end.
   */
  private boolean _singleQuoteStart = false;

  /**
   * If we see a '[', didn't find an ipv6 address, and the bracket option is on, then look for urls inside the brackets.
   */
  private boolean _dontMatchIpv6 = false;

  /**
   * Stores the found urls.
   */
  private ArrayList<Url> _urlList = new ArrayList<Url>();

  /**
   * Keeps the count of special characters used to match quotes and different types of brackets.
   */
  private HashMap<Character, Integer> _characterMatch = new HashMap<Character, Integer>();

  /**
   * Keeps track of certain indices to create a Url object.
   */
  private UrlMarker _currentUrlMarker = new UrlMarker();

  /**
   * The states to use to continue writing or not.
   */
  public enum ReadEndState {
    /**
     * The current url is valid.
     */
    ValidUrl,
    /**
     * The current url is invalid.
     */
    InvalidUrl
  }

  /**
   * Creates a new UrlDetector object used to find urls inside of text.
   * @param content The content to search inside of.
   * @param options The UrlDetectorOptions to use when detecting the content.
   */
  public UrlDetector(String content, UrlDetectorOptions options) {
    _reader = new InputTextReader(content);
    UrlDetectorOptionsList optList = new UrlDetectorOptionsList.UrlDetectorOptionsListBuilder()
       .addOption(options).build();
     _options = optList;
   }

   /**
    * Creates a new UrlDetector object used to find urls inside of text, with a DEFAULT config.
    * @param content The content to search inside of.
    */
   public UrlDetector(String content) {
     _reader = new InputTextReader(content);
     UrlDetectorOptionsList optList = new UrlDetectorOptionsList.UrlDetectorOptionsListBuilder().build();
     _options = optList;
   }

   /**
    * Creates a new UrlDetector object used to find urls inside of text, with the provided list of config options.
    * @param content The content to search inside of.
    * @param optionsList The UrlDetectorOptionsList to use when detecting the content.
    */
   public UrlDetector(String content, UrlDetectorOptionsList optionsList) {
     _reader = new InputTextReader(content);
     _options = optionsList;
   }

  /**
   * Detects the urls and returns a list of detected url strings.
   * @return A list with detected urls.
   */
  public List<Url> detect() {
    readDefault();
    return _urlList;
  }

  /**
   * The default input reader which looks for specific flags to start detecting the url.
   */
  private void readDefault() {
    //Keeps track of the number of characters read to be able to later cut out the domain name.
    int length = 0;
    int position = 0;

    //until end of string read the contents
    while (!_reader.eof()) {
        
      //read the next char to process.
      char curr = _reader.read();
      switch (curr) {
        case ' ':
          //space was found, check if it's a valid single level domain.
          if (_options.hasFlag(UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN) && _buffer.length() > 0 && _hasScheme) {
            _reader.goBack();
            if (!readDomainName(_buffer.substring(length))) {
              readEnd(ReadEndState.InvalidUrl);
            };
          }
          _buffer.append(curr);
          readEnd(ReadEndState.InvalidUrl);
          length = 0;
          break;
        case '%':
          if (_reader.canReadChars(2)) {
            if (_reader.peek(2).equalsIgnoreCase("3a")) {
              _buffer.append(curr);
              _buffer.append(_reader.read());
              _buffer.append(_reader.read());
              length = processColon(length);
            } else if (CharUtils.isHex(_reader.peekChar(0)) && CharUtils.isHex(_reader.peekChar(1))) {
              _buffer.append(curr);
              _buffer.append(_reader.read());
              _buffer.append(_reader.read());

              if (!readDomainName(_buffer.substring(length))) {
                readEnd(ReadEndState.InvalidUrl);
              }
              length = 0;
            }
          }
          break;
        case '\u3002': //non-standard dots
        case '\uFF0E':
        case '\uFF61':
        case '.': //"." was found, read the domain name using the start from length.
          _buffer.append(curr);
          if (!readDomainName(_buffer.substring(length))) {
            readEnd(ReadEndState.InvalidUrl);
          }
          length = 0;
          break;
        case '@': //Check the domain name after a username
          if (_buffer.length() > 0) {
            _currentUrlMarker.setIndex(UrlPart.USERNAME_PASSWORD, length);
            _buffer.append(curr);
            if (!readDomainName(null)) {
              readEnd(ReadEndState.InvalidUrl);
            }
            length = 0;
          }
          break;
        case '[':
          if (_dontMatchIpv6) {
            //Check if we need to match characters. If we match characters and this is a start or stop of range,
            //either way reset the world and start processing again.
            if (checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
              readEnd(ReadEndState.InvalidUrl);
              length = 0;
            }
          }
          int beginning = _reader.getPosition();

          //if it doesn't have a scheme, clear the buffer.
          if (!_hasScheme) {
            _buffer.delete(0, _buffer.length());
          }
          _buffer.append(curr);

          if (!readDomainName(_buffer.substring(length))) {
            //if we didn't find an ipv6 address, then check inside the brackets for urls
            readEnd(ReadEndState.InvalidUrl);
            _reader.seek(beginning);
            _dontMatchIpv6 = true;
          }
          length = 0;
          break;
        case '/':
          // "/" was found, then we either read a scheme, or if we already read a scheme, then
          // we are reading a url in the format http://123123123/asdf

          if (_hasScheme || (_options.hasFlag(UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN) && _buffer.length() > 1)) {
            //we already have the scheme, so then we already read:
            //http://something/ <- if something is all numeric then its a valid url.
            //OR we are searching for single level domains. We have buffer length > 1 condition
            //to weed out infinite backtrack in cases of html5 roots

            //unread this "/" and continue to check the domain name starting from the beginning of the domain
            _reader.goBack();
            if (!readDomainName(_buffer.substring(length))) {
              readEnd(ReadEndState.InvalidUrl);
            }
            length = 0;
          } else {

            //we don't have a scheme already, then clear state, then check for html5 root such as: "//google.com/"
            // remember the state of the quote when clearing state just in case its "//google.com" so its not cleared.
            readEnd(ReadEndState.InvalidUrl);
            _buffer.append(curr);
            _hasScheme = readHtml5Root();
            length = _buffer.length();
          }
          break;
        case ':':
          //add the ":" to the url and check for scheme/username
          _buffer.append(curr);
          length = processColon(length);
          break;
        default:
          //Check if we need to match characters. If we match characters and this is a start or stop of range,
          //either way reset the world and start processing again.
          if (checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
            readEnd(ReadEndState.InvalidUrl);
            length = 0;
          } else {
            _buffer.append(curr);
          }
          break;
      }
          
      if (position == _reader.getPosition()) {
          // we haven't made any progress, advance by one char
          _reader.read();
      }
      
      position = _reader.getPosition();
    }
    if (_options.hasFlag(UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN) && _buffer.length() > 0 && _hasScheme) {
      if (!readDomainName(_buffer.substring(length))) {
        readEnd(ReadEndState.InvalidUrl);
      }
    }
  }

  /**
   * We found a ":" and is now trying to read either scheme, username/password
   * @param length first index of the previous part (could be beginning of the buffer, beginning of the username/password, or beginning
   * @return new index of where the domain starts
   */
  private int processColon(int length) {
    if (_hasScheme) {
      //read it as username/password if it has scheme
      if (!readUserPass(length)) {
        //unread the ":" so that the domain reader can process it
        _reader.goBack();
        
        // Check buffer length before clearing it; set length to 0 if buffer is empty
        if (_buffer.length() > 0) {
          _buffer.delete(_buffer.length() - 1, _buffer.length());
        } else {
          length = 0;
        }

        int backtrackOnFail = _reader.getPosition() - _buffer.length() + length;
        if (!readDomainName(_buffer.substring(length))) {
          //go back to length location and restart search
          _reader.seek(backtrackOnFail);
          readEnd(ReadEndState.InvalidUrl);
        }
        length = 0;
      } else {
    	length = 0;
      }
    } else if (readScheme() && _buffer.length() > 0) {
      _hasScheme = true;
      length = _buffer.length(); //set length to be right after the scheme
    } else if (_buffer.length() > 0 && _options.hasFlag(UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN)
        && _reader.canReadChars(1)) { //takes care of case like hi:
      _reader.goBack(); //unread the ":" so readDomainName can take care of the port
      _buffer.delete(_buffer.length() - 1, _buffer.length());
      if (!readDomainName(_buffer.toString())) {
        readEnd(ReadEndState.InvalidUrl);
      }
    } else {
      readEnd(ReadEndState.InvalidUrl);
      length = 0;
    }

    return length;
  }

  /**
   * Gets the number of times the current character was seen in the document. Only special characters are tracked.
   * @param curr The character to look for.
   * @return The number of times that character was seen
   */
  private int getCharacterCount(char curr) {
    Integer count = _characterMatch.get(curr);
    return count == null ? 0 : count;
  }

  /**
   * Increments the counter for the characters seen and return if this character matches a special character
   * that might require stopping reading the url.
   * @param curr The character to check.
   * @return The state that this character requires.
   */
  private CharacterMatch checkMatchingCharacter(char curr) {

    //This is a quote and we are matching quotes.
    if ((curr == '\"' && _options.hasFlag(UrlDetectorOptions.QUOTE_MATCH))
        || (curr == '\'' && _options.hasFlag(UrlDetectorOptions.SINGLE_QUOTE_MATCH))) {
      boolean quoteStart;
      if (curr == '\"') {
        quoteStart = _quoteStart;

        //remember that a double quote was found.
        _quoteStart = true;
      } else {
        quoteStart = _singleQuoteStart;

        //remember that a single quote was found.
        _singleQuoteStart = true;
      }

      //increment the number of quotes found.
      Integer currVal = getCharacterCount(curr) + 1;
      _characterMatch.put(curr, currVal);

      //if there was already a quote found, or the number of quotes is even, return that we have to stop, else its a start.
      return quoteStart || currVal % 2 == 0 ? CharacterMatch.CharacterMatchStop : CharacterMatch.CharacterMatchStart;
    } else if (_options.hasFlag(UrlDetectorOptions.BRACKET_MATCH) && (curr == '[' || curr == '{' || curr == '(')) {
      //Look for start of bracket
      _characterMatch.put(curr, getCharacterCount(curr) + 1);
      return CharacterMatch.CharacterMatchStart;
    } else if (_options.hasFlag(UrlDetectorOptions.XML) && (curr == '<')) {
      //If its html, look for "<"
      _characterMatch.put(curr, getCharacterCount(curr) + 1);
      return CharacterMatch.CharacterMatchStart;
    } else if ((_options.hasFlag(UrlDetectorOptions.BRACKET_MATCH) && (curr == ']' || curr == '}' || curr == ')'))
        || (_options.hasFlag(UrlDetectorOptions.XML) && (curr == '>'))) {

      //If we catch a end bracket increment its count and get rid of not ipv6 flag
      Integer currVal = getCharacterCount(curr) + 1;
      _characterMatch.put(curr, currVal);

      //now figure out what the start bracket was associated with the closed bracket.
      char match = '\0';
      switch (curr) {
        case ']':
          match = '[';
          break;
        case '}':
          match = '{';
          break;
        case ')':
          match = '(';
          break;
        case '>':
          match = '<';
          break;
        default:
          break;
      }

      //If the number of open is greater then the number of closed, return a stop.
      return getCharacterCount(match) > currVal ? CharacterMatch.CharacterMatchStop
          : CharacterMatch.CharacterMatchStart;
    }

    //Nothing else was found.
    return CharacterMatch.CharacterNotMatched;
  }

  /**
   * Checks if the url is in the format:
   * //google.com/static/js.js
   * @return True if the url is in this format and was matched correctly.
   */
  private boolean readHtml5Root() {
    //end of input then go away.
    if (_reader.eof()) {
      return false;
    }

    //read the next character. If its // then return true.
    char curr = _reader.read();
    if (curr == '/') {
      _buffer.append(curr);
      return true;
    } else {
      //if its not //, then go back and reset by 1 character.
      _reader.goBack();
      readEnd(ReadEndState.InvalidUrl);
    }
    return false;
  }

  /**
   * Reads the scheme and allows returns true if the scheme is http(s?):// or ftp(s?)://
   * @return True if the scheme was found, else false.
   */
  private boolean readScheme() {
    //Check if we are checking html and the length is longer than mailto:
    if (_options.hasFlag(UrlDetectorOptions.HTML) && _buffer.length() >= HTML_MAILTO.length()) {
      //Check if the string is actually mailto: then just return nothing.
      if (HTML_MAILTO.equalsIgnoreCase(_buffer.substring(_buffer.length() - HTML_MAILTO.length()))) {
        return readEnd(ReadEndState.InvalidUrl);
      }
    }

    int originalLength = _buffer.length();
    int numSlashes = 0;

    while (!_reader.eof()) {
      char curr = _reader.read();

      //if we match a slash, look for a second one.
      if (curr == '/') {
        _buffer.append(curr);
        if (numSlashes == 1) {
          //return only if its an approved protocol. This can be expanded to allow others
          int schemeStartIndex = findValidSchemeStartIndex(_buffer.toString());
          if (schemeStartIndex >= 0) {
            _buffer.delete(0, schemeStartIndex);
            _currentUrlMarker.setIndex(UrlPart.SCHEME, 0);
            return true;
          } else {
            return false;
          }
        }
        numSlashes++;
      } else if (curr == ' ' || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
        //if we find a space or end of input, then nothing found.
        _buffer.append(curr);
        return false;
      } else if (curr == '[') { //if we're starting to see an ipv6 address
        _reader.goBack(); //unread the '[', so that we can start looking for ipv6
        return false;
      } else if (originalLength > 0 || numSlashes > 0 || !CharUtils.isAlpha(curr)) {
        // if it's not a character a-z or A-Z then assume we aren't matching scheme, but instead
        // matching username and password.
        _reader.goBack();
        return readUserPass(0);
      }
    }

    return false;
  }

  private Integer findValidSchemeStartIndex(final String optionalScheme) {
    final String optionalSchemeLowercase = optionalScheme.toLowerCase();
    if (_options.hasFlag(UrlDetectorOptions.EXTENDED_IANA_DETECTION)){
      // If the option is set to check for all IANA defined schemes, use the IANA list
      return VALID_IANA_SCHEMES.stream()
        .filter(optionalSchemeLowercase::endsWith)
        .map(optionalSchemeLowercase::lastIndexOf)
        .findFirst().orElse(-1);
    } // Otherwise use the classic "web" scheme list
    return VALID_SCHEMES.stream()
      .filter(optionalSchemeLowercase::endsWith)
      .map(optionalSchemeLowercase::lastIndexOf)
      .findFirst().orElse(-1);
  }

  /**
   * Reads the input and looks for a username and password.
   * Handles:
   * http://username:password@...
   * @param beginningOfUsername Index of the buffer of where the username began
   * @return True if a valid username and password was found.
   */
  private boolean readUserPass(int beginningOfUsername) {
    //The start of where we are.
    int start = _buffer.length();
    
    //keep looping until "done"
    boolean done = false;

    //if we had a dot in the input, then it might be a domain name and not a username and password.
    boolean rollback = false;
    while (!done && !_reader.eof()) {
      char curr = _reader.read();

      // if we hit this, then everything is ok and we are matching a domain name.
      if (curr == '@') {
        _buffer.append(curr);
        _currentUrlMarker.setIndex(UrlPart.USERNAME_PASSWORD, beginningOfUsername);
        return readDomainName("");
      } else if (CharUtils.isDot(curr) || curr == '[') {
        //everything is still ok, just remember that we found a dot or '[' in case we might need to backtrack
        _buffer.append(curr);
        rollback = true;
      } else if (curr == '#' || curr == ' ' || curr == '/'
          || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
        //one of these characters indicates we are invalid state and should just return.
        rollback = true;
        done = true;
      } else {
        //all else, just append character assuming its ok so far.
        _buffer.append(curr);
      }
    }

    if (rollback) {
      //got to here, so there is no username and password. (We didn't find a @)
      int distance = _buffer.length() - start;
      _buffer.delete(start, _buffer.length());

      int currIndex = Math.max(_reader.getPosition() - distance - (done ? 1 : 0), 0);
      _reader.seek(currIndex);

      return false;
    } else {
      return readEnd(ReadEndState.InvalidUrl);
    }
  }

  /**
   * Try to read the current string as a domain name
   * @param current The current string used.
   * @return Whether the domain is valid or not.
   */
  private boolean readDomainName(String current) {
    int hostIndex = current == null ? _buffer.length() : _buffer.length() - current.length();
    _currentUrlMarker.setIndex(UrlPart.HOST, hostIndex);
    //create the domain name reader and specify the handler that will be called when a quote character
    //or something is found.
    DomainNameReader reader =
        new DomainNameReader(_reader, _buffer, current, _options, new DomainNameReader.CharacterHandler() {
          @Override
          public void addCharacter(char character) {
            checkMatchingCharacter(character);
          }
        });

    //Try to read the dns and act on the response.
    DomainNameReader.ReaderNextState state = reader.readDomainName();
    switch (state) {
      case ValidDomainName:
        return readEnd(ReadEndState.ValidUrl);
      case ReadFragment:
        return readFragment();
      case ReadPath:
        return readPath();
      case ReadPort:
        return readPort();
      case ReadQueryString:
        return readQueryString();
      case ReadUserPass:
        int host = _currentUrlMarker.indexOf(UrlPart.HOST);
        _currentUrlMarker.unsetIndex(UrlPart.HOST);
        return readUserPass(host);
      default:
        return false;
    }
  }

  /**
   * Reads the fragments which is the part of the url starting with #
   * @return If a valid fragment was read true, else false.
   */
  private boolean readFragment() {
    _currentUrlMarker.setIndex(UrlPart.FRAGMENT, _buffer.length() - 1);

    while (!_reader.eof()) {
      char curr = _reader.read();

      //if it's the end or space, then a valid url was read.
      if (curr == ' ' || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
        return readEnd(ReadEndState.ValidUrl);
      } else {
        //otherwise keep appending.
        _buffer.append(curr);
      }
    }

    //if we are here, anything read is valid.
    return readEnd(ReadEndState.ValidUrl);
  }

  /**
   * Try to read the query string.
   * @return True if the query string was valid.
   */
  private boolean readQueryString() {
    _currentUrlMarker.setIndex(UrlPart.QUERY, _buffer.length() - 1);

    while (!_reader.eof()) {
      char curr = _reader.read();

      if (curr == '#') { //fragment
        _buffer.append(curr);
        return readFragment();
      } else if (curr == ' ' || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
        //end of query string
        return readEnd(ReadEndState.ValidUrl);
      } else { //all else add to buffer.
        _buffer.append(curr);
      }
    }
    //a valid url was read.
    return readEnd(ReadEndState.ValidUrl);
  }

  /**
   * Try to read the port of the url.
   * @return True if a valid port was read.
   */
  private boolean readPort() {
    _currentUrlMarker.setIndex(UrlPart.PORT, _buffer.length());
    //The length of the port read.
    int portLen = 0;
    while (!_reader.eof()) {
      //read the next one and remember the length
      char curr = _reader.read();
      portLen++;

      if (curr == '/') {
        //continue to read path
        _buffer.append(curr);
        return readPath();
      } else if (curr == '?') {
        //continue to read query string
        _buffer.append(curr);
        return readQueryString();
      } else if (curr == '#') {
        //continue to read fragment.
        _buffer.append(curr);
        return readFragment();
      } else if (checkMatchingCharacter(curr) == CharacterMatch.CharacterMatchStop || !CharUtils.isNumeric(curr)) {
        //if we got here, then what we got so far is a valid url. don't append the current character.
        _reader.goBack();

        //no port found; it was something like google.com:hello.world
        if (portLen == 1) {
          //remove the ":" from the end.
          _buffer.delete(_buffer.length() - 1, _buffer.length());
        }
        _currentUrlMarker.unsetIndex(UrlPart.PORT);
        return readEnd(ReadEndState.ValidUrl);
      } else {
        //this is a valid character in the port string.
        _buffer.append(curr);
      }
    }

    //found a correct url
    return readEnd(ReadEndState.ValidUrl);
  }

  /**
   * Tries to read the path
   * @return True if the path is valid.
   */
  private boolean readPath() {
    _currentUrlMarker.setIndex(UrlPart.PATH, _buffer.length() - 1);
    while (!_reader.eof()) {
      //read the next char
      char curr = _reader.read();

      if (curr == ' ' || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
        //if end of state and we got here, then the url is valid.
        return readEnd(ReadEndState.ValidUrl);
      }

      //append the char
      _buffer.append(curr);

      //now see if we move to another state.
      if (curr == '?') {
        //if ? read query string
        return readQueryString();
      } else if (curr == '#') {
        //if # read the fragment
        return readFragment();
      }
    }

    //end of input then this url is good.
    return readEnd(ReadEndState.ValidUrl);
  }

  /**
   * The url has been read to here. Remember the url if its valid, and reset state.
   * @param state The state indicating if this url is valid. If its valid it will be added to the list of urls.
   * @return True if the url was valid.
   */
  private boolean readEnd(ReadEndState state) {
    //if the url is valid and greater then 0
    if (state == ReadEndState.ValidUrl && _buffer.length() > 0) {
      //get the last character. if its a quote, cut it off.
      int len = _buffer.length();
      if (_quoteStart && _buffer.charAt(len - 1) == '\"') {
        _buffer.delete(len - 1, len);
      }

      //Add the url to the list of good urls.
      if (_buffer.length() > 0) {
        _currentUrlMarker.setOriginalUrl(_buffer.toString());
        _urlList.add(_currentUrlMarker.createUrl());
      }
    }

    //clear out the buffer.
    _buffer.delete(0, _buffer.length());

    //reset the state of internal objects.
    _quoteStart = false;
    _hasScheme = false;
    _dontMatchIpv6 = false;
    _currentUrlMarker = new UrlMarker();

    //return true if valid.
    return state == ReadEndState.ValidUrl;
  }
}
