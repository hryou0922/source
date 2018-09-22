/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package java.net;

import java.io.*;

/**
 * 本类实现了 IP Socket Address ( (IP address + port number) 或(hostname + port number))
 * 当使用hostname时，会尝试解析hostname。如果解析失败，会说unresolved。但是有些环境下，向通过代理连接，会依然被使用
 *
 * 此类为不可变类
 *
 * 通配符 是一个特殊的本地IP地址。 它通常意味着“任何”地址，并且只能用于bind操作。
 *
 * @see Socket
 * @see ServerSocket
 * @since 1.4
 */
public class InetSocketAddress
    extends SocketAddress
{
    // Private implementation class pointed to by all public methods.
    // 私有实现类
    private static class InetSocketAddressHolder {
        // The hostname of the Socket Address
        private String hostname;
        // The IP address of the Socket Address
        private InetAddress addr;
        // The port number of the Socket Address
        private int port;

        private InetSocketAddressHolder(String hostname, InetAddress addr, int port) {
            this.hostname = hostname;
            this.addr = addr;
            this.port = port;
        }

        private int getPort() {
            return port;
        }

        private InetAddress getAddress() {
            return addr;
        }

        private String getHostName() {
            if (hostname != null)
                return hostname;
            if (addr != null)
                return addr.getHostName();
            return null;
        }

        private String getHostString() {
            if (hostname != null)
                return hostname;
            if (addr != null) {
                if (addr.holder().getHostName() != null)
                    return addr.holder().getHostName();
                else
                    return addr.getHostAddress();
            }
            return null;
        }

        private boolean isUnresolved() {
            return addr == null;
        }

        @Override
        public String toString() {
            if (isUnresolved()) {
                return hostname + ":" + port;
            } else {
                return addr.toString() + ":" + port;
            }
        }

        @Override
        public final boolean equals(Object obj) {
            if (obj == null || !(obj instanceof InetSocketAddressHolder))
                return false;
            InetSocketAddressHolder that = (InetSocketAddressHolder)obj;
            boolean sameIP;
            if (addr != null)
                sameIP = addr.equals(that.addr);
            else if (hostname != null)
                sameIP = (that.addr == null) &&
                    hostname.equalsIgnoreCase(that.hostname);
            else
                sameIP = (that.addr == null) && (that.hostname == null);
            return sameIP && (port == that.port);
        }

        @Override
        public final int hashCode() {
            if (addr != null)
                return addr.hashCode() + port;
            if (hostname != null)
                return hostname.toLowerCase().hashCode() + port;
            return port;
        }
    }

    private final transient InetSocketAddressHolder holder;

    private static final long serialVersionUID = 5076001401234631237L;

    private static int checkPort(int port) {
        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException("port out of range:" + port);
        return port;
    }

    private static String checkHost(String hostname) {
        if (hostname == null)
            throw new IllegalArgumentException("hostname can't be null");
        return hostname;
    }



    /**
     * 根据IP地址和端口创建一个套接字
     *
     * 合法的端口从0到65535
     * 但是当设置端口为0时，则系统在bind操作会从空闲的端口中选择一个临时端口进行绑定
     *
     * 当addr设置为null时，系统会设置此值为通配符地址（如 0.0.0.0 或 ::0）
     *
     * @param   addr    The IP address
     * @param   port    The port number
     * @throws IllegalArgumentException if the port parameter is outside the specified
     * range of valid port values.
     */
    public InetSocketAddress(InetAddress addr, int port) {
        holder = new InetSocketAddressHolder(
                        null,
                        addr == null ? InetAddress.anyLocalAddress() : addr, // 如果IP为null，则会选择一个通配符地址
                        checkPort(port)); // 检查端
    }

    public InetSocketAddress(int port) {
        this(InetAddress.anyLocalAddress(), port);
    }

    public InetSocketAddress(String hostname, int port) {
        checkHost(hostname);
        InetAddress addr = null;
        String host = null;
        try {
            // An attempt will be made to resolve the hostname into an InetAddress.
            // If that attempt fails, the address will be flagged as <I>unresolved</I>.
            addr = InetAddress.getByName(hostname);
        } catch(UnknownHostException e) {
            host = hostname;
        }
        holder = new InetSocketAddressHolder(host, addr, checkPort(port));
    }

    // private constructor for creating unresolved instances
    private InetSocketAddress(int port, String hostname) {
        holder = new InetSocketAddressHolder(hostname, null, port);
    }

    /**
     * 从主机名和端口号创建未解析的套接字地址。
     *
     * @since 1.5
     */
    public static InetSocketAddress createUnresolved(String host, int port) {
        return new InetSocketAddress(checkPort(port), checkHost(host));
    }

    /**
     * @serialField hostname String
     * @serialField addr InetAddress
     * @serialField port int
     */
    private static final ObjectStreamField[] serialPersistentFields = {
         new ObjectStreamField("hostname", String.class),
         new ObjectStreamField("addr", InetAddress.class),
         new ObjectStreamField("port", int.class)};

    private void writeObject(ObjectOutputStream out)
        throws IOException
    {
        // Don't call defaultWriteObject()
         ObjectOutputStream.PutField pfields = out.putFields();
         pfields.put("hostname", holder.hostname);
         pfields.put("addr", holder.addr);
         pfields.put("port", holder.port);
         out.writeFields();
     }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        // Don't call defaultReadObject()
        ObjectInputStream.GetField oisFields = in.readFields();
        final String oisHostname = (String)oisFields.get("hostname", null);
        final InetAddress oisAddr = (InetAddress)oisFields.get("addr", null);
        final int oisPort = oisFields.get("port", -1);

        // Check that our invariants are satisfied
        checkPort(oisPort);
        if (oisHostname == null && oisAddr == null)
            throw new InvalidObjectException("hostname and addr " +
                                             "can't both be null");

        InetSocketAddressHolder h = new InetSocketAddressHolder(oisHostname,
                                                                oisAddr,
                                                                oisPort);
        UNSAFE.putObject(this, FIELDS_OFFSET, h);
    }

    private void readObjectNoData()
        throws ObjectStreamException
    {
        throw new InvalidObjectException("Stream data required");
    }

    private static final long FIELDS_OFFSET;
    private static final sun.misc.Unsafe UNSAFE;
    static {
        try {
            sun.misc.Unsafe unsafe = sun.misc.Unsafe.getUnsafe();
            FIELDS_OFFSET = unsafe.objectFieldOffset(
                    InetSocketAddress.class.getDeclaredField("holder"));
            UNSAFE = unsafe;
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    // 返回端口
    public final int getPort() {
        return holder.getPort();
    }

    // 返回InetAddress，如果host无法被解析，则返回null
    public final InetAddress getAddress() {
        return holder.getAddress();
    }

    /**
     * Gets the {@code hostname}.
     * Note: This method may trigger a name service reverse lookup if the
     * address was created with a literal IP address.
     *
     * @return  the hostname part of the address.
     */
    public final String getHostName() {
        return holder.getHostName();
    }

    /**
     * Returns the hostname, or the String form of the address if it
     * doesn't have a hostname (it was created using a literal).
     * This has the benefit of <b>not</b> attempting a reverse lookup.
     *
     * @return the hostname, or String representation of the address.
     * @since 1.7
     */
    public final String getHostString() {
        return holder.getHostString();
    }

    /**
     * Checks whether the address has been resolved or not.
     *
     * @return {@code true} if the hostname couldn't be resolved into
     *          an {@code InetAddress}.
     */
    public final boolean isUnresolved() {
        return holder.isUnresolved();
    }

    /**
     * Constructs a string representation of this InetSocketAddress.
     * This String is constructed by calling toString() on the InetAddress
     * and concatenating the port number (with a colon). If the address
     * is unresolved then the part before the colon will only contain the hostname.
     *
     * @return  a string representation of this object.
     */
    @Override
    public String toString() {
        return holder.toString();
    }

    /**
     * Compares this object against the specified object.
     * The result is {@code true} if and only if the argument is
     * not {@code null} and it represents the same address as
     * this object.
     * <p>
     * Two instances of {@code InetSocketAddress} represent the same
     * address if both the InetAddresses (or hostnames if it is unresolved) and port
     * numbers are equal.
     * If both addresses are unresolved, then the hostname and the port number
     * are compared.
     *
     * Note: Hostnames are case insensitive. e.g. "FooBar" and "foobar" are
     * considered equal.
     *
     * @param   obj   the object to compare against.
     * @return  {@code true} if the objects are the same;
     *          {@code false} otherwise.
     * @see InetAddress#equals(Object)
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj == null || !(obj instanceof InetSocketAddress))
            return false;
        return holder.equals(((InetSocketAddress) obj).holder);
    }

    /**
     * Returns a hashcode for this socket address.
     *
     * @return  a hash code value for this socket address.
     */
    @Override
    public final int hashCode() {
        return holder.hashCode();
    }
}
