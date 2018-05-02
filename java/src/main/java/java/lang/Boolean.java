/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * 封装私有类型boolean
 *
 * @author  Arthur van Hoff
 * @since   JDK1.0
 */
public final class Boolean implements java.io.Serializable,
                                      Comparable<Boolean>
{
    /**
     * 对应私有类型是true
     */
    public static final Boolean TRUE = new Boolean(true);

    /**
     * 对应私有类型是true
     */
    public static final Boolean FALSE = new Boolean(false);

    /**
     * The Class object representing the primitive type boolean.
     *
     * @since   JDK1.1
     */
    @SuppressWarnings("unchecked")
    public static final Class<Boolean> TYPE = (Class<Boolean>) Class.getPrimitiveClass("boolean");

    /**
     * 布尔值
     *
     */
    private final boolean value;

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = -3665804199014368530L;

    /**
     * 通常不使用这个构造方法
     */
    public Boolean(boolean value) {
        this.value = value;
    }

    /**
     * 只有字符串的值非空且值为true（忽略大小写），则值为true，否则为false
     *
     */
    public Boolean(String s) {
        this(parseBoolean(s));
    }

    /**
     * 只有字符串的值非空且值为true（忽略大小写），则值为true，否则为false
     * @since 1.5
     */
    public static boolean parseBoolean(String s) {
        return ((s != null) && s.equalsIgnoreCase("true"));
    }

    /**
     * 返回布尔值
     */
    public boolean booleanValue() {
        return value;
    }

    /**
     * 返回布尔值对应的Boolean对象
     */
    public static Boolean valueOf(boolean b) {
        return (b ? TRUE : FALSE);
    }

    public static Boolean valueOf(String s) {
        return parseBoolean(s) ? TRUE : FALSE;
    }

    public static String toString(boolean b) {
        return b ? "true" : "false";
    }

    public String toString() {
        return value ? "true" : "false";
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(value);
    }

    /**
     * 返回boolean对象的hash值
     * @since 1.8
     */
    public static int hashCode(boolean value) {
        return value ? 1231 : 1237;
    }

   /**
    * 只有传入参数不为null且值表示相同boolean值的Boolean对象才返回true
     */
    public boolean equals(Object obj) {
        if (obj instanceof Boolean) {
            return value == ((Boolean)obj).booleanValue();
        }
        return false;
    }

    /**
     * 只有满足如下所有条件才有：
     *  从系统变量中可以找到参数的值，且字符串的值可以转化为true
     */
    public static boolean getBoolean(String name) {
        boolean result = false;
        try {
            result = parseBoolean(System.getProperty(name));
        } catch (IllegalArgumentException | NullPointerException e) {
        }
        return result;
    }

    /**
     * Compares this {@code Boolean} instance with another.
     *
     * @param   b the {@code Boolean} instance to be compared
     * @return  zero if this object represents the same boolean value as the
     *          argument; a positive value if this object represents true
     *          and the argument represents false; and a negative value if
     *          this object represents false and the argument represents true
     * @throws  NullPointerException if the argument is {@code null}
     * @see     Comparable
     * @since  1.5
     */
    public int compareTo(Boolean b) {
        return compare(this.value, b.value);
    }

    /**
     * 比较布尔值
     * @since 1.7
     */
    public static int compare(boolean x, boolean y) {
        return (x == y) ? 0 : (x ? 1 : -1);
    }

    /**
     * 两个boolean值的逻辑And操作
     * @since 1.8
     */
    public static boolean logicalAnd(boolean a, boolean b) {
        return a && b;
    }

    /**
     * 两个boolean值的逻辑Or操作
     *
     * @since 1.8
     */
    public static boolean logicalOr(boolean a, boolean b) {
        return a || b;
    }

    /**
     * 两个boolean值的逻辑Xor操作
     *
     * @since 1.8
     */
    public static boolean logicalXor(boolean a, boolean b) {
        return a ^ b;
    }
}
