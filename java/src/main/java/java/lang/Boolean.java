/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.lang;

public final class Boolean implements java.io.Serializable,
                                      Comparable<Boolean>
{

    public static final Boolean TRUE = new Boolean(true);

    public static final Boolean FALSE = new Boolean(false);

    /**
     * The Class object representing the primitive type boolean.
     *
     * @since   JDK1.1
     */
    @SuppressWarnings("unchecked")
    public static final Class<Boolean> TYPE = (Class<Boolean>) Class.getPrimitiveClass("boolean");

    // 成员变量值
    private final boolean value;

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = -3665804199014368530L;

    public Boolean(boolean value) {
        this.value = value;
    }

    public Boolean(String s) {
        this(parseBoolean(s));
    }

    /**
     * 只有字符串值"true"，才是true
     * @param s
     * @return
     */
    public static boolean parseBoolean(String s) {
        return ((s != null) && s.equalsIgnoreCase("true"));
    }

    public boolean booleanValue() {
        return value;
    }

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

    public static int hashCode(boolean value) {
        return value ? 1231 : 1237;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Boolean) {
            return value == ((Boolean)obj).booleanValue();
        }
        return false;
    }

    public static boolean getBoolean(String name) {
        boolean result = false;
        try {
            result = parseBoolean(System.getProperty(name));
        } catch (IllegalArgumentException | NullPointerException e) {
        }
        return result;
    }

    public int compareTo(Boolean b) {
        return compare(this.value, b.value);
    }

    /**
     *
     * @since 1.7
     */
    public static int compare(boolean x, boolean y) {
        return (x == y) ? 0 : (x ? 1 : -1);
    }

    /**
     * 逻辑And操作
     * @since 1.8
     */
    public static boolean logicalAnd(boolean a, boolean b) {
        return a && b;
    }

    /**
     * 逻辑Or操作
     * @see java.util.function.BinaryOperator
     * @since 1.8
     */
    public static boolean logicalOr(boolean a, boolean b) {
        return a || b;
    }

    /**
     * 逻辑XOR 操作
     *
     * @since 1.8
     */
    public static boolean logicalXor(boolean a, boolean b) {
        return a ^ b;
    }
}
