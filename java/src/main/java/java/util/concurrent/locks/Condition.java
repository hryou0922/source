/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.Date;

/**
 * 提供Object#wait() wait、Object#notify notify、Object#notifyAll notifyAll外的其它方式的等待/唤醒方式
 * Condtion通常和Lock关联
 *
 *  Conditions(也称为<条件队列或条件变量)暂停一个线程执行（“等待”）,直到另一个线程修改了condition的状态为true，才唤醒等待线程
 *  因为访问这个共享状态信息发生在不同的线程中，所以它必须被保护，所以某种形式的Lock与Condition相关联。
 *  Condtion提供的用于等待的关键属性：它自动释放关联的锁并挂起当前线程，就像{@code Object.wait}一样。
 *
 *  一个{Condition}实例内在地绑定到一个锁，
 *  要为特定的{Lock}实例获取一个{Condition}实例，请使用它的{Lock＃newCondition newCondition（}方法。
 *
 *  举例来说，假设我们有一个支持{put}和{take}方法的有界缓冲区。
 *  如果尝试在空缓冲区上执行{take}，则线程将阻塞，直到缓冲区上内有数据;
 *  如果在满的缓冲区上尝试{put}，则线程将阻塞，直到有空间可用。
 *
 *  我们希望将因{put}等待线程和因{take}等待线程放入不同的等待集中，这样我们可以优化通知。
 *  当缓冲区中的满或变的可用时，我们只需要通知一类线程
 *  这个可以使用两个Condition实例实现
 *
 * Demo 代码：
 * class BoundedBuffer {
 *   <b>final Lock lock = new ReentrantLock();</b>
 *   final Condition notFull  = <b>lock.newCondition(); </b>
 *   final Condition notEmpty = <b>lock.newCondition(); </b>
 *
 *   final Object[] items = new Object[100];
 *   int putptr, takeptr, count;
 *
 *   public void put(Object x) throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == items.length)
 *         <b>notFull.await();</b>
 *       items[putptr] = x;
 *       if (++putptr == items.length) putptr = 0;
 *       ++count;
 *       <b>notEmpty.signal();</b>
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 *
 *   public Object take() throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == 0)
 *         <b>notEmpty.await();</b>
 *       Object x = items[takeptr];
 *       if (++takeptr == items.length) takeptr = 0;
 *       --count;
 *       <b>notFull.signal();</b>
 *       return x;
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 * }
 * </pre>
 *
 * java.util.concurrent.ArrayBlockingQueue 实现了上面类似的功能
 *
 * Condition的提供了不同于Object监控方法的行为和语义。例如保证通知排序，或者在执行通知时不要求锁定。 如果实现提供了这种专用语义，那么实现必须记录这些语
 *
 * 注意：
 *  {Condition}实例只是普通对象，并且可以自己用作{synchronized}语句中的目标，并且可以拥有自己的监视器有方法：{Object#wait}和{Object#notify}
 *  获取Condition实例对象上的锁或使用监视方法，不会影响Lock和此Condition的关联，也不会影响{@linkplain #await } and {@linkplain #signal }方法的调用
 *  为避免混淆，建议您不要以这种方式使用条件实例，
 *
 * <h3>实施注意事项</h3>
 *
 * 当基于Condition的等待， 一般来说，作为对底层平台语义的让步，一个“虚假唤醒”可能会发生。
 * 这对大多数应用程序没有什么实际影响，因为Condition应始终在循环中等待，并测试正在等待的状态。
 * 一个实现可以自由地消除虚假唤醒的可能性，但建议应用程序员总是假设它们可能发生，因此总是等待循环。
 *
 * 三种condition等待情况(interruptible, non-interruptible, and timed) 在一些平台上的易用性和它们的性能特征可能不同
 * 特别是，可能很难提供这些功能并保持特定的语义，如排序保证。
 * 此外，中断当前暂停的线程的功能，并不能在所有平台上实现
 *
 * 因此，实现不需要为所有三种形式的等待定义完全相同的保证或语义，也不需要支持中断实际挂起的线程。
 *
 * 由于中断通常意味着取消，并且中断的检查通常不频繁，所以建议通过普通方法返回中断中断信息。
 *
 * 例子：
 *  java Condition源码分析：https://blog.csdn.net/coslay/article/details/45217069
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Condition {

    /**
     * 使得当前线程等待，直至线程被signalled或被Thread#interrupt中断
     *
     * 关联Condition的Lock会执行原子释放操作，并且当前线程因线程调度目的而被禁用，并且处于休眠状态，直到以下4种情况的任何一种：
     *  1. 其它线程调用这个Condtion调用#signal方法，当前线程恰好被唤醒
     *  2. 其它线程调用#signalAll方法
     *  3. 其它线程调用Thread#interrupt中断当前线程并且当前线程支持挂起线程中断
     *  4. "虚假唤醒"发生了
     *
     * 在以上所有的情况下，在此方法返回前，当前线程会重新获取和此condition相关的锁。即当线程返回，保证已经获取此锁。
     *
     * 如当前的线程：
     *  1. 在进入此方法时被设置了中断状态;
     *  2. 或者此线程被执行Thread#interrupt，当它在等待时且系统支持对悬挂的线程执行中断
     * 那么
     *  会抛出InterruptedException异常，并且当前线程的中断状态被清除。
     *  对于第一中情况，是不会详细说明中断是否发生在锁释放之前
     *
     */
    void await() throws InterruptedException;

    /**
     * 使得当前的线程等待直到被signalled。此方法不会被中断唤醒
     *
     * 关联Condition的Lock会执行原子释放操作，并且当前线程因线程调度目的而被禁用，并且处于休眠状态，直到以下3种情况的任何一种：
     *  1. 其它线程调用这个Condtion调用#signal方法，当前线程恰好被唤醒
     *  2. 其它线程调用#signalAll方法
     *  3. "虚假唤醒"发生了
     *
     *  在以上所有的情况下，在此方法返回前，当前线程会重新获取和此condition相关的锁。即当线程返回，保证已经获取此锁。
     *
     * 当进入此这个方法后，当前线程的被设置中断状态或线程在等待时被Thread#interrupt中断，
     * 则此线程会挂断等待直到被signalled。当此线程最终从方法返回时，线程的中断会被设置
     *
     */
    void awaitUninterruptibly();

    /**
     * 使用当前线程等待直到被signalled或被Thread#interrupt中断或超时
     *
     * * 关联Condition的Lock会执行原子释放操作，并且当前线程因线程调度目的而被禁用，并且处于休眠状态，直到以下5种情况的任何一种：
     * 1.2.3.4. 前4种情况和await()方法相同
     * 5. 等待的时间超时
     *
     * 在以上所有的情况下，在此方法返回前，当前线程会重新获取和此condition相关的锁。即当线程返回，保证已经获取此锁。
     *
     * 如当前的线程：
     *  1. 在进入此方法时被设置了中断状态;
     *  2. 或者此线程被执行Thread#interrupt，当它在等待时且系统支持对悬挂的线程执行中断
     * 那么
     *  会抛出InterruptedException异常，并且当前线程的中断状态被清除。
     *  对于第一种情况，是不会详细说明中断是否发生在锁释放之前
     *
     * 此方法返回预估的剩余的纳秒数或超时返回一个小于或等于零的值
     * 此值用于确定是否需要或再重新等待多久以持有condition上的锁，如果方法返回时线程没有持有锁
     *
     * 此方法的典型用法采用以下形式：
     *  <pre>
     * boolean aMethod(long timeout, TimeUnit unit) {
     *   long nanos = unit.toNanos(timeout);
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (nanos <= 0L)
     *         return false;
     *       nanos = theCondition.awaitNanos(nanos);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     */
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * 使用当前线程等待直到被signalled或被Thread#interrupt中断或超时
     * 和awaitNanos方法相同
     */
    boolean await(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 使用当前线程等待直到被signalled或被Thread#interrupt中断或直到到达指定的截止时间。
     * 关联Condition的Lock会执行原子释放操作，并且当前线程因线程调度目的而被禁用，并且处于休眠状态，直到以下5种情况的任何一种：
     * 1.2.3.4. 前4种情况和await()方法相同
     * 5. 到达指定的截止时间
     *
     * 在以上所有的情况下，在此方法返回前，当前线程会重新获取和此condition相关的锁。即当线程返回，保证已经获取此锁。
     *
     * 如当前的线程：
     *  1. 在进入此方法时被设置了中断状态;
     *  2. 或者此线程被执行Thread#interrupt，当它在等待时且系统支持对悬挂的线程执行中断
     * 那么
     *  会抛出InterruptedException异常，并且当前线程的中断状态被清除。
     *  对于第一种情况，是不会详细说明中断是否发生在锁释放之前
     *
     * 返回值指示截止日期是否已过：
     *  true: 如果截止日期已经过去，则返回true
     *
     * <pre>
     * boolean aMethod(Date deadline) {
     *   boolean stillWaiting = true;
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (!stillWaiting)
     *         return false;
     *       stillWaiting = theCondition.awaitUntil(deadline);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     */
    boolean awaitUntil(Date deadline) throws InterruptedException;

    /**
     * 唤醒一个等待的线程
     *
     * 如果有许多线程在等待在这个condition，则只选择一个进行唤醒。此线程会重新获取该锁在await方法返回执行
     *
     * 此方法的一个可能的实现（并且通常会）会要求当前的线程在这个方法中，持有与这个{Condition}相关联的锁。
     * 实现必须记录这个先决条件以及如果不锁定所采取的任何行动
     */
    void signal();

    /**
     * 唤醒所有的等待线程
     *
     * 所有在这个 condition上等待的线程会被全部唤醒。每个线程会在从await方法返回之前会尝试重新获取锁
     *
     * 此方法的一个可能的实现（并且通常会）会要求当前的线程在这个方法中，持有与这个{Condition}相关联的锁。
     * 实现必须记录这个先决条件以及如果不锁定所采取的任何行动
     */
    void signalAll();
}
