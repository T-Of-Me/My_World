/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.Cookie;
import org.eclipse.jetty.http.CookieCompliance;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class CookieCutter {
    private static final Logger LOG = Log.getLogger(CookieCutter.class);
    private final CookieCompliance _compliance;
    private Cookie[] _cookies;
    private Cookie[] _lastCookies;
    private final List<String> _fieldList = new ArrayList<String>();
    int _fields;

    public CookieCutter() {
        this(CookieCompliance.RFC6265);
    }

    public CookieCutter(CookieCompliance compliance) {
        this._compliance = compliance;
    }

    public Cookie[] getCookies() {
        if (this._cookies != null) {
            return this._cookies;
        }
        if (this._lastCookies != null && this._fields == this._fieldList.size()) {
            this._cookies = this._lastCookies;
        } else {
            this.parseFields();
        }
        this._lastCookies = this._cookies;
        return this._cookies;
    }

    public void setCookies(Cookie[] cookies) {
        this._cookies = cookies;
        this._lastCookies = null;
        this._fieldList.clear();
        this._fields = 0;
    }

    public void reset() {
        this._cookies = null;
        this._fields = 0;
    }

    public void addCookieField(String f) {
        if (f == null) {
            return;
        }
        if ((f = f.trim()).length() == 0) {
            return;
        }
        if (this._fieldList.size() > this._fields) {
            if (f.equals(this._fieldList.get(this._fields))) {
                ++this._fields;
                return;
            }
            while (this._fieldList.size() > this._fields) {
                this._fieldList.remove(this._fields);
            }
        }
        this._cookies = null;
        this._lastCookies = null;
        this._fieldList.add(this._fields++, f);
    }

    /*
     * Unable to fully structure code
     */
    protected void parseFields() {
        this._lastCookies = null;
        this._cookies = null;
        cookies = new ArrayList<Cookie>();
        version = 0;
        while (this._fieldList.size() > this._fields) {
            this._fieldList.remove(this._fields);
        }
        unquoted = null;
        for (String hdr : this._fieldList) {
            name = null;
            value = null;
            cookie = null;
            invalue = false;
            inQuoted = false;
            quoted = false;
            escaped = false;
            tokenstart = -1;
            tokenend = -1;
            length = hdr.length();
            last = length - 1;
            block18: for (i = 0; i < length; ++i) {
                block55: {
                    block56: {
                        block54: {
                            c = hdr.charAt(i);
                            if (!inQuoted) break block54;
                            if (escaped) {
                                escaped = false;
                                unquoted.append(c);
                                continue;
                            }
                            switch (c) {
                                case '\"': {
                                    inQuoted = false;
                                    if (i == last) {
                                        value = unquoted.toString();
                                        unquoted.setLength(0);
                                    } else {
                                        quoted = true;
                                        tokenstart = i;
                                        tokenend = -1;
                                    }
                                    break block55;
                                }
                                case '\\': {
                                    if (i == last) {
                                        unquoted.setLength(0);
                                        inQuoted = false;
                                        --i;
                                        break;
                                    }
                                    escaped = true;
                                    break;
                                }
                                default: {
                                    if (i == last) {
                                        unquoted.setLength(0);
                                        inQuoted = false;
                                        --i;
                                        break;
                                    }
                                    unquoted.append(c);
                                    break;
                                }
                            }
                            continue;
                        }
                        if (!invalue) break block56;
                        switch (c) {
                            case '\t': 
                            case ' ': {
                                break;
                            }
                            case ';': {
                                if (quoted) {
                                    value = unquoted.toString();
                                    unquoted.setLength(0);
                                    quoted = false;
                                } else {
                                    value = tokenstart >= 0 && tokenend >= 0 ? hdr.substring(tokenstart, tokenend + 1) : "";
                                }
                                tokenstart = -1;
                                invalue = false;
                                break;
                            }
                            case '\"': {
                                if (tokenstart >= 0) ** GOTO lbl82
                                tokenstart = i;
                                inQuoted = true;
                                if (unquoted == null) {
                                    unquoted = new StringBuilder();
                                    break;
                                }
                                break block55;
                            }
lbl82:
                            // 2 sources

                            default: {
                                if (quoted) {
                                    unquoted.append(hdr.substring(tokenstart, i));
                                    inQuoted = true;
                                    quoted = false;
                                    --i;
                                    continue block18;
                                }
                                if (tokenstart < 0) {
                                    tokenstart = i;
                                }
                                tokenend = i;
                                if (i != last) continue block18;
                                value = hdr.substring(tokenstart, tokenend + 1);
                                break;
                            }
                        }
                        break block55;
                    }
                    switch (c) {
                        case '\t': 
                        case ' ': {
                            continue block18;
                        }
                        case ';': {
                            if (quoted) {
                                name = unquoted.toString();
                                unquoted.setLength(0);
                                quoted = false;
                            } else if (tokenstart >= 0 && tokenend >= 0) {
                                name = hdr.substring(tokenstart, tokenend + 1);
                            }
                            tokenstart = -1;
                            break;
                        }
                        case '=': {
                            if (quoted) {
                                name = unquoted.toString();
                                unquoted.setLength(0);
                                quoted = false;
                            } else if (tokenstart >= 0 && tokenend >= 0) {
                                name = hdr.substring(tokenstart, tokenend + 1);
                            }
                            tokenstart = -1;
                            invalue = true;
                            break;
                        }
                        default: {
                            if (quoted) {
                                unquoted.append(hdr.substring(tokenstart, i));
                                inQuoted = true;
                                quoted = false;
                                --i;
                                continue block18;
                            }
                            if (tokenstart < 0) {
                                tokenstart = i;
                            }
                            tokenend = i;
                            if (i != last) continue block18;
                        }
                    }
                }
                if (invalue && i == last && value == null) {
                    if (quoted) {
                        value = unquoted.toString();
                        unquoted.setLength(0);
                        quoted = false;
                    } else {
                        value = tokenstart >= 0 && tokenend >= 0 ? hdr.substring(tokenstart, tokenend + 1) : "";
                    }
                }
                if (name == null || value == null) continue;
                try {
                    if (name.startsWith("$")) {
                        lowercaseName = name.toLowerCase(Locale.ENGLISH);
                        if (this._compliance != CookieCompliance.RFC6265) {
                            if ("$path".equals(lowercaseName)) {
                                if (cookie != null) {
                                    cookie.setPath(value);
                                }
                            } else if ("$domain".equals(lowercaseName)) {
                                if (cookie != null) {
                                    cookie.setDomain(value);
                                }
                            } else if ("$port".equals(lowercaseName)) {
                                if (cookie != null) {
                                    cookie.setComment("$port=" + value);
                                }
                            } else if ("$version".equals(lowercaseName)) {
                                version = Integer.parseInt(value);
                            }
                        }
                    } else {
                        cookie = new Cookie(name, value);
                        if (version > 0) {
                            cookie.setVersion(version);
                        }
                        cookies.add(cookie);
                    }
                }
                catch (Exception e) {
                    CookieCutter.LOG.debug(e);
                }
                name = null;
                value = null;
            }
        }
        this._cookies = cookies.toArray(new Cookie[cookies.size()]);
        this._lastCookies = this._cookies;
    }
}

