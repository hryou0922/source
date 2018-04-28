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
import java.util.List;
import java.util.Collection;

/**
 * 在Executor基础上增加关闭线程的功能和提供多个返回Future的方法，此futrue用于跟踪一个或多个异常的方法
 *
 */
public interface ExecutorService extends Executor {

    /**
     * 根据任务提交到Executor中的顺序逐个关闭任务。
     * 此时Executor不会再接受新的任务
     * 此方法被重复调用是不会有影响
     * 此方法不会等待之前已经提交的任务执行完毕。如果需要，则使用awaitTermination方法
     *
     */
    void shutdown();

    /**
     * 尝试停止所有正在执行的任务，暂停等待任务的处理，并返回正在等待执行的任务列表。
     * 此方法不会等待任务执行完毕才返回，如果有需要可以使用awaitTermination方法
     * 程序竭尽全力尝试停止处理主动执行的任务，但是不保证一定会停止。
     * 例如，典型的实现将通过Thread＃interrupt}来取消任务，但是如果任务不响应中断，则任务都不会终止。
     *
     *
     */
    List<Runnable> shutdownNow();

    /**
     * 如果executor已经关闭，则返回true
     *
     */
    boolean isShutdown();

    /**
     * 同时满足如下条件，才返回true
     *  1.所有任务者已经执行完毕
     *  2.shutdown或shutdownNow在此方法之前已经被调用
     *
     */
    boolean isTerminated();

    /**
     * 阻塞当前线程，直到以下任意一个条件发生：
     *  1. 所有的任务在 shutdown方法被调用后，执行完当前任务
     *  2. 超时
     *  3. 当前线程被中断
     *
     */
    boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * 提交有返回结果的Callable，并返回表示未完成任务结果的Future。 Future的get方法将在成功完成时返回任务的结果。
     *
     */
    <T> Future<T> submit(Callable<T> task);

    /**
     * 提交Runnable任务并返回表示该任务结果的Future
     *
     */
    <T> Future<T> submit(Runnable task, T result);

    /**
     * 提交Runnable，并返回Future
     */
    Future<?> submit(Runnable task);

    /**
     * 提交任务集合，当所有任务都被执行时，才返回表示任务执行情况的Future列表
     *  备注：执行完毕的任务有两类，一是正常结束，二是抛出异常结束
     *  返回Future列表顺序和tasks传入的顺序是一致的
     *
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException;

    /**
     * 超时版本的invokeAll。当因为超时返回Future列表时，未完成的任务Future的状态为cancelled
     *
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                  long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * 返回集合中任何一个执行成功（没有抛出异常）的结果
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException;

    /**
     * 超时版本的invokeAny
     *
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                    long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
