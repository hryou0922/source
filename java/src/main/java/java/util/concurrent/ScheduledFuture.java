/*
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

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * A delayed result-bearing action that can be cancelled.
 *
 * 一般是给ScheduledExecutorService使用
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface ScheduledFuture<V> extends Delayed, Future<V> {
}
