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
import java.util.Collection;

/**
 * A reentrant mutual exclusion {@link Lock} with the same basic
 * behavior and semantics as the implicit monitor lock accessed using
 * {@code synchronized} methods and statements, but with extended
 * capabilities.
 *
 * <p>A {@code ReentrantLock} is <em>owned</em> by the thread last
 * successfully locking, but not yet unlocking it. A thread invoking
 * {@code lock} will return, successfully acquiring the lock, when
 * the lock is not owned by another thread. The method will return
 * immediately if the current thread already owns the lock. This can
 * be checked using methods {@link #isHeldByCurrentThread}, and {@link
 * #getHoldCount}.
 *
 * <p>The constructor for this class accepts an optional
 * <em>fairness</em> parameter.  When set {@code true}, under
 * contention, locks favor granting access to the longest-waiting
 * thread.  Otherwise this lock does not guarantee any particular
 * access order.  Programs using fair locks accessed by many threads
 * may display lower overall throughput (i.e., are slower; often much
 * slower) than those using the default setting, but have smaller
 * variances in times to obtain locks and guarantee lack of
 * starvation. Note however, that fairness of locks does not guarantee
 * fairness of thread scheduling. Thus, one of many threads using a
 * fair lock may obtain it multiple times in succession while other
 * active threads are not progressing and not currently holding the
 * lock.
 * Also note that the untimed {@link #tryLock()} method does not
 * honor the fairness setting. It will succeed if the lock
 * is available even if other threads are waiting.
 *
 * <p>It is recommended practice to <em>always</em> immediately
 * follow a call to {@code lock} with a {@code try} block, most
 * typically in a before/after construction such as:
 *
 *  <pre> {@code
 * class X {
 *   private final ReentrantLock lock = new ReentrantLock();
 *   // ...
 *
 *   public void m() {
 *     lock.lock();  // block until condition holds
 *     try {
 *       // ... method body
 *     } finally {
 *       lock.unlock()
 *     }
 *   }
 * }}</pre>
 *
 * <p>In addition to implementing the {@link Lock} interface, this
 * class defines a number of {@code public} and {@code protected}
 * methods for inspecting the state of the lock.  Some of these
 * methods are only useful for instrumentation and monitoring.
 *
 * <p>Serialization of this class behaves in the same way as built-in
 * locks: a deserialized lock is in the unlocked state, regardless of
 * its state when serialized.
 *
 * <p>This lock supports a maximum of 2147483647 recursive locks by
 * the same thread. Attempts to exceed this limit result in
 * {@link Error} throws from locking methods.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;
    /** Synchronizer providing all implementation mechanics */
    private final Sync sync;

    /**
     * 基础的同步器的实现。
     * 其子类为有公平锁和非公平锁两个
     * 使用AWS的state表示持有锁的数量
     *
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        /**
         * Performs {@link Lock#lock}. The main reason for subclassing
         * is to allow fast path for nonfair version.
         */
        abstract void lock();

        /**
         * 非公平锁的tryLock
         * tryAcquire在子类中实现
         * Performs non-fair tryLock.  tryAcquire is implemented in
         * subclasses, but both need nonfair try for trylock method.
         */
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread(); // 获取当前线程
            int c = getState();
            if (c == 0) { // 如果当前的state为0，则尝试获取锁
                if (compareAndSetState(0, acquires)) { // 如果原子设置state值为从0-acquires，则获取值成功
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread())  {// 如果当前的state非0，但是当前锁的拥有者为当前线程，则可以锁可以重入
                int nextc = c + acquires; // 设置新的state值
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }

        /**
         * 释放锁
         * @param releases
         * @return
         */
        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread()) // 如果不是锁的拥有者释放锁,则抛出异常
                throw new IllegalMonitorStateException();
            // 下面代码开始同时对于同一个锁，只能有一个线程操作，所有以下为为线程安全操作
            boolean free = false;
            if (c == 0) { // 如果新的status值为0, 则表示当前线程完全释放锁,需要设置锁的拥有者为null
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c); //　设置status
            return free;
        }

        /**
         * 返回true：表示当前线程拥有此锁
         * @return
         */
        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // Methods relayed from outer class

        // 获取锁的拥有者
        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        // 如果当前state是0，则表示当前锁正在被使用，返回true
        final boolean isLocked() {
            return getState() != 0;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }

    /**
     * 非公平锁的实现
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        /**
         * Performs lock.  Try immediate barge, backing up to normal
         * acquire on failure.
         */
        final void lock() {
            if (compareAndSetState(0, 1)) // 尝试直接设置state值从0->1，如果成功，则获取锁成功
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }

    /**
     * 公平锁：
     *  和非公平锁的区别是：lock和tryAcquire的实现方法不同
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        final void lock() {
            acquire(1);
        }

        /**
         * 公平锁版本的 tryAcquire
         * Don't grant access unless
         * recursive call or no waiters or is first.
         */
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) { // 如果当前锁没有被人获取
                if (!hasQueuedPredecessors() && // 当前线程排在队列的头位置或队列为空，则尝试获取锁
                    compareAndSetState(0, acquires)) { // 设置state状态从0到1，如果成功，则获取锁成功
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) { // 如果当前的state非0，但是当前锁的拥有者为当前线程，则可以锁可以重入
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
    }

    /**
     * Creates an instance of {@code ReentrantLock}.
     * This is equivalent to using {@code ReentrantLock(false)}.
     */
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    /**
     * Creates an instance of {@code ReentrantLock} with the
     * given fairness policy.
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * 获取锁
     *
     * #...
     */
    public void lock() {
        sync.lock();
    }

    /**
     * 可中断的获取锁
     *
     * #...
     */
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     * 尝试获取锁
     *  如果成功获取锁或锁本身为本线程所有，则返回true。
     *  如果失败，则直接返回false
     * 相对lock方法，此方法是不会阻塞等待
     *
     * #...
     */
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * 在指定时间内且线程未被中断时，获取锁
     *
     * @param timeout the time to wait for the lock
     * @param unit the time unit of the timeout argument
     * @return {@code true} if the lock was free and was acquired by the
     *         current thread, or the lock was already held by the current
     *         thread; and {@code false} if the waiting time elapsed before
     *         the lock could be acquired
     */
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * 释放锁
     *
     * #...
     *
     * @throws IllegalMonitorStateException if the current thread does not
     *         hold this lock
     */
    public void unlock() {
        sync.release(1);
    }

    /**
     * 返回一个基于当前Lock实例的Condition实例
     *
     * # ...
     *
     * @return the Condition object
     */
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * 查询当前线程持有此锁的数量
     * 此方法主要用于测试
     *
     * #...
     *
     * @return the number of holds on this lock by the current thread,
     *         or zero if this lock is not held by the current thread
     */
    public int getHoldCount() {
        return sync.getHoldCount();
    }

    /**
     * 查询此锁是否被当前线程获取
     * 此方法主要用于测试
     *
     * # ...
     *
     * @return {@code true} if current thread holds this lock and
     *         {@code false} otherwise
     */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * 查询当前锁是否被任一线程拥有。
     * 此方法主要用于监视系统的状态，不用于同步控制
     *
     * @return {@code true} if any thread holds this lock and
     *         {@code false} otherwise
     */
    public boolean isLocked() {
        return sync.isLocked();
    }

    /**
     * 如果当前使用公平锁，则返回true
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 返回拥有当前锁的线程，如果锁没有被线程拥有，则返回null
     *
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * 查询当前是否有线程在等待获取锁
     * 此方法主要用于监视系统状态，不是用于同步控制
     *
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 查询当前线程是否在等待队列中
     * 此方法主要用于监视系统状态，不是用于同步控制
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * 返回当前等待队列中线程的数量的估值
     * 此方法主要用于监视系统状态，不是用于同步控制
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 返回当前等待队列中所有可能的线程
     * 此方法主要用于监视系统状态，不是用于同步控制
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 查询condtion队列中是否有线程在等待
     * 此方法主要用于监视系统状态，不是用于同步控制
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 查询在condtion队列中是等待线程的估值数量
     * 此方法主要用于监视系统状态，不是用于同步控制
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 查询在condtion队列中是等待线程的估值列表
     * 此方法主要用于监视系统状态，不是用于同步控制
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a string identifying this lock, as well as its lock state.
     * The state, in brackets, includes either the String {@code "Unlocked"}
     * or the String {@code "Locked by"} followed by the
     * {@linkplain Thread#getName name} of the owning thread.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                                   "[Unlocked]" :
                                   "[Locked by thread " + o.getName() + "]");
    }
}
