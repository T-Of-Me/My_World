/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

public class HttpUtils {
    private static final String LSTRING_FILE = "javax.servlet.http.LocalStrings";
    private static ResourceBundle lStrings = ResourceBundle.getBundle("javax.servlet.http.LocalStrings");

    public static Hashtable<String, String[]> parseQueryString(String s) {
        String[] valArray = null;
        if (s == null) {
            throw new IllegalArgumentException();
        }
        Hashtable<String, String[]> ht = new Hashtable<String, String[]>();
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(s, "&");
        while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            int pos = pair.indexOf(61);
            if (pos == -1) {
                throw new IllegalArgumentException();
            }
            String key = HttpUtils.parseName(pair.substring(0, pos), sb);
            String val = HttpUtils.parseName(pair.substring(pos + 1, pair.length()), sb);
            if (ht.containsKey(key)) {
                String[] oldVals = ht.get(key);
                valArray = new String[oldVals.length + 1];
                for (int i = 0; i < oldVals.length; ++i) {
                    valArray[i] = oldVals[i];
                }
                valArray[oldVals.length] = val;
            } else {
                valArray = new String[]{val};
            }
            ht.put(key, valArray);
        }
        return ht;
    }

    public static Hashtable<String, String[]> parsePostData(int len, ServletInputStream in) {
        if (len <= 0) {
            return new Hashtable<String, String[]>();
        }
        if (in == null) {
            throw new IllegalArgumentException();
        }
        byte[] postedBytes = new byte[len];
        try {
            int inputLen;
            int offset = 0;
            do {
                if ((inputLen = in.read(postedBytes, offset, len - offset)) > 0) continue;
                String msg = lStrings.getString("err.io.short_read");
                throw new IllegalArgumentException(msg);
            } while (len - (offset += inputLen) > 0);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        try {
            String postedBody = new String(postedBytes, 0, len, "8859_1");
            return HttpUtils.parseQueryString(postedBody);
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private static String parseName(String s, StringBuilder sb) {
        sb.setLength(0);
        block7: for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '+': {
                    sb.append(' ');
                    continue block7;
                }
                case '%': {
                    try {
                        sb.append((char)Integer.parseInt(s.substring(i + 1, i + 3), 16));
                        i += 2;
                        continue block7;
                    }
                    catch (NumberFormatException e) {
                        throw new IllegalArgumentException();
                    }
                    catch (StringIndexOutOfBoundsException e) {
                        String rest = s.substring(i);
                        sb.append(rest);
                        if (rest.length() != 2) continue block7;
                        ++i;
                        continue block7;
                    }
                }
                default: {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    public static StringBuffer getRequestURL(HttpServletRequest req) {
        StringBuffer url = new StringBuffer();
        String scheme = req.getScheme();
        int port = req.getServerPort();
        String urlPath = req.getRequestURI();
        url.append(scheme);
        url.append("://");
        url.append(req.getServerName());
        if (scheme.equals("http") && port != 80 || scheme.equals("https") && port != 443) {
            url.append(':');
            url.append(req.getServerPort());
        }
        url.append(urlPath);
        return url;
    }
}

