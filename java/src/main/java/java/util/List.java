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

import java.util.function.UnaryOperator;

/**
 * 有顺序的集合
 *  允许有重复元素
 *  List会有特殊iterator，即ListIterator。它允许元素插入、 覆盖、双向访问和普通Iterator方法.
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see Collection
 * @see Set
 * @see ArrayList
 * @see LinkedList
 * @see Vector
 * @see Arrays#asList(Object[])
 * @see Collections#nCopies(int, Object)
 * @see Collections#EMPTY_LIST
 * @see AbstractList
 * @see AbstractSequentialList
 * @since 1.2
 */

public interface List<E> extends Collection<E> {
    // Query Operations

    /**
     * 返回集合大小
     */
    int size();

    /**
     * 如果集合中没有元素，则返回true
     *
     */
    boolean isEmpty();

    /**
     * 如果集合中至少包含指定的元素，则返回true
     *
     */
    boolean contains(Object o);

    /**
     * 返回Iterator
     */
    Iterator<E> iterator();

    /**
     * @see Collection#toArray()
     */
    Object[] toArray();

    /**
     * @see Collection#toArray(T[])
     */
    <T> T[] toArray(T[] a);


    // Modification Operations

    /**
     * @see Collection#add(E)
     */
    boolean add(E e);

    /**
     * 删除第一个符合要求的元素。
     * 如果集合发生变化，则返回true
     */
    boolean remove(Object o);


    // Bulk Modification Operations

    /**
     *  @see Collection#containsAll(Collection)
     */
    boolean containsAll(Collection<?> c);

    /**
     *  @see Collection#addAll(Collection)
     */
    boolean addAll(Collection<? extends E> c);

    /**
     * 在index索引后面开始插入集合中的数据
     *
     * @return <tt>true</tt> if this list changed as a result of the call
     */
    boolean addAll(int index, Collection<? extends E> c);

    /**
     * 根据传入的特定集合从集合中删除元素
     *
     * @return <tt>true</tt> if this list changed as a result of the call
     */
    boolean removeAll(Collection<?> c);

    /**
     * 集合中只保留传入的特定集合中的元素，即从集合中删除不在传入集合中的元素
     *
     * @return <tt>true</tt> if this list changed as a result of the call
     */
    boolean retainAll(Collection<?> c);

    /**
     * 使用UnaryOperator操作的结果替换集合中每个元素
     *  如果对任一函数的求值引发异常，则将其传递给组合函数的调用者
     *
     * If the list's list-iterator does not support the {@code set} operation
     * then an {@code UnsupportedOperationException} will be thrown when
     * replacing the first element.
     *
     * @since 1.8
     */
    default void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final ListIterator<E> li = this.listIterator();
        while (li.hasNext()) {
            li.set(operator.apply(li.next()));
        }
    }

    /**
     * Sorts this list according to the order induced by the specified
     * {@link Comparator}.
     *
     * <p>All elements in this list must be <i>mutually comparable</i> using the
     * specified comparator (that is, {@code c.compare(e1, e2)} must not throw
     * a {@code ClassCastException} for any elements {@code e1} and {@code e2}
     * in the list).
     *
     * <p>If the specified comparator is {@code null} then all elements in this
     * list must implement the {@link Comparable} interface and the elements'
     * {@linkplain Comparable natural ordering} should be used.
     *
     * <p>This list must be modifiable, but need not be resizable.
     *
     * @implSpec
     * The default implementation obtains an array containing all elements in
     * this list, sorts the array, and iterates over this list resetting each
     * element from the corresponding position in the array. (This avoids the
     * n<sup>2</sup> log(n) performance that would result from attempting
     * to sort a linked list in place.)
     *
     * @implNote
     * This implementation is a stable, adaptive, iterative mergesort that
     * requires far fewer than n lg(n) comparisons when the input array is
     * partially sorted, while offering the performance of a traditional
     * mergesort when the input array is randomly ordered.  If the input array
     * is nearly sorted, the implementation requires approximately n
     * comparisons.  Temporary storage requirements vary from a small constant
     * for nearly sorted input arrays to n/2 object references for randomly
     * ordered input arrays.
     *
     * <p>The implementation takes equal advantage of ascending and
     * descending order in its input array, and can take advantage of
     * ascending and descending order in different parts of the same
     * input array.  It is well-suited to merging two or more sorted arrays:
     * simply concatenate the arrays and sort the resulting array.
     *
     * <p>The implementation was adapted from Tim Peters's list sort for Python
     * (<a href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">
     * TimSort</a>).  It uses techniques from Peter McIlroy's "Optimistic
     * Sorting and Information Theoretic Complexity", in Proceedings of the
     * Fourth Annual ACM-SIAM Symposium on Discrete Algorithms, pp 467-474,
     * January 1993.
     *
     * @param c the {@code Comparator} used to compare list elements.
     *          A {@code null} value indicates that the elements'
     *          {@linkplain Comparable natural ordering} should be used
     * @throws ClassCastException if the list contains elements that are not
     *         <i>mutually comparable</i> using the specified comparator
     * @throws UnsupportedOperationException if the list's list-iterator does
     *         not support the {@code set} operation
     * @throws IllegalArgumentException
     *         (<a href="Collection.html#optional-restrictions">optional</a>)
     *         if the comparator is found to violate the {@link Comparator}
     *         contract
     * @since 1.8
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    default void sort(Comparator<? super E> c) {
        Object[] a = this.toArray();
        Arrays.sort(a, (Comparator) c);
        ListIterator<E> i = this.listIterator();
        for (Object e : a) {
            i.next();
            i.set((E) e);
        }
    }

    /**
     * 从集合中删除所有的元素
     *
     */
    void clear();


    // Comparison and hashing

    /**
     * 满足如下条件，则返回true:
     *  传入的元素必须是list，要比较的两个列表的大小相同且两个列表中所有相应的元素对均等于
     *  即：两个列表拥有相同的元素且顺序相同
     *
     */
    boolean equals(Object o);

    /**
     * 返回列表的hash值
     *  如果两个列表相等，则必须保证两个列表的hash值必须相同
     *
     */
    int hashCode();


    // Positional Access Operations

    /**
     * 返回列表中指定位置的元素
     */
    E get(int index);

    /**
     * 用指定的元素替换此列表中指定位置的元素
     *
     * @return the element previously at the specified position
     *
     */
    E set(int index, E element);

    /**
     * 将指定的元素插入此列表中的指定位置
     *
     */
    void add(int index, E element);

    /**
     * 删除此列表中指定位置的元素
     *
     * @return the element previously at the specified position
     */
    E remove(int index);


    // Search Operations

    /**
     * 返回特定元素首次出现的位置，如果没有找到则返回-1
     *
     */
    int indexOf(Object o);

    /**
     * 返回特定元素最后一次出现的位置，如果没有找到则返回-1
     *
     */
    int lastIndexOf(Object o);


    // List Iterators

    /**
     * 返回一个ListIterator
     *
     */
    ListIterator<E> listIterator();

    /**
     * 返回一个ListIterator，从指定的索引开始
     *
     * 当第一次调用ListIterator#next时，返回指定索引的元素
     * 当第一次调用ListIterator#previous时，返回(指定索引-1)的元素
     *
     */
    ListIterator<E> listIterator(int index);

    // View

    /**
     *
     * 返回指定的<tt> fromIndex </ tt>（包含）和<tt> toIndex </ tt>之间的此列表部分的视图，exclusive。 （如果<tt> fromIndex </ tt>和<tt> toIndex </ tt>相等，则返回的列表为空。）返回的列表由此列表支持，因此返回列表中的非结构更改将反映在 这个列表，反之亦然。 返回的列表支持列表支持的所有可选列表操作。<p>
     *
     * Returns a view of the portion of this list between the specified
     * <tt>fromIndex</tt>, inclusive, and <tt>toIndex</tt>, exclusive.  (If
     * <tt>fromIndex</tt> and <tt>toIndex</tt> are equal, the returned list is
     * empty.)  The returned list is backed by this list, so non-structural
     * changes in the returned list are reflected in this list, and vice-versa.
     * The returned list supports all of the optional list operations supported
     * by this list.<p>
     *
     * 此方法消除了对显式范围操作（数组通常存在的那种操作）的需要。 任何需要列表的操作都可以通过传递子列表视图而不是整个列表来用作范围操作。 例如，下面的习语从列表中删除了一系列元素：
     * This method eliminates the need for explicit range operations (of
     * the sort that commonly exist for arrays).  Any operation that expects
     * a list can be used as a range operation by passing a subList view
     * instead of a whole list.  For example, the following idiom
     * removes a range of elements from a list:
     * <pre>{@code
     *      list.subList(from, to).clear();
     * }</pre>
     * Similar idioms may be constructed for <tt>indexOf</tt> and
     * <tt>lastIndexOf</tt>, and all of the algorithms in the
     * <tt>Collections</tt> class can be applied to a subList.<p>
     *
     * The semantics of the list returned by this method become undefined if the backing list (i.e., this list) is <i>structurally modified</i> in any way other than via the returned list.  (Structural modifications are those that change the size of this list, or otherwise perturb it in such a fashion that iterations in progress may yield incorrect results.)
     *
     * The semantics of the list returned by this method become undefined if
     * the backing list (i.e., this list) is <i>structurally modified</i> in
     * any way other than via the returned list.  (Structural modifications are
     * those that change the size of this list, or otherwise perturb it in such
     * a fashion that iterations in progress may yield incorrect results.)
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex high endpoint (exclusive) of the subList
     * @return a view of the specified range within this list
     *
     */
    List<E> subList(int fromIndex, int toIndex);

    /**
     * Creates a {@link Spliterator} over the elements in this list.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED} and
     * {@link Spliterator#ORDERED}.  Implementations should document the
     * reporting of additional characteristic values.
     *
     * @implSpec
     * The default implementation creates a
     * <em><a href="Spliterator.html#binding">late-binding</a></em> spliterator
     * from the list's {@code Iterator}.  The spliterator inherits the
     * <em>fail-fast</em> properties of the list's iterator.
     *
     * @implNote
     * The created {@code Spliterator} additionally reports
     * {@link Spliterator#SUBSIZED}.
     *
     * @return a {@code Spliterator} over the elements in this list
     * @since 1.8
     */
    @Override
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.ORDERED);
    }
}
