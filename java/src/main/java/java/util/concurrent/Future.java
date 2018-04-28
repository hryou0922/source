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
 * 此类表示异常的计算结果。
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Future<V> {

    /**
     * 深度取消当前的任务。
     * 以下情况会返回false
     *  a. 任务已经执行完毕
     *  b. 任务已经被取消
     *  c. 因为其它的原因无法被取消
     *
     * 如果执行这个方法成功，这个任务永远不会再被执行
     *
     * 如果在执行取消时，任务正在执行，根据mayInterruptIfRunning参数是否尝试中断当前正在执行的任务
     *
     * 当执行这个方法后，如果调用isDone，则一直返回true。
     * 如果执行这个方法返回true，则调用isCancelled一直返回true
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     * task should be interrupted; otherwise, in-progress tasks are allowed
     * to complete
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * 如果任务在正常执行完毕前被取消，则返回true
     */
    boolean isCancelled();

    /**
     * 满足以下任何条件，则返回true
     *  a. 正常结束
     *  b. 抛出异常
     *  c. 被取消
     *
     */
    boolean isDone();

    /**
     * 阻塞直任务执行完毕，并返回执行结果

     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     */
    V get() throws InterruptedException, ExecutionException;

    /**
     * 超时版本的get方法
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     * @throws TimeoutException if the wait timed out
     */
    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
