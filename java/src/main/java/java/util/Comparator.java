/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Serializable;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.ToDoubleFunction;
import java.util.Comparators;

/**
 * 此类提供比较功能。
 * Comparators可以传递给一个排序方法（如 Collections#sort(List,Comparator)或Arrays#sort(Object[],Comparator)) 做精确控制
 * Comparators也可用于控制某些数据结构的顺序（如 SortedSet或SortedMap）
 * 或者为没有Comparable的对象集合提供排序功能
 *
 * comparator排序后的集合S中，S中每个元素如果当且仅当c.compare(e1, e2)==0，则e1.equals(e2)
 *
 * 要小心处理集合中满足以下元素：
 *  如果a.equal(b)但是c.compare(a, b) != 0)
 *
 * Caution should be exercised when using a comparator capable of imposing an
 * ordering inconsistent with equals to order a sorted set (or sorted map).
 * Suppose a sorted set (or sorted map) with an explicit comparator <tt>c</tt>
 * is used with elements (or keys) drawn from a set <tt>S</tt>.  If the
 * ordering imposed by <tt>c</tt> on <tt>S</tt> is inconsistent with equals,
 * the sorted set (or sorted map) will behave "strangely."  In particular the
 * sorted set (or sorted map) will violate the general contract for set (or
 * map), which is defined in terms of <tt>equals</tt>.<p>
 *
 * For example, suppose one adds two elements {@code a} and {@code b} such that
 * {@code (a.equals(b) && c.compare(a, b) != 0)}
 * to an empty {@code TreeSet} with comparator {@code c}.
 * The second {@code add} operation will return
 * true (and the size of the tree set will increase) because {@code a} and
 * {@code b} are not equivalent from the tree set's perspective, even though
 * this is contrary to the specification of the
 * {@link Set#add Set.add} method.<p>
 *
 *
 *  注意：通常建议comparators的类要实现java.io.Serializable，
 *  因为它们可能被用作序列化数据结构中的排序方法（如TreeSet，、TreeMap）
 *  为了使数据结构成功序列化，comparator必须实现 Serializable
 *
 * For the mathematically inclined, the <i>relation</i> that defines the
 * <i>imposed ordering</i> that a given comparator <tt>c</tt> imposes on a
 * given set of objects <tt>S</tt> is:<pre>
 *       {(x, y) such that c.compare(x, y) &lt;= 0}.
 * </pre> The <i>quotient</i> for this total order is:<pre>
 *       {(x, y) such that c.compare(x, y) == 0}.
 * </pre>
 *
 * It follows immediately from the contract for <tt>compare</tt> that the
 * quotient is an <i>equivalence relation</i> on <tt>S</tt>, and that the
 * imposed ordering is a <i>total order</i> on <tt>S</tt>.  When we say that
 * the ordering imposed by <tt>c</tt> on <tt>S</tt> is <i>consistent with
 * equals</i>, we mean that the quotient for the ordering is the equivalence
 * relation defined by the objects' {@link Object#equals(Object)
 * equals(Object)} method(s):<pre>
 *     {(x, y) such that x.equals(y)}. </pre>
 *
 * <p>Unlike {@code Comparable}, a comparator may optionally permit
 * comparison of null arguments, while maintaining the requirements for
 * an equivalence relation.
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see Comparable
 * @see Serializable
 * @since 1.2
 */
@FunctionalInterface
public interface Comparator<T> {
    /**
     * 比较两个参数，返回结果如下：
     *  负数：第一个参数 < 第二个参数
     *  0：第一个参数 = 第二个参数
     *  正数：第一个参数 > 第二个参数
     *
     */
    int compare(T o1, T o2);

    /**
     * 如果返回true，满足如下条件：
     *  a. 传入参数必须是comparator
     *  b. 传入参数与这个比较器相同的顺序。即如果comp1.equals(comp2)，
     *      则对于集合中每个元素o1,o2， sgn(comp1.compare(o1,o2))==sgn(comp2.compare(o1, o2))
     *
     * @see Object#equals(Object)
     * @see Object#hashCode()
     */
    boolean equals(Object obj);

    /**
     * 返回和此类相反顺序的comparator
     *
     * @since 1.8
     */
    default Comparator<T> reversed() {
        return Collections.reverseOrder(this);
    }

    /**
     * Returns a lexicographic-order comparator with another comparator.
     * If this {@code Comparator} considers two elements equal, i.e.
     * {@code compare(a, b) == 0}, {@code other} is used to determine the order.
     *
     * <p>The returned comparator is serializable if the specified comparator
     * is also serializable.
     *
     * @apiNote
     * For example, to sort a collection of {@code String} based on the length
     * and then case-insensitive natural ordering, the comparator can be
     * composed using following code,
     *
     * <pre>{@code
     *     Comparator<String> cmp = Comparator.comparingInt(String::length)
     *             .thenComparing(String.CASE_INSENSITIVE_ORDER);
     * }</pre>
     *
     * @param  other the other comparator to be used when this comparator
     *         compares two objects that are equal.
     * @return a lexicographic-order comparator composed of this and then the
     *         other comparator
     * @throws NullPointerException if the argument is null.
     * @since 1.8
     */
    default Comparator<T> thenComparing(Comparator<? super T> other) {
        Objects.requireNonNull(other);
        return (Comparator<T> & Serializable) (c1, c2) -> {
            int res = compare(c1, c2);
            return (res != 0) ? res : other.compare(c1, c2);
        };
    }

    /**
     * Returns a lexicographic-order comparator with a function that
     * extracts a key to be compared with the given {@code Comparator}.
     *
     * @implSpec This default implementation behaves as if {@code
     *           thenComparing(comparing(keyExtractor, cmp))}.
     *
     * @param  <U>  the type of the sort key
     * @param  keyExtractor the function used to extract the sort key
     * @param  keyComparator the {@code Comparator} used to compare the sort key
     * @return a lexicographic-order comparator composed of this comparator
     *         and then comparing on the key extracted by the keyExtractor function
     * @throws NullPointerException if either argument is null.
     * @see #comparing(Function, Comparator)
     * @see #thenComparing(Comparator)
     * @since 1.8
     */
    default <U> Comparator<T> thenComparing(
            Function<? super T, ? extends U> keyExtractor,
            Comparator<? super U> keyComparator)
    {
        return thenComparing(comparing(keyExtractor, keyComparator));
    }

    /**
     * Returns a lexicographic-order comparator with a function that
     * extracts a {@code Comparable} sort key.
     *
     * @implSpec This default implementation behaves as if {@code
     *           thenComparing(comparing(keyExtractor))}.
     *
     * @param  <U>  the type of the {@link Comparable} sort key
     * @param  keyExtractor the function used to extract the {@link
     *         Comparable} sort key
     * @return a lexicographic-order comparator composed of this and then the
     *         {@link Comparable} sort key.
     * @throws NullPointerException if the argument is null.
     * @see #comparing(Function)
     * @see #thenComparing(Comparator)
     * @since 1.8
     */
    default <U extends Comparable<? super U>> Comparator<T> thenComparing(
            Function<? super T, ? extends U> keyExtractor)
    {
        return thenComparing(comparing(keyExtractor));
    }

    /**
     * Returns a lexicographic-order comparator with a function that
     * extracts a {@code int} sort key.
     *
     * @implSpec This default implementation behaves as if {@code
     *           thenComparing(comparingInt(keyExtractor))}.
     *
     * @param  keyExtractor the function used to extract the integer sort key
     * @return a lexicographic-order comparator composed of this and then the
     *         {@code int} sort key
     * @throws NullPointerException if the argument is null.
     * @see #comparingInt(ToIntFunction)
     * @see #thenComparing(Comparator)
     * @since 1.8
     */
    default Comparator<T> thenComparingInt(ToIntFunction<? super T> keyExtractor) {
        return thenComparing(comparingInt(keyExtractor));
    }

    /**
     * Returns a lexicographic-order comparator with a function that
     * extracts a {@code long} sort key.
     *
     * @implSpec This default implementation behaves as if {@code
     *           thenComparing(comparingLong(keyExtractor))}.
     *
     * @param  keyExtractor the function used to extract the long sort key
     * @return a lexicographic-order comparator composed of this and then the
     *         {@code long} sort key
     * @throws NullPointerException if the argument is null.
     * @see #comparingLong(ToLongFunction)
     * @see #thenComparing(Comparator)
     * @since 1.8
     */
    default Comparator<T> thenComparingLong(ToLongFunction<? super T> keyExtractor) {
        return thenComparing(comparingLong(keyExtractor));
    }

    /**
     * Returns a lexicographic-order comparator with a function that
     * extracts a {@code double} sort key.
     *
     * @implSpec This default implementation behaves as if {@code
     *           thenComparing(comparingDouble(keyExtractor))}.
     *
     * @param  keyExtractor the function used to extract the double sort key
     * @return a lexicographic-order comparator composed of this and then the
     *         {@code double} sort key
     * @throws NullPointerException if the argument is null.
     * @see #comparingDouble(ToDoubleFunction)
     * @see #thenComparing(Comparator)
     * @since 1.8
     */
    default Comparator<T> thenComparingDouble(ToDoubleFunction<? super T> keyExtractor) {
        return thenComparing(comparingDouble(keyExtractor));
    }

    /**
     * Returns a comparator that imposes the reverse of the <em>natural
     * ordering</em>.
     *
     * <p>The returned comparator is serializable and throws {@link
     * NullPointerException} when comparing {@code null}.
     *
     * @param  <T> the {@link Comparable} type of element to be compared
     * @return a comparator that imposes the reverse of the <i>natural
     *         ordering</i> on {@code Comparable} objects.
     * @see Comparable
     * @since 1.8
     */
    public static <T extends Comparable<? super T>> Comparator<T> reverseOrder() {
        return Collections.reverseOrder();
    }

    /**
     * Returns a comparator that compares {@link Comparable} objects in natural
     * order.
     *
     * <p>The returned comparator is serializable and throws {@link
     * NullPointerException} when comparing {@code null}.
     *
     * @param  <T> the {@link Comparable} type of element to be compared
     * @return a comparator that imposes the <i>natural ordering</i> on {@code
     *         Comparable} objects.
     * @see Comparable
     * @since 1.8
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<? super T>> Comparator<T> naturalOrder() {
        return (Comparator<T>) Comparators.NaturalOrderComparator.INSTANCE;
    }

    /**
     * Returns a null-friendly comparator that considers {@code null} to be
     * less than non-null. When both are {@code null}, they are considered
     * equal. If both are non-null, the specified {@code Comparator} is used
     * to determine the order. If the specified comparator is {@code null},
     * then the returned comparator considers all non-null values to be equal.
     *
     * <p>The returned comparator is serializable if the specified comparator
     * is serializable.
     *
     * @param  <T> the type of the elements to be compared
     * @param  comparator a {@code Comparator} for comparing non-null values
     * @return a comparator that considers {@code null} to be less than
     *         non-null, and compares non-null objects with the supplied
     *         {@code Comparator}.
     * @since 1.8
     */
    public static <T> Comparator<T> nullsFirst(Comparator<? super T> comparator) {
        return new Comparators.NullComparator<>(true, comparator);
    }

    /**
     * Returns a null-friendly comparator that considers {@code null} to be
     * greater than non-null. When both are {@code null}, they are considered
     * equal. If both are non-null, the specified {@code Comparator} is used
     * to determine the order. If the specified comparator is {@code null},
     * then the returned comparator considers all non-null values to be equal.
     *
     * <p>The returned comparator is serializable if the specified comparator
     * is serializable.
     *
     * @param  <T> the type of the elements to be compared
     * @param  comparator a {@code Comparator} for comparing non-null values
     * @return a comparator that considers {@code null} to be greater than
     *         non-null, and compares non-null objects with the supplied
     *         {@code Comparator}.
     * @since 1.8
     */
    public static <T> Comparator<T> nullsLast(Comparator<? super T> comparator) {
        return new Comparators.NullComparator<>(false, comparator);
    }

    /**
     * Accepts a function that extracts a sort key from a type {@code T}, and
     * returns a {@code Comparator<T>} that compares by that sort key using
     * the specified {@link Comparator}.
      *
     * <p>The returned comparator is serializable if the specified function
     * and comparator are both serializable.
     *
     * @apiNote
     * For example, to obtain a {@code Comparator} that compares {@code
     * Person} objects by their last name ignoring case differences,
     *
     * <pre>{@code
     *     Comparator<Person> cmp = Comparator.comparing(
     *             Person::getLastName,
     *             String.CASE_INSENSITIVE_ORDER);
     * }</pre>
     *
     * @param  <T> the type of element to be compared
     * @param  <U> the type of the sort key
     * @param  keyExtractor the function used to extract the sort key
     * @param  keyComparator the {@code Comparator} used to compare the sort key
     * @return a comparator that compares by an extracted key using the
     *         specified {@code Comparator}
     * @throws NullPointerException if either argument is null
     * @since 1.8
     */
    public static <T, U> Comparator<T> comparing(
            Function<? super T, ? extends U> keyExtractor,
            Comparator<? super U> keyComparator)
    {
        Objects.requireNonNull(keyExtractor);
        Objects.requireNonNull(keyComparator);
        return (Comparator<T> & Serializable)
            (c1, c2) -> keyComparator.compare(keyExtractor.apply(c1),
                                              keyExtractor.apply(c2));
    }

    /**
     * Accepts a function that extracts a {@link Comparable
     * Comparable} sort key from a type {@code T}, and returns a {@code
     * Comparator<T>} that compares by that sort key.
     *
     * <p>The returned comparator is serializable if the specified function
     * is also serializable.
     *
     * @apiNote
     * For example, to obtain a {@code Comparator} that compares {@code
     * Person} objects by their last name,
     *
     * <pre>{@code
     *     Comparator<Person> byLastName = Comparator.comparing(Person::getLastName);
     * }</pre>
     *
     * @param  <T> the type of element to be compared
     * @param  <U> the type of the {@code Comparable} sort key
     * @param  keyExtractor the function used to extract the {@link
     *         Comparable} sort key
     * @return a comparator that compares by an extracted key
     * @throws NullPointerException if the argument is null
     * @since 1.8
     */
    public static <T, U extends Comparable<? super U>> Comparator<T> comparing(
            Function<? super T, ? extends U> keyExtractor)
    {
        Objects.requireNonNull(keyExtractor);
        return (Comparator<T> & Serializable)
            (c1, c2) -> keyExtractor.apply(c1).compareTo(keyExtractor.apply(c2));
    }

    /**
     * Accepts a function that extracts an {@code int} sort key from a type
     * {@code T}, and returns a {@code Comparator<T>} that compares by that
     * sort key.
     *
     * <p>The returned comparator is serializable if the specified function
     * is also serializable.
     *
     * @param  <T> the type of element to be compared
     * @param  keyExtractor the function used to extract the integer sort key
     * @return a comparator that compares by an extracted key
     * @see #comparing(Function)
     * @throws NullPointerException if the argument is null
     * @since 1.8
     */
    public static <T> Comparator<T> comparingInt(ToIntFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (Comparator<T> & Serializable)
            (c1, c2) -> Integer.compare(keyExtractor.applyAsInt(c1), keyExtractor.applyAsInt(c2));
    }

    /**
     * Accepts a function that extracts a {@code long} sort key from a type
     * {@code T}, and returns a {@code Comparator<T>} that compares by that
     * sort key.
     *
     * <p>The returned comparator is serializable if the specified function is
     * also serializable.
     *
     * @param  <T> the type of element to be compared
     * @param  keyExtractor the function used to extract the long sort key
     * @return a comparator that compares by an extracted key
     * @see #comparing(Function)
     * @throws NullPointerException if the argument is null
     * @since 1.8
     */
    public static <T> Comparator<T> comparingLong(ToLongFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (Comparator<T> & Serializable)
            (c1, c2) -> Long.compare(keyExtractor.applyAsLong(c1), keyExtractor.applyAsLong(c2));
    }

    /**
     * Accepts a function that extracts a {@code double} sort key from a type
     * {@code T}, and returns a {@code Comparator<T>} that compares by that
     * sort key.
     *
     * <p>The returned comparator is serializable if the specified function
     * is also serializable.
     *
     * @param  <T> the type of element to be compared
     * @param  keyExtractor the function used to extract the double sort key
     * @return a comparator that compares by an extracted key
     * @see #comparing(Function)
     * @throws NullPointerException if the argument is null
     * @since 1.8
     */
    public static<T> Comparator<T> comparingDouble(ToDoubleFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (Comparator<T> & Serializable)
            (c1, c2) -> Double.compare(keyExtractor.applyAsDouble(c1), keyExtractor.applyAsDouble(c2));
    }
}
