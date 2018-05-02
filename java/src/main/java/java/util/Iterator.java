/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Consumer;

/**
 * 集合的iterator
 *
 * @author  Josh Bloch
 * @see Collection
 * @see ListIterator
 * @see Iterable
 * @since 1.2
 */
public interface Iterator<E> {
    /**
     * 如果迭代有更多元素，则返回True
     */
    boolean hasNext();

    /**
     * 返回迭代中的下一个元素
     */
    E next();

    /**
     * 从底层集合中移除迭代器返回最后一个元素。每次调用next方法只能调用此
     * 方法一次。如果除了通过调用此方法以外的任何其他方式在进行迭代时修改了底层集合，
     * 则此方法的行为不可控。
     * *
     * 从底层集合中移除此迭代器返回的最后一个元素（可选操作）。 每次调用{@link #next}时只能调用此方法一次。
     *
     */
    default void remove() {
        throw new UnsupportedOperationException("remove");
    }

    /**
     * 为剩余的所有的元素执行指定的行为，直到所有的元素执行完毕
     *
     * @since 1.8
     */
    default void forEachRemaining(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        while (hasNext())
            action.accept(next());
    }
}
