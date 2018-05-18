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
 * 用于处理哪些无法被ThreadPoolExecutor执行的任务
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface RejectedExecutionHandler {

    /**
     * 当ThreadPoolExecutor#execute的方法不在接受新任务时，此方法被ThreadPoolExecutor调用
     * 当Executor关闭或线程的数量超过阈值时，会导致没有更多线程来执行任务时，此方法会被执行
     *
     */
    void rejectedExecution(Runnable r, ThreadPoolExecutor executor);
}
