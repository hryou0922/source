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

/**
 *
 *
 * Lock提供比使用{synchronized}方法和语句更广泛的锁定操作。
 * Lock允许更灵活的结构化，可能具有完全不同的属性，并且可以支持多个关联的{Condition}对象。
 *
 * Lock是一个控制多线程访问共享资源的工具。
 * 通常，锁提供对共享资源的独占访问权限：一次只有一个线程可以获取该锁，并且对共享资源的所有访问都要求首先获取该锁。
 * 但是，某些锁定可能允许并发访问共享资源，例如Read LinkLock的读取锁定。
 *
 * 使用synchronized方法或语句可以访问与每个对象相关的隐式监视器锁，但强制所有的锁获取和释放以块结构的方式发生：
 *  当获得多个锁时，它们必须是以相反的顺序释放，并且所有的锁都必须在它们被获取的同一个词法范围内释放。
 *
 *
 * 尽管{synchronized}方法和语句的范围机制使得使用监视器锁编程变得更加容易，并且有助于避免涉及锁的许多常见编程错误，
 * 但在某些情况下，您需要以更灵活的方式处理锁。
 * 例如，用于并发遍历访问的数据结构的一些算法需要使用“交换”（hand-over-hand） 或“链锁定”：
 * 您获得节点A的锁，然后获取节点B的锁，然后释放节点A的锁再获取节点C的锁，然后释放节点B的锁再获取节点D的锁等等。
 * Lock可以实现这样形式：通过允许在不同范围内获取和释放锁，并允许以任意顺序获取和释放多个锁
 *
 * 没有块结构的锁会自动释放锁，当离开{synchronized}方法和语句时。
 * Lock增加了灵活性，但是需要增加额外的释放，通常用法如下
 *
 *  <pre> {@code
 * Lock l = ...;
 * l.lock();
 * try {
 *   // access the resource protected by this lock
 * } finally {
 *   l.unlock();
 * }}</pre>
 *
 * 当在不同的作用域中发生锁定和解锁时，必须注意确保在锁定期间执行的所有代码都由try-finally或try-catch保护，以确保在必要时释放锁定。
 *
 * 比起{synchronized}方法和语句的同步方式，Lock可以通过tryLock（）尝试获取非阻塞获取锁，
 * 使用lockInterruptibly获取可以被中断的锁，使用tryLock（long，TimeUnit）尝试获取可能超时的锁
 * Lock类还可以提供与隐式监视器锁定非常不同的行为和语义，例如保证排序，不可重入使用或死锁检测。
 *
 * 请注意，{Lock}实例只是普通的对象，并且它们本身可以用作一个{synchronized}语句中的目标
 * 获取“锁定”实例的监视器锁定与调用该实例的任何{#lock}方法没有特定的关系
 * 为避免混淆，建议您不要以这种方式使用Lock实例，除非在它们自己的实现中
 *
 *
 * <h3>内存同步</h3>
 * 所有锁定实现必须强制执行与内置监视器锁定相同的内存同步语义
 *  成功的lock方法操作与成功的锁操作具有相同的内存同步效果。
 *  成功的unlock方法操作与成功的解锁操作具有相同的内存同步效果。
 *
 *
 * 三种形式的锁获取（可中断，不可中断和定时）可能拥有不同的性能特征、执行顺序或其他。
 * 此外，在特定的Lock类上，中断正在获取锁的操作是不可行的。
 * 因此，不需要为所有三种形式锁获取定义完全相同的保证或语义，也不需要支持中断正在进行的锁获取。
 * 需要清楚的记录每个锁定方法提供的语义和保证
 * 它也必须遵守这个接口中定义的中断语义，以支持锁获取的中断：哪一个是完全的，或者仅仅是方法入口。
 *
 *
 * 由于中断通常意味着取消，并且检查中断通常很少发生，所以通常使用正常方法返回的中断的信息
 *
 * @see ReentrantLock
 * @see Condition
 * @see ReadWriteLock
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Lock {

    /**
     * 获取锁
     *
     * 如果锁不可用，则阻塞当前线程
     *
     * Lock实现可能发现检测到锁的错误使用。例如会导致死锁的调用、抛出异常
     *
     */
    void lock();

    /**
     * 获取锁直至当前线程被 Thread#interrupt 中断
     *
     * 如果获取锁成功，则立即返回。
     * 如果当前没有可用的锁，则当前线程被阻塞，直到发生如下2个情况：
     *  a. 当前线程获取锁
     *  b. 其它调用Thread#interrupt中断当前线程并且支持锁获取过程中的中断
     *
     * 如果当前线程发到如下情况
     *  a. 在进入方法时被设置了中断状态
     *  b. 或在获取锁的过程中被其它线程调用了Thread#interrupt中断过并且支持锁获取过程中的中断
     * 那么会抛出异常InterruptedException并且当前线程的中断状态被清除
     *
     * <p>The ability to interrupt a lock acquisition in some
     * implementations may not be possible, and if possible may be an
     * expensive operation.  The programmer should be aware that this
     * may be the case. An implementation should document when this is
     * the case.
     *
     * 通常通过普通方法的返回中断的状态
     *
     * Lock实现可能发现检测到锁的错误使用。例如会导致死锁的调用、抛出异常
     *
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * 尝试获取锁
     *  如果获取锁成功，则立即返回true
     *  如果当前锁不可用，也立即返回值，值为false.
     *
     * <p> 使用的场景:
     *
     *  <pre> {@code
     * Lock lock = ...;
     * if (lock.tryLock()) {
     *   try {
     *     // manipulate protected state
     *   } finally {
     *     lock.unlock();
     *   }
     * } else {
     *   // perform alternative actions
     * }}</pre>
     *
     * This usage ensures that the lock is unlocked if it was acquired, and
     * doesn't try to unlock if the lock was not acquired.
     *
     */
    boolean tryLock();

    /**
     * 中断版本的tryLock()
     *
     * 如果获取锁成功，则返回，返回值为true。
     *
     * 如果当前没有可用锁，则阻塞当前线程:
     *  a. 在超时之前获取锁，则返回true
     *  b. 如果当前线程被其它线程中断，则抛出异常
     *  c. 如果当前线程超时未获取锁，则返回false
     *
     *
     * 如果当前线程发到如下情况
     *  a. 在进入方法时被设置了中断状态
     *  b. 或在获取锁的过程中被其它线程调用了Thread#interrupt中断过并且支持锁获取过程中的中断
     * 那么会抛出异常InterruptedException并且当前线程的中断状态被清除
     *
     * 如果指定等待的时间 <= 0，则方法不会执行等待
     *
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The ability to interrupt a lock acquisition in some implementations
     * may not be possible, and if possible may
     * be an expensive operation.
     * The programmer should be aware that this may be the case. An
     * implementation should document when this is the case.
     *
     * 通常通过普通方法的返回中断的状态
     * Lock实现可能发现检测到锁的错误使用。例如会导致死锁的调用、抛出异常
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return {@code true} if the lock was acquired and {@code false}
     *         if the waiting time elapsed before the lock was acquired
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while acquiring the lock (and interruption of lock
     *         acquisition is supported)
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 释放锁
     *
     *  Lock实现通常会限制哪个线程可以释放一个锁（通常只有锁的持有者才能释放它），
     *  如果违反限制条件，可能会抛出一个异常
     *
     */
    void unlock();

    /**
     * Returns a new {@link Condition} instance that is bound to this
     * {@code Lock} instance.
     *
     * <p>Before waiting on the condition the lock must be held by the
     * current thread.
     * A call to {@link Condition#await()} will atomically release the lock
     * before waiting and re-acquire the lock before the wait returns.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The exact operation of the {@link Condition} instance depends on
     * the {@code Lock} implementation and must be documented by that
     * implementation.
     *
     * @return A new {@link Condition} instance for this {@code Lock} instance
     * @throws UnsupportedOperationException if this {@code Lock}
     *         implementation does not support conditions
     */
    Condition newCondition();
}
