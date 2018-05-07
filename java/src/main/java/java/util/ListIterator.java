/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

/**
 * 列表迭代器，在普通迭代器的基础增加如下内容：
 *  a. 允许任意方向遍历列表;
 *  b. 允许在迭代过程中修改列表
 *  c. 允许迭代器在当前列表的位置
 *
 *  ListIterator没有当前元素，它的光标位置总是位于调用previous()返回的元素和调用next()返回元素的之间
 *
 *  对于长度为n的列表，迭代器有n+1个光标的位置，如下：
 * <PRE>
 *                      Element(0)   Element(1)   Element(2)   ... Element(n-1)
 * cursor positions:  ^            ^            ^            ^                  ^
 * </PRE>
 *
 * Note that the {@link #remove} and {@link #set(Object)} methods are
 * <i>not</i> defined in terms of the cursor position;  they are defined to
 * operate on the last element returned by a call to {@link #next} or
 * {@link #previous()}.
 *
 *
 * @author  Josh Bloch
 * @see Collection
 * @see List
 * @see Iterator
 * @see Enumeration
 * @see List#listIterator()
 * @since   1.2
 */
public interface ListIterator<E> extends Iterator<E> {
    // Query Operations

    /**
     * 如果向前遍历时，如果有元素则返回true.
     *
     */
    boolean hasNext();

    /**
     * 返回列表中的下一个元素
     *  和previous遍历的方向相反
     */
    E next();

    /**
     * 如果向相反的方向遍历时，如果有元素则返回true.
     *
     */
    boolean hasPrevious();

    /**
     * 返回列表中的上一个元素
     *  和next相反
     */
    E previous();

    /**
     * 返回下一个元素的索引，即调用next方法返回元素所在的位置
     *
     */
    int nextIndex();

    /**
     * 返回上一个元素的索引，即调用previous方法返回元素所在的位置
     */
    int previousIndex();


    // Modification Operations

    /**
     * 只有在next方法和previous方法被执行后，且调用上面的方法后add方法也没有被执行，
     * 才可以执行这个方法，会删除next方法和previous返回的元素。
     *
     * 每次next方法和previous方法执行后，此方法最多被执行一次，
     *
     */
    void remove();

    /**
     * 只有在next方法和previous方法被执行后，且调用上面的方法后remove方法和add方法也没有被执行，
     * 才可以执行这个方法，使用传入参数替换的next方法和previous返回的元素。
     *
     */
    void set(E e);

    /**
     * 将指定的元素插入到列表中
     *  a. 此元素会被插入到next方法返回元素所有位置的前一个。如果插入成功，则调用previous会返回这个插入的元素
     *  b. 如果列表为空，则元素会成功列表的唯一元素
     *
     *  插入成功后，调用nextIndex和previousIndex，返回值会增加1
     *
     */
    void add(E e);
}
