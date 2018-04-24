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
 * 执行Runnable任务。这个接口将任务的任务提交和任务如何执行进行解耦
 *
 *  ExecutorService：Executor的子类
 *  ThreadPoolExecutor：为ThreadPoolExecutor增加线程池功能
 *  Executors：创建Executor的工厂类
 *
 */
public interface Executor {

    /**
     * 在将来的某个时间运行给定的command。
     * 运行这个command的线程可以是新创建的、或线程池中的一个、或调用方。
     *
     * @param command the runnable task
     * @throws RejectedExecutionException if this task cannot be
     * accepted for execution
     * @throws NullPointerException if command is null
     */
    void execute(Runnable command);
}
