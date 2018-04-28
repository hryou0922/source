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
 * 混合接口标记在给定延迟后执行本对象
 *
 * 实现此接口的对象，必须实现compareTo方法用于排序，用于在getDelay中使用
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Delayed extends Comparable<Delayed> {

    /**
     * 返回剩余的延迟时间
     *
     * @param unit the time unit
     * @return the remaining delay; zero or negative values indicate
     * that the delay has already elapsed
     */
    long getDelay(TimeUnit unit);
}
