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
 * An implementation of {@link ReadWriteLock} supporting similar
 * semantics to {@link ReentrantLock}.
 * <p>This class has the following properties:
 *
 * <ul>
 * <li><b>Acquisition order</b>
 *
 * <p>This class does not impose a reader or writer preference
 * ordering for lock access.  However, it does support an optional
 * <em>fairness</em> policy.
 *
 * <dl>
 * <dt><b><i>Non-fair mode (default)</i></b>
 * <dd>When constructed as non-fair (the default), the order of entry
 * to the read and write lock is unspecified, subject to reentrancy
 * constraints.  A nonfair lock that is continuously contended may
 * indefinitely postpone one or more reader or writer threads, but
 * will normally have higher throughput than a fair lock.
 *
 * <dt><b><i>Fair mode</i></b>
 * <dd>When constructed as fair, threads contend for entry using an
 * approximately arrival-order policy. When the currently held lock
 * is released, either the longest-waiting single writer thread will
 * be assigned the write lock, or if there is a group of reader threads
 * waiting longer than all waiting writer threads, that group will be
 * assigned the read lock.
 *
 * <p>A thread that tries to acquire a fair read lock (non-reentrantly)
 * will block if either the write lock is held, or there is a waiting
 * writer thread. The thread will not acquire the read lock until
 * after the oldest currently waiting writer thread has acquired and
 * released the write lock. Of course, if a waiting writer abandons
 * its wait, leaving one or more reader threads as the longest waiters
 * in the queue with the write lock free, then those readers will be
 * assigned the read lock.
 *
 * <p>A thread that tries to acquire a fair write lock (non-reentrantly)
 * will block unless both the read lock and write lock are free (which
 * implies there are no waiting threads).  (Note that the non-blocking
 * {@link ReadLock#tryLock()} and {@link WriteLock#tryLock()} methods
 * do not honor this fair setting and will immediately acquire the lock
 * if it is possible, regardless of waiting threads.)
 * <p>
 * </dl>
 *
 * <li><b>Reentrancy</b>
 *
 * <p>This lock allows both readers and writers to reacquire read or
 * write locks in the style of a {@link ReentrantLock}. Non-reentrant
 * readers are not allowed until all write locks held by the writing
 * thread have been released.
 *
 * <p>Additionally, a writer can acquire the read lock, but not
 * vice-versa.  Among other applications, reentrancy can be useful
 * when write locks are held during calls or callbacks to methods that
 * perform reads under read locks.  If a reader tries to acquire the
 * write lock it will never succeed.
 *
 * <li><b>Lock downgrading</b>
 * <p>Reentrancy also allows downgrading from the write lock to a read lock,
 * by acquiring the write lock, then the read lock and then releasing the
 * write lock. However, upgrading from a read lock to the write lock is
 * <b>not</b> possible.
 *
 * <li><b>Interruption of lock acquisition</b>
 * <p>The read lock and write lock both support interruption during lock
 * acquisition.
 *
 * <li><b>{@link Condition} support</b>
 * <p>The write lock provides a {@link Condition} implementation that
 * behaves in the same way, with respect to the write lock, as the
 * {@link Condition} implementation provided by
 * {@link ReentrantLock#newCondition} does for {@link ReentrantLock}.
 * This {@link Condition} can, of course, only be used with the write lock.
 *
 * <p>The read lock does not support a {@link Condition} and
 * {@code readLock().newCondition()} throws
 * {@code UnsupportedOperationException}.
 *
 * <li><b>Instrumentation</b>
 * <p>This class supports methods to determine whether locks
 * are held or contended. These methods are designed for monitoring
 * system state, not for synchronization control.
 * </ul>
 *
 * <p>Serialization of this class behaves in the same way as built-in
 * locks: a deserialized lock is in the unlocked state, regardless of
 * its state when serialized.
 *
 * <p><b>Sample usages</b>. Here is a code sketch showing how to perform
 * lock downgrading after updating a cache (exception handling is
 * particularly tricky when handling multiple locks in a non-nested
 * fashion):
 *
 * <pre> {@code
 * class CachedData {
 *   Object data;
 *   volatile boolean cacheValid;
 *   final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *
 *   void processCachedData() {
 *     rwl.readLock().lock();
 *     if (!cacheValid) {
 *       // Must release read lock before acquiring write lock
 *       rwl.readLock().unlock();
 *       rwl.writeLock().lock();
 *       try {
 *         // Recheck state because another thread might have
 *         // acquired write lock and changed state before we did.
 *         if (!cacheValid) {
 *           data = ...
 *           cacheValid = true;
 *         }
 *         // Downgrade by acquiring read lock before releasing write lock
 *         rwl.readLock().lock();
 *       } finally {
 *         rwl.writeLock().unlock(); // Unlock write, still hold read
 *       }
 *     }
 *
 *     try {
 *       use(data);
 *     } finally {
 *       rwl.readLock().unlock();
 *     }
 *   }
 * }}</pre>
 *
 * ReentrantReadWriteLocks can be used to improve concurrency in some
 * uses of some kinds of Collections. This is typically worthwhile
 * only when the collections are expected to be large, accessed by
 * more reader threads than writer threads, and entail operations with
 * overhead that outweighs synchronization overhead. For example, here
 * is a class using a TreeMap that is expected to be large and
 * concurrently accessed.
 *
 *  <pre> {@code
 * class RWDictionary {
 *   private final Map<String, Data> m = new TreeMap<String, Data>();
 *   private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *   private final Lock r = rwl.readLock();
 *   private final Lock w = rwl.writeLock();
 *
 *   public Data get(String key) {
 *     r.lock();
 *     try { return m.get(key); }
 *     finally { r.unlock(); }
 *   }
 *   public String[] allKeys() {
 *     r.lock();
 *     try { return m.keySet().toArray(); }
 *     finally { r.unlock(); }
 *   }
 *   public Data put(String key, Data value) {
 *     w.lock();
 *     try { return m.put(key, value); }
 *     finally { w.unlock(); }
 *   }
 *   public void clear() {
 *     w.lock();
 *     try { m.clear(); }
 *     finally { w.unlock(); }
 *   }
 * }}</pre>
 *
 * <h3>Implementation Notes</h3>
 *
 * <p>This lock supports a maximum of 65535 recursive write locks
 * and 65535 read locks. Attempts to exceed these limits result in
 * {@link Error} throws from locking methods.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ReentrantReadWriteLock
        implements ReadWriteLock, java.io.Serializable {
    private static final long serialVersionUID = -6992448646407690164L;
    /** Inner class providing readlock */
    private final ReadLock readerLock;
    /** Inner class providing writelock */
    private final WriteLock writerLock;
    /** Performs all synchronization mechanics */
    final Sync sync;

    /**
     * Creates a new {@code ReentrantReadWriteLock} with
     * default (nonfair) ordering properties.
     */
    public ReentrantReadWriteLock() {
        this(false);
    }

    /**
     * Creates a new {@code ReentrantReadWriteLock} with
     * the given fairness policy.
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    public WriteLock writeLock() { return writerLock; }
    public ReadLock  readLock()  { return readerLock; }

    /**
     * ReentrantReadWriteLock的同步器实现
     * 分成公平锁版本和非公平锁版本
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 6317671515068378041L;

        /*
         * Read vs write count extraction constants and functions.
         * Lock state is logically divided into two unsigned shorts:
         * The lower one representing the exclusive (writer) lock hold count,
         * and the upper the shared (reader) hold count.
         */

        static final int SHARED_SHIFT   = 16;
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT);     // 0x00010000
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1; // 0x0000FFFF
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1; // 0x0000FFFF

        // 一个int值，高16位保存共享锁持有的数量，低16位保存独占锁持有的数量
        /** Returns the number of shared holds represented in count  */
        static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
        /** Returns the number of exclusive holds represented in count  */
        static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }

        /**
         * A counter for per-thread read hold counts.
         * 使用ThreadLocal维护，缓存在成员变量cachedHoldCounter中
         */
        static final class HoldCounter {
            int count = 0;
            // Use id, not reference, to avoid garbage retention
            final long tid = getThreadId(Thread.currentThread()); // 获取线程的tid值
        }

        /**
         * ThreadLocal subclass. Easiest to explicitly define for sake
         * of deserialization mechanics.
         */
        static final class ThreadLocalHoldCounter
            extends ThreadLocal<HoldCounter> {
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

        /**
         * 保存当前线程获取 reentrant read locks的数量
         * 只有constructor and readObject中初始此值
         * 当前线程拥有读锁的次数变成0时，会删除对应的值
         */
        private transient ThreadLocalHoldCounter readHolds;

        /**
         *
         * 保存最后一个成功获取readLock的线程的HoldCounter。
         *
         * 这会在通常情况下保存ThreadLocal查找，下一个要发布的线程是最后一个要获取的线程。""这是非易失性的，因为它只是作为一种启发式使用，对于线程缓存来说非常有用。
         * The hold count of the last thread to successfully acquire
         * readLock. This saves ThreadLocal lookup in the common case
         * where the next thread to release is the last one to
         * acquire. This is non-volatile since it is just used
         * as a heuristic, and would be great for threads to cache.
         *
         * 可以超越正在缓存读取保持计数的线程，但通过不保留对线程的引用来避免垃圾留存
         * <p>Can outlive the Thread for which it is caching the read
         * hold count, but avoids garbage retention by not retaining a
         * reference to the Thread.
         *
         * 通过良性数据竞赛访问;""依靠存储器模型的最终领域和超薄空气保证。 ""/私人瞬态HoldCounter cachedHoldCounter
         * Accessed via a benign data race; relies on the memory
         * model's final field and out-of-thin-air guarantees.
         */
        private transient HoldCounter cachedHoldCounter;

        /**
         * firstReader是第一个获取read lock.的线程
         * firstReaderHoldCount是firstReader持有的数量
         *
         * 更准确的说，firstReader是这样的一个唯一的线程：最后修改shared count从0到1，并且从这之后没有释放read lock
         * 如果没有这样的线程，则此值为0
         *
         * 除非线程在不放弃其读取锁的情况下终止，否则不会导致垃圾留存，因为tryReleaseShared将其设置为null
         *
         * 通过良性数据竞赛访问; 依赖于存储器模型对参考文献的超薄保证。
         * <p>Accessed via a benign data race; relies on the memory
         * model's out-of-thin-air guarantees for references.
         *
         * 这允许跟踪无争夺读锁的读取保持非常便宜。
         * <p>This allows tracking of read holds for uncontended read
         * locks to be very cheap.
         */
        private transient Thread firstReader = null;
        private transient int firstReaderHoldCount;

        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState()); // ensures visibility of readHolds
        }

        /*
         * Acquires and releases use the same code for fair and
         * nonfair locks, but differ in whether/how they allow barging
         * when queues are non-empty.
         */

        /**
         *
         * 当前线程试图获取读锁定并且有资格这样做，如果被阻塞，则返回true，因为超出其他等待线程的策略应该阻塞
         * Returns true if the current thread, when trying to acquire
         * the read lock, and otherwise eligible to do so, should block
         * because of policy for overtaking other waiting threads.
         */
        abstract boolean readerShouldBlock();

        /**
         * 当前线程试图获取写锁定并且有资格这样做，如果被阻塞，则返回true，因为超出其他等待线程的策略应该阻塞
         * Returns true if the current thread, when trying to acquire
         * the write lock, and otherwise eligible to do so, should block
         * because of policy for overtaking other waiting threads.
         */
        abstract boolean writerShouldBlock();

        /*
         * Note that tryRelease and tryAcquire can be called by
         * Conditions. So it is possible that their arguments contain
         * both read and write holds that are all released during a
         * condition wait and re-established in tryAcquire.
         */

        // 释放锁
        protected final boolean tryRelease(int releases) {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int nextc = getState() - releases; // 执行本次释放后，state的值
            boolean free = exclusiveCount(nextc) == 0; // 如果新的state为0，则设置锁的拥有线程为null
            if (free)
                setExclusiveOwnerThread(null);
            setState(nextc); // 设置state新值
            return free;
        }

        protected final boolean tryAcquire(int acquires) {
            /*
             * Walkthrough:
             * 1. If read count nonzero or write count nonzero
             *    and owner is a different thread, fail.
             * 2. If count would saturate, fail. (This can only
             *    happen if count is already nonzero.)
             * 3. Otherwise, this thread is eligible for lock if
             *    it is either a reentrant acquire or
             *    queue policy allows it. If so, update state
             *    and set owner.
             */
            Thread current = Thread.currentThread(); // 获取当前线程
            int c = getState(); // 获取state值
            int w = exclusiveCount(c); // 当前排它锁的持有数量
            if (c != 0) {
                // 如果 c!=0且w==0，则表示有线程在获取共享锁，所有获取读锁失败
                // 如果 c!=0且(w!=0且线程被当前线程获取)，则当前线程已经拥有写锁，可以再次获取写锁
                // (Note: if c != 0 and w == 0 then shared count != 0)
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                // 以下操作同时只有一个线程才会进入，所有代码线程安全
                if (w + exclusiveCount(acquires) > MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                // Reentrant acquire
                setState(c + acquires); // 设置新的statue，独占锁是低16位，可以直接加
                return true;
            }
            // 如果c == 0，才会执行如下代码
            if (writerShouldBlock() || // 写锁是否需要等待
                !compareAndSetState(c, c + acquires)) // 如果设置state失败，则表示此锁已经被其它线程获取
                return false;
            setExclusiveOwnerThread(current); // 如果设置statu成功，则设置当前线程拥有此锁
            return true;
        }

        // 释放共享锁
        protected final boolean tryReleaseShared(int unused) {
            Thread current = Thread.currentThread();
            if (firstReader == current) { // 如果当前线程是是第一个获取read lock.的线程
                // assert firstReaderHoldCount > 0;
                if (firstReaderHoldCount == 1) // 表示当前线程完全释放锁，设置此firstReader值为null
                    firstReader = null;
                else
                    firstReaderHoldCount--; // 只是将当前线程持有锁的次数-1
            } else {
                HoldCounter rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current))
                    rh = readHolds.get(); // 如果当前线程不是最后一个成功获取锁的线程，则使用ThreadLocalHoldCounteraqbc获取当前线程的HoldCounter值
                int count = rh.count;
                if (count <= 1) {
                    readHolds.remove(); // 如果线程的读持有次数为1，则意味本次完成释放锁，可以从缓存中删除此线程的HoldCounter值
                    if (count <= 0)
                        throw unmatchedUnlockException();
                }
                --rh.count; // 读锁的持有次数-1
            }
            for (;;) {
                int c = getState();
                int nextc = c - SHARED_UNIT; // 相同于 （读锁的次数 -1）。因为 state 的高16位存在读持有的数据， -1 相同  state - 0x00010000(SHARED_UNIT)
                if (compareAndSetState(c, nextc)) // 设置新的state值
                    // Releasing the read lock has no effect on readers,
                    // but it may allow waiting writers to proceed if
                    // both read and write locks are now free.
                    return nextc == 0;
            }
        }

        private IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException(
                "attempt to unlock read lock, not locked by current thread");
        }

        // 获取共享锁：失败返回 -1
        protected final int tryAcquireShared(int unused) {
            /*
             * Walkthrough:
             * 1. If write lock held by another thread, fail.
             * 2. Otherwise, this thread is eligible for
             *    lock wrt state, so ask if it should block
             *    because of queue policy. If not, try
             *    to grant by CASing state and updating count.
             *    Note that step does not check for reentrant
             *    acquires, which is postponed to full version
             *    to avoid having to check hold count in
             *    the more typical non-reentrant case.
             * 3. If step 2 fails either because thread
             *    apparently not eligible or CAS fails or count
             *    saturated, chain to version with full retry loop.
             */
            Thread current = Thread.currentThread();
            int c = getState();
            if (exclusiveCount(c) != 0 &&
                getExclusiveOwnerThread() != current) // 写锁已经被其它线程获取，则获取失败
                return -1;
            int r = sharedCount(c); // 返回共享锁持有的次数
            if (!readerShouldBlock() &&  // 当前线程试图获取读锁定并且有资格这样做，如果不被阻塞，则继续()条件后续判断
                r < MAX_COUNT &&
                compareAndSetState(c, c + SHARED_UNIT)) { // 设置新的读锁次数
                if (r == 0) { // 如果当前为第一个读，则设置firstReader和firstReaderHoldCount值
                    firstReader = current;
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) { // 如果当前为第一个
                    firstReaderHoldCount++;
                } else {
                    HoldCounter rh = cachedHoldCounter; // 获取缓存中的第一个
                    if (rh == null || rh.tid != getThreadId(current))
                        cachedHoldCounter = rh = readHolds.get(); //　如果当前不是缓存中的值，则将当前的访问HoldCounter设置为缓存值
                    else if (rh.count == 0)
                        readHolds.set(rh); //
                    rh.count++; // 设置读次数加1
                }
                return 1;
            }
            return fullTryAcquireShared(current);
        }

        /**
         * 完全版本读获取:处理CAS和可重复读没有被tryAcquireShared处理的部分
         * Full version of acquire for reads, that handles CAS misses
         * and reentrant reads not dealt with in tryAcquireShared.
         */
        final int fullTryAcquireShared(Thread current) {
            /*
             * This code is in part redundant with that in
             * tryAcquireShared but is simpler overall by not
             * complicating tryAcquireShared with interactions between
             * retries and lazily reading hold counts.
             */
            HoldCounter rh = null;
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0) { // 如果已经加了写锁,则进入
                    if (getExclusiveOwnerThread() != current) // 非锁的拥有者,退出方法
                        return -1;
                    // else we hold the exclusive lock; blocking here
                    // would cause deadlock.
                } else if (readerShouldBlock()) { // 当前线程试图获取读锁定并且有资格这样做，如果可以被阻塞，则返回true
                    // Make sure we're not acquiring read lock reentrantly
                    if (firstReader == current) { // 当前锁为第一个获取锁的线程，则执行后续的操作
                        // assert firstReaderHoldCount > 0;
                    } else {
                        if (rh == null) { //
                            rh = cachedHoldCounter; // 从缓存获取值
                            if (rh == null || rh.tid != getThreadId(current)) {
                                rh = readHolds.get(); // 如果缓存值和当前值不同，则使用当前线程值
                                if (rh.count == 0)
                                    readHolds.remove();
                            }
                        }
                        if (rh.count == 0)
                            return -1;
                    }
                }
                if (sharedCount(c) == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) { // 设置当前锁的读部分+1
                    if (sharedCount(c) == 0) {  // 如果当前为第一个读，则设置firstReader和firstReaderHoldCount值，这里同tryAcquireShared部分代码
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        if (rh == null)
                            rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                        cachedHoldCounter = rh; // cache for release 设置当前获取锁的线程信息
                    }
                    return 1;
                }
            }
        }

        /**
         * 尝试获取写锁, enabling barging in both modes.
         * This is identical in effect to tryAcquire except for lack
         * of calls to writerShouldBlock.
         */
        final boolean tryWriteLock() {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c != 0) {
                int w = exclusiveCount(c); // 获取当前写锁被写的次数
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false; // 如果已经加了读锁或已经被其它线程加了写锁，则返回
                if (w == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
            }
            if (!compareAndSetState(c, c + 1))
                return false;
            setExclusiveOwnerThread(current); // 获取写锁成功
            return true;
        }

        /**
         * 尝试获取读锁, enabling barging in both modes.
         * This is identical in effect to tryAcquireShared except for
         * lack of calls to readerShouldBlock.
         */
        final boolean tryReadLock() {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0 &&
                    getExclusiveOwnerThread() != current)
                    return false; // 如果锁已经被其它线程的获取写锁，则返回false
                int r = sharedCount(c);
                if (r == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) { // 尝试获取读锁
                    if (r == 0) { // 第一次获取读锁
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) { // 第一次获取读锁的线程再次获取读锁
                        firstReaderHoldCount++;
                    } else {
                        HoldCounter rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            cachedHoldCounter = rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                    }
                    return true;
                }
            }
        }

        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // Methods relayed to outer class

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // 如果锁的写锁没有被获取，则锁的拥有者为null，否则返回为锁的写锁拥有者线程
        final Thread getOwner() {
            // Must read state before owner to ensure memory consistency
            return ((exclusiveCount(getState()) == 0) ?
                    null :
                    getExclusiveOwnerThread());
        }

        // 返回读锁的次数
        final int getReadLockCount() {
            return sharedCount(getState());
        }

        // 是否被加了写锁
        final boolean isWriteLocked() {
            return exclusiveCount(getState()) != 0;
        }

        // 返回当前线程写锁的拥有次数
        final int getWriteHoldCount() {
            return isHeldExclusively() ? exclusiveCount(getState()) : 0;
        }

        // 返回当前线程读锁的拥有次数
        final int getReadHoldCount() {
            if (getReadLockCount() == 0)
                return 0;

            Thread current = Thread.currentThread();
            if (firstReader == current)
                return firstReaderHoldCount;

            HoldCounter rh = cachedHoldCounter;
            if (rh != null && rh.tid == getThreadId(current))
                return rh.count;

            int count = readHolds.get().count;
            if (count == 0) readHolds.remove();
            return count;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            readHolds = new ThreadLocalHoldCounter();
            setState(0); // reset to unlocked state
        }

        final int getCount() { return getState(); }
    }

    /**
     * 非公平锁版本同步器
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -8159625535654395037L;
        final boolean writerShouldBlock() {
            return false; // writers can always barge
        }
        final boolean readerShouldBlock() {
            /* As a heuristic to avoid indefinite writer starvation,
             * block if the thread that momentarily appears to be head
             * of queue, if one exists, is a waiting writer.  This is
             * only a probabilistic effect since a new reader will not
             * block if there is a waiting writer behind other enabled
             * readers that have not yet drained from the queue.
             */
            // 第一个结点存在且正在独占模式下等待，返回true
            return apparentlyFirstQueuedIsExclusive();
        }
    }

    /**
     * 公平锁版本同步器
     * Fair version of Sync
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -2274990926593161451L;
        final boolean writerShouldBlock() {
            // 查询队列中是否有比当前线程等待时间更长的线程，如果是，则返回true
            return hasQueuedPredecessors();
        }
        final boolean readerShouldBlock() {
            // 查询队列中是否有比当前线程等待时间更长的线程，如果是，则返回true
            return hasQueuedPredecessors();
        }
    }

    /**
     * The lock returned by method {@link ReentrantReadWriteLock#readLock}.
     */
    public static class ReadLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -5992448646407690164L;
        private final Sync sync;

        /**
         * Constructor for use by subclasses
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * 获取读锁
         *
         * 如果锁没有被其它线程获取，则获取读取并立即返回
         * 如果写锁已经被其它线程获取，则进行等待
         *
         */
        public void lock() {
            sync.acquireShared(1);
        }

        /**
         * 获取锁直至当前线程被 Thread#interrupt 中断
         * 如果写锁没有被其它线程持有，则立即获取读锁并返回
         * 如果写锁已经被其它线程获取，则进行等待
         *
         * 详细见{@link  Lock#lockInterruptibly}
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        /**
         * 尝试获取锁，
         * 如果当前写锁没有被其它线程持有，则获取锁，并立即返回
         * 详细见{@link  Lock#tryLock}
         */
        public boolean tryLock() {
            return sync.tryReadLock();
        }

        /**
         * 超时尝试获取锁
         * 详细见{@link  Lock#tryLock}
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

        /**
         * 释放锁
         * 详细见{@link  Lock#unlock}
         */
        public void unlock() {
            sync.releaseShared(1);
        }

        /**
         * 读锁不支持newCondition
         *
         * @throws UnsupportedOperationException always
         */
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        public String toString() {
            int r = sync.getReadLockCount();
            return super.toString() +
                "[Read locks = " + r + "]";
        }
    }

    /**
     * The lock returned by method {@link ReentrantReadWriteLock#writeLock}.
     */
    public static class WriteLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -4992448646407690164L;
        private final Sync sync;

        /**
         * Constructor for use by subclasses
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * 获取写锁
         * 如果读写和写锁都没有被其它线程获取，则获取写锁，则立即返回
         * 如果当前线程已经获取写锁，则hold count值加1，然后方法立即返回
         * 如果锁被其它线程获取，则当前线程进行等待，直到获取写锁
         *
         */
        public void lock() {
            sync.acquire(1);
        }

        /**
         * 获取锁直至当前线程被 Thread#interrupt 中断
         * 省略的内容同本类中的lock()方法内容.. 略
         *
         * 详细见{@link  Lock#lockInterruptibly}
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        /**
         * 读锁和写锁没有被其它线程所有，则获取写锁成功
         * 如果当前线程已经获取写锁，则hold count值加1
         *
         */
        public boolean tryLock( ) {
            return sync.tryWriteLock();
        }

        /**
         * 超时尝试获取锁
         * 在超时之前，读锁和写锁没有被其它线程所有/或当前锁已经被线程所有，则获取写锁成功
         *
         * 详细见{@link  Lock#tryLock}
         *
         * @return {@code true} if the lock was free and was acquired
         * by the current thread, or the write lock was already held by the
         * current thread; and {@code false} if the waiting time
         * elapsed before the lock could be acquired.
         *
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }

        /**
         * 尝试释放锁
         * 如果当前线程持有当前锁，则hold count值减1
         * 如果hold count值为0，则锁被释放
         * 如果当前线程没有持有这个锁，则抛出IllegalMonitorStateException
         *
         */
        public void unlock() {
            sync.release(1);
        }

        /**
         * 使用当前的锁，返回一个Condition实例
         *
         * 返回的Condition实例，可以像使用Object#wait()、Object#notify notify、Object#notifyAll notifyAll 一样使用锁
         *
         * <li>If this write lock is not held when any {@link
         * Condition} method is called then an {@link
         * IllegalMonitorStateException} is thrown.  (Read locks are
         * held independently of write locks, so are not checked or
         * affected. However it is essentially always an error to
         * invoke a condition waiting method when the current thread
         * has also acquired read locks, since other threads that
         * could unblock it will not be able to acquire the write
         * lock.)
         *
         * <li>When the condition {@linkplain Condition#await() waiting}
         * methods are called the write lock is released and, before
         * they return, the write lock is reacquired and the lock hold
         * count restored to what it was when the method was called.
         *
         * <li>If a thread is {@linkplain Thread#interrupt interrupted} while
         * waiting then the wait will terminate, an {@link
         * InterruptedException} will be thrown, and the thread's
         * interrupted status will be cleared.
         *
         * <li> Waiting threads are signalled in FIFO order.
         *
         * <li>The ordering of lock reacquisition for threads returning
         * from waiting methods is the same as for threads initially
         * acquiring the lock, which is in the default case not specified,
         * but for <em>fair</em> locks favors those threads that have been
         * waiting the longest.
         *
         * </ul>
         *
         * @return the Condition object
         */
        public Condition newCondition() {
            return sync.newCondition();
        }

        public String toString() {
            Thread o = sync.getOwner();
            return super.toString() + ((o == null) ?
                                       "[Unlocked]" :
                                       "[Locked by thread " + o.getName() + "]");
        }

        /**
         * 查询当前线程是否持有写锁
         * @return {@code true} if the current thread holds this lock and
         *         {@code false} otherwise
         * @since 1.6
         */
        public boolean isHeldByCurrentThread() {
            return sync.isHeldExclusively();
        }

        /**
         * 查询当前写锁持有的数量
         *
         * @return the number of holds on this lock by the current thread,
         *         or zero if this lock is not held by the current thread
         * @since 1.6
         */
        public int getHoldCount() {
            return sync.getWriteHoldCount();
        }
    }

    // Instrumentation and status

    /**
     * 如果是公平锁，则返回true
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 返回当前拥有写锁的线程
     *
     * 该方法旨在便于构建提供更广泛的锁定监视设施的子类。
     *
     *  @return the owner, or {@code null} if not owned
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * 查询当前锁持有的读锁的数量
     * 此方法用于监视系统，不做同步控制
     */
    public int getReadLockCount() {
        return sync.getReadLockCount();
    }

    /**
     * 查询当前写锁是否被任意线程持有
     * 此方法用于监视系统，不做同步控制
     * @return {@code true} if any thread holds the write lock and
     *         {@code false} otherwise
     */
    public boolean isWriteLocked() {
        return sync.isWriteLocked();
    }

    /**
     * 查询写锁是否被当前线程持有
     * 此方法用于监视系统，不做同步控制
     *  @return {@code true} if the current thread holds the write lock and
     *         {@code false} otherwise
     */
    public boolean isWriteLockedByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * 查询当前线程持有
     * Queries the number of reentrant write holds on this lock by the
     * current thread.
     * 此方法用于监视系统，不做同步控制
     *
     * @return the number of holds on the write lock by the current thread,
     *         or zero if the write lock is not held by the current thread
     */
    public int getWriteHoldCount() {
        return sync.getWriteHoldCount();
    }

    /**
     * Queries the number of reentrant read holds on this lock by the
     * current thread.
     *
     * 此方法用于监视系统，不做同步控制
     *
     * @return the number of holds on the read lock by the current thread,
     *         or zero if the read lock is not held by the current thread
     * @since 1.6
     */
    public int getReadHoldCount() {
        return sync.getReadHoldCount();
    }

    /**
     * 返回可能在等待获取写锁的线程集合
     *
     * 此方法用于监视系统，不做同步控制
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedWriterThreads() {
        return sync.getExclusiveQueuedThreads();
    }

    /**
     *
     * 返回可能在等待获取读锁的线程集合
     *
     * 此方法用于监视系统，不做同步控制
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedReaderThreads() {
        return sync.getSharedQueuedThreads();
    }

    /**
     * 查询是否有线程在等待获取读锁或写锁
     *
     * 此方法用于监视系统，不做同步控制
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 查询指定的线程是否在等待获取读锁或写锁
     *
     * 此方法用于监视系统，不做同步控制
     *
     * @param thread the thread
     * @return {@code true} if the given thread is queued waiting for this lock
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * 查询等待获取读锁或写锁的线程的数量
     *
     * 此方法用于监视系统，不做同步控制
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 查询等待获取读锁或写锁的线程的集合
     *
     * 此方法用于监视系统，不做同步控制
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 查询是否有线程在指定的Condition上等待
     *
     * 此方法用于监视系统，不做同步控制
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     *
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 查询在指定的Condition上等待线程的数量
     *
     * 此方法用于监视系统，不做同步控制
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     *
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 查询在指定的Condition上等待线程的集合
     * 此方法用于监视系统，不做同步控制
     *
     * @param condition the condition
     * @return the collection of threads
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
     * The state, in brackets, includes the String {@code "Write locks ="}
     * followed by the number of reentrantly held write locks, and the
     * String {@code "Read locks ="} followed by the number of held
     * read locks.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        int c = sync.getCount();
        int w = Sync.exclusiveCount(c);
        int r = Sync.sharedCount(c);

        return super.toString() +
            "[Write locks = " + w + ", Read locks = " + r + "]";
    }

    /**
     * Returns the thread id for the given thread.  We must access
     * this directly rather than via method Thread.getId() because
     * getId() is not final, and has been known to be overridden in
     * ways that do not preserve unique mappings.
     */
    static final long getThreadId(Thread thread) {
        return UNSAFE.getLongVolatile(thread, TID_OFFSET);
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long TID_OFFSET;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            TID_OFFSET = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("tid"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
