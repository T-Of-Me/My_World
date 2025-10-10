/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.net.InetAddress;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.eclipse.jetty.util.StringUtil;

public class InetAddressSet
extends AbstractSet<String>
implements Set<String>,
Predicate<InetAddress> {
    private Map<String, InetPattern> _patterns = new HashMap<String, InetPattern>();

    @Override
    public boolean add(String pattern) {
        return this._patterns.put(pattern, this.newInetRange(pattern)) == null;
    }

    protected InetPattern newInetRange(String pattern) {
        if (pattern == null) {
            return null;
        }
        int slash = pattern.lastIndexOf(47);
        int dash = pattern.lastIndexOf(45);
        try {
            if (slash >= 0) {
                return new CidrInetRange(pattern, InetAddress.getByName(pattern.substring(0, slash).trim()), StringUtil.toInt(pattern, slash + 1));
            }
            if (dash >= 0) {
                return new MinMaxInetRange(pattern, InetAddress.getByName(pattern.substring(0, dash).trim()), InetAddress.getByName(pattern.substring(dash + 1).trim()));
            }
            return new SingletonInetRange(pattern, InetAddress.getByName(pattern));
        }
        catch (Exception e) {
            try {
                if (slash < 0 && dash > 0) {
                    return new LegacyInetRange(pattern);
                }
            }
            catch (Exception e2) {
                e.addSuppressed(e2);
            }
            throw new IllegalArgumentException("Bad pattern: " + pattern, e);
        }
    }

    @Override
    public boolean remove(Object pattern) {
        return this._patterns.remove(pattern) != null;
    }

    @Override
    public Iterator<String> iterator() {
        return this._patterns.keySet().iterator();
    }

    @Override
    public int size() {
        return this._patterns.size();
    }

    @Override
    public boolean test(InetAddress address) {
        if (address == null) {
            return false;
        }
        byte[] raw = address.getAddress();
        for (InetPattern pattern : this._patterns.values()) {
            if (!pattern.test(address, raw)) continue;
            return true;
        }
        return false;
    }

    static class LegacyInetRange
    extends InetPattern {
        int[] _min = new int[4];
        int[] _max = new int[4];

        public LegacyInetRange(String pattern) {
            super(pattern);
            String[] parts = pattern.split("\\.");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Bad legacy pattern: " + pattern);
            }
            for (int i = 0; i < 4; ++i) {
                String part = parts[i].trim();
                int dash = part.indexOf(45);
                if (dash < 0) {
                    this._min[i] = this._max[i] = Integer.parseInt(part);
                } else {
                    this._min[i] = dash == 0 ? 0 : StringUtil.toInt(part, 0);
                    int n = this._max[i] = dash == part.length() - 1 ? 255 : StringUtil.toInt(part, dash + 1);
                }
                if (this._min[i] >= 0 && this._min[i] <= this._max[i] && this._max[i] <= 255) continue;
                throw new IllegalArgumentException("Bad legacy pattern: " + pattern);
            }
        }

        @Override
        public boolean test(InetAddress item, byte[] raw) {
            if (raw.length != 4) {
                return false;
            }
            for (int i = 0; i < 4; ++i) {
                if ((0xFF & raw[i]) >= this._min[i] && (0xFF & raw[i]) <= this._max[i]) continue;
                return false;
            }
            return true;
        }
    }

    static class CidrInetRange
    extends InetPattern {
        final byte[] _raw;
        final int _octets;
        final int _mask;
        final int _masked;

        public CidrInetRange(String pattern, InetAddress address, int cidr) {
            super(pattern);
            this._raw = address.getAddress();
            this._octets = cidr / 8;
            this._mask = 0xFF & 255 << 8 - cidr % 8;
            int n = this._masked = this._mask == 0 ? 0 : this._raw[this._octets] & this._mask;
            if (cidr > this._raw.length * 8) {
                throw new IllegalArgumentException("CIDR too large: " + pattern);
            }
            if (this._mask != 0 && this._raw[this._octets] != this._masked) {
                throw new IllegalArgumentException("CIDR bits non zero: " + pattern);
            }
            for (int o = this._octets + (this._mask == 0 ? 0 : 1); o < this._raw.length; ++o) {
                if (this._raw[o] == 0) continue;
                throw new IllegalArgumentException("CIDR bits non zero: " + pattern);
            }
        }

        @Override
        public boolean test(InetAddress item, byte[] raw) {
            if (raw.length != this._raw.length) {
                return false;
            }
            for (int o = 0; o < this._octets; ++o) {
                if (this._raw[o] == raw[o]) continue;
                return false;
            }
            return this._mask == 0 || (raw[this._octets] & this._mask) == this._masked;
        }
    }

    static class MinMaxInetRange
    extends InetPattern {
        final int[] _min;
        final int[] _max;

        public MinMaxInetRange(String pattern, InetAddress min, InetAddress max) {
            super(pattern);
            int i;
            byte[] raw_min = min.getAddress();
            byte[] raw_max = max.getAddress();
            if (raw_min.length != raw_max.length) {
                throw new IllegalArgumentException("Cannot mix IPv4 and IPv6: " + pattern);
            }
            if (raw_min.length == 4) {
                int count = 0;
                for (char c : pattern.toCharArray()) {
                    if (c != '.') continue;
                    ++count;
                }
                if (count != 6) {
                    throw new IllegalArgumentException("Legacy pattern: " + pattern);
                }
            }
            this._min = new int[raw_min.length];
            this._max = new int[raw_min.length];
            for (i = 0; i < this._min.length; ++i) {
                this._min[i] = 0xFF & raw_min[i];
                this._max[i] = 0xFF & raw_max[i];
            }
            for (i = 0; i < this._min.length; ++i) {
                if (this._min[i] > this._max[i]) {
                    throw new IllegalArgumentException("min is greater than max: " + pattern);
                }
                if (this._min[i] < this._max[i]) break;
            }
        }

        @Override
        public boolean test(InetAddress item, byte[] raw) {
            if (raw.length != this._min.length) {
                return false;
            }
            boolean min_ok = false;
            boolean max_ok = false;
            for (int i = 0; i < this._min.length; ++i) {
                int r = 0xFF & raw[i];
                if (!min_ok) {
                    if (r < this._min[i]) {
                        return false;
                    }
                    if (r > this._min[i]) {
                        min_ok = true;
                    }
                }
                if (!max_ok) {
                    if (r > this._max[i]) {
                        return false;
                    }
                    if (r < this._max[i]) {
                        max_ok = true;
                    }
                }
                if (min_ok && max_ok) break;
            }
            return true;
        }
    }

    static class SingletonInetRange
    extends InetPattern {
        final InetAddress _address;

        public SingletonInetRange(String pattern, InetAddress address) {
            super(pattern);
            this._address = address;
        }

        @Override
        public boolean test(InetAddress address, byte[] raw) {
            return this._address.equals(address);
        }
    }

    static abstract class InetPattern {
        final String _pattern;

        InetPattern(String pattern) {
            this._pattern = pattern;
        }

        abstract boolean test(InetAddress var1, byte[] var2);

        public String toString() {
            return this._pattern;
        }
    }
}

