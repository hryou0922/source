/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.util.function;

import java.util.Objects;

/**
 *  函数式接口
 *  接受一个输入参数，返回一个布尔值
 *
 * @since 1.8
 */
@FunctionalInterface
public interface Predicate<T> {

    /**
     * 接受一个输入参数，返回一个布尔值
     *
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    boolean test(T t);

    /**
     * 本Predicate和传入的Predicate执行and操作。
     * 在执行过程中抛出的异常会返回给调用者
     *
     */
    default Predicate<T> and(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        // 下面的代码等价于
//        return new Predicate<T>() {
//            @Override
//            public boolean test(T t) {
//                return test(t) && other.test(t);
//            }
//        };
        return (t) -> test(t) && other.test(t);
    }

    /**
     * 对本对象的test方法执行非操作
     */
    default Predicate<T> negate() {
        return (t) -> !test(t);
    }

    /**
     * 本Predicate和传入的Predicate执行or操作。
     * @throws NullPointerException if other is null
     */
    default Predicate<T> or(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) || other.test(t);
    }

    /**
     * 本Predicate和传入的Predicate执行equal操作。
     */
    static <T> Predicate<T> isEqual(Object targetRef) {
        return (null == targetRef)
                ? Objects::isNull
                : object -> targetRef.equals(object);
    }
}
