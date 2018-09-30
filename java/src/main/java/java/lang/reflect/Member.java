/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.reflect;

/**
 * 此接口标识 单个成员（字段或方法）或构造函数的信息
 *
 * @see Class
 * @see Field
 * @see Method
 * @see Constructor
 *
 * @author Nakul Saraiya
 */
public
interface Member {

    // 标识类或接口的所有公共成员的集合(只含public)，包括继承的成员。
    public static final int PUBLIC = 0;

    // 标识当前类或接口的已声明成员集(含public,private,protected,pacakage)。 不包括继承的成员。
    public static final int DECLARED = 1;

    /**
     * Returns the Class object representing the class or interface
     * that declares the member or constructor represented by this Member.
     *
     * 好像是此类内部定义的类??
     *
     * @return an object representing the declaring class of the
     * underlying member
     */
    public Class<?> getDeclaringClass();

    // 返回  underlying member 的简单名称
    public String getName();

    // 以整数形式返回此Member表示的成员或构造函数的Java语言修饰符。 应使用Modifier类来解码整数中的修饰符。
    public int getModifiers();

    /**
     * 由java编译器生成的（除了像默认构造函数这一类的）方法或者类,如果是则返回true，否则false
     *
     * 关于Synthetic解释看这篇文章：
     *  Java 中冷门的 synthetic 关键字原理解读： https://www.cnblogs.com/bethunebtj/p/7761596.html
     *      编译器通过生成一些在源代码中不存在的synthetic方法和类的方式，实现了对private级别的字段和类的访问，从而绕开了语言限制，这可以算是一种trick
     *
     * Returns {@code true} if this member was introduced by
     * the compiler; returns {@code false} otherwise.
     *
     * @return true if and only if this member was introduced by
     * the compiler.
     * @jls 13.1 The Form of a Binary
     * @since 1.5
     */
    public boolean isSynthetic();
}
