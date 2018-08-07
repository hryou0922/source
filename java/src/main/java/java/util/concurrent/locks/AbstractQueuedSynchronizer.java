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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import sun.misc.Unsafe;

/**
 * 为实现依赖于先进先出（FIFO）等待队列的阻塞锁和相关同步器（信号量，事件等）提供框架
 * 此类对大多数依赖于单个原子{int}值来表示状态的同步器非常有用
 * 子类需要重新实现修改类state值的protectd方法以及获取或释放此状态的方法
 * 鉴于这些，本类中其它方法实现了所有的排队和阻塞机制。
 * 子类可以维护其他状态字段，但是只能通过{#getState}，{#setState}和{#compareAndSetState}原子更新{int}值。
 *
 *  <p>应将子类定义为用于实现其封闭类的同步属性的非公共内部辅助类。
 * <p>Subclasses should be defined as non-public internal helper
 * classes that are used to implement the synchronization properties
 * of their enclosing class.
 * 类AbstractQueuedSynchronizer不实现任何同步接口。 相反，它定义了诸如{#acquireInterruptibly}之类的方法，这此方法用于实现具体锁和相关同步器的公共方法。
 *
 * 此方法可以单个或同时支持独占（exclusive）模式和共享模式，默认为独占模式。
 * 在独占模式下，如果锁已经被获取，则其他线程尝试获取不成功。而在共享模式，多个线程可能（但不一定）同时获取锁成功
 * 共享模式获取成功时，下一个等待线程（如果存在的话）还必须确定它是否也可以获取。 在不同模式下等待的线程共享相同的FIFO队列。
 * 通常，实现子类只支持这些模式中的一种，但在{ReadWriteLock}中同时支持两者模式。
 * 仅支持独占模式或仅共享模式的子类不需要定义支持未使用模式的方法。
 *
 * 此类中定义一个内部类ConditionObject，它是Condition的实现类。
 * 它用于哪些需要支持独占模式的子类：
 *  方法{#isHeldExclusively}报告同步器是否被当前线程独占
 *  方法{#release}使用{#getState}值做为参数，会完全释放此对象，并且方法{#acquire}使用{#getState}值做为参数会下最终将此对象恢复到其先前获取的状态
 *
 *  否则{AbstractQueueQueuedSynchronizer}方法会创建这样一个条件，所以如果不能满足这个约束，就不要使用它。 {@link ConditionObject}的行为当然取决于其同步器实现的语义。
 *  No {@code AbstractQueuedSynchronizer} method otherwise creates such a
 * condition, so if this constraint cannot be met, do not use it.  The
 * behavior of {@link ConditionObject} depends of course on the
 * semantics of its synchronizer implementation.
 *
 * 此类为内部队列提供检查，检测和监视方法，以及condition object的类似提供类似的方法。
 *
 * 这些可以根据需要导出到类中，使用AbstractQueuedSynchronizer作为它们的同步机制。
 * These can be exported as desired into classes
 * using an {@code AbstractQueuedSynchronizer} for their
 * synchronization mechanics.
 *
 *
 * 使用场景：
 * 此类为同步器的基础，需要重新如下方法，另外可以使用{#getState}，{#setState}和{#compareAndSetState}来检查和/或修改同步状态来
 * <ul>
 * <li> {@link #tryAcquire}
 * <li> {@link #tryRelease}
 * <li> {@link #tryAcquireShared}
 * <li> {@link #tryReleaseShared}
 * <li> {@link #isHeldExclusively}
 * </ul>
 * 以上方法中都默认抛出{@link UnsupportedOperationException}。
 * 实现这些方法的必须保证是内部线程安全的，并且通常应该是短而不是阻塞的。所有其他方法都声明为final final，因为它们不能独立变化。
 *
 * 您还可以从{@link AbstractOwnableSynchronizer}中找到继承的方法，以便跟踪拥有独占同步器的线程。
 *
 * 尽管此类基于内部FIFO队列，但不会自动执行FIFO采集策略。 独占同步的核心形式如下：
 *
 * <pre>
 * Acquire:
 *     while (!tryAcquire(arg)) {
 *        <em>enqueue thread if it is not already queued</em>;
 *        <em>possibly block current thread</em>;
 *     }
 *
 * Release:
 *     if (tryRelease(arg))
 *        <em>unblock the first queued thread</em>;
 * </pre>
 *
 * (共享模式也是类似，但是可能涉及级联signal.)
 *
 *  因为获取中的检查在节点入队之前被调用，所以新的竞争线程可能会先于其他被阻塞和排队的线程获取锁。
 *  但是，您可以定义tryAcquire和/或tryAcquireShared方法，然后内部调用一个或多个检查方法，从而提供一个公平的FIFO获取顺序""
         * However, you can, if desired,
         * define {@code tryAcquire} and/or {@code tryAcquireShared} to
         * disable barging by internally invoking one or more of the inspection
         * methods, thereby providing a <em>fair</em> FIFO acquisition order.
 *  特别是，如果{#hasQueuedPredecessors}（专为公平同步器使用的方法）返回{true}，则大多数公平同步器可以定义{tryAcquire}以返回{false}。"
 *
 *  默认的barging的吞吐量和可扩展性（也称为greedy，renouncement和convoy-avoidance）通常是最高的。
 *  虽然这不能保证公平或无饥饿，但可以在稍后排队的线程之前重新排队早期排队的线程，并且每次调整都有一个无偏于的机会来抵御传入的线程。""而且，虽然获取不“旋转”""在通常意义上，它们可能会在阻塞之前执行多次调用{@code tryAcquire}以及其他计算。""当独占同步只是简单地进行时，这提供了自旋的大部分好处，而当没有大部分的责任时，则没有大部分的责任。""如果需要的话，您可以通过前面的调用来获取这些信息，以获取具有“快速路径”检查的方法，可能会预先检查{@link #hasContended}和/或{@link #hasQueuedThreads}，只有在同步器可能不会""被争辩。"
 *
 * <p>Throughput and scalability are generally highest for the
 * default barging (also known as <em>greedy</em>,
 * <em>renouncement</em>, and <em>convoy-avoidance</em>) strategy.
 * While this is not guaranteed to be fair or starvation-free, earlier
 * queued threads are allowed to recontend before later queued
 * threads, and each recontention has an unbiased chance to succeed
 * against incoming threads.  Also, while acquires do not
 * &quot;spin&quot; in the usual sense, they may perform multiple
 * invocations of {@code tryAcquire} interspersed with other
 * computations before blocking.  This gives most of the benefits of
 * spins when exclusive synchronization is only briefly held, without
 * most of the liabilities when it isn't. If so desired, you can
 * augment this by preceding calls to acquire methods with
 * "fast-path" checks, possibly prechecking {@link #hasContended}
 * and/or {@link #hasQueuedThreads} to only do so if the synchronizer
 * is likely not to be contended.
 *
 * 这个类为部分同步提供了一个高效的和可扩展的基础，通过将它的使用范围专门化到可以依赖于状态，获取和释放参数的同步器以及一个内部FIFO等待队列中。""当这还不够时，您可以使用{@link
 *  java.util.concurrent.atomic 原子类，您自己的自定义{@link java.util.Queue}类和{@link LockSupport}阻塞从较低级别构建同步器""支持
 *
 * <p>This class provides an efficient and scalable basis for
 * synchronization in part by specializing its range of use to
 * synchronizers that can rely on {@code int} state, acquire, and
 * release parameters, and an internal FIFO wait queue. When this does
 * not suffice, you can build synchronizers from a lower level using
 * {@link java.util.concurrent.atomic atomic} classes, your own custom
 * {@link java.util.Queue} classes, and {@link LockSupport} blocking
 * support.
 *
 * <h3>Usage Examples</h3>
 *
 * 这是一个不可重入的互斥锁类，它使用零值表示解锁状态，一个表示锁定状态。""尽管非重入锁并不严格要求记录当前所有者线程，但该类无论如何都会这样做，以便更易于监视使用。""它还支持条件并公开其中一种工具方法："
 * <p>Here is a non-reentrant mutual exclusion lock class that uses
 * the value zero to represent the unlocked state, and one to
 * represent the locked state. While a non-reentrant lock
 * does not strictly require recording of the current owner
 * thread, this class does so anyway to make usage easier to monitor.
 * It also supports conditions and exposes
 * one of the instrumentation methods:
 *
 *  <pre> {@code
 * class Mutex implements Lock, java.io.Serializable {
 *
 *   // Our internal helper class
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     // Reports whether in locked state
 *     protected boolean isHeldExclusively() {
 *       return getState() == 1;
 *     }
 *
 *     // Acquires the lock if state is zero
 *     public boolean tryAcquire(int acquires) {
 *       assert acquires == 1; // Otherwise unused
 *       if (compareAndSetState(0, 1)) {
 *         setExclusiveOwnerThread(Thread.currentThread());
 *         return true;
 *       }
 *       return false;
 *     }
 *
 *     // Releases the lock by setting state to zero
 *     protected boolean tryRelease(int releases) {
 *       assert releases == 1; // Otherwise unused
 *       if (getState() == 0) throw new IllegalMonitorStateException();
 *       setExclusiveOwnerThread(null);
 *       setState(0);
 *       return true;
 *     }
 *
 *     // Provides a Condition
 *     Condition newCondition() { return new ConditionObject(); }
 *
 *     // Deserializes properly
 *     private void readObject(ObjectInputStream s)
 *         throws IOException, ClassNotFoundException {
 *       s.defaultReadObject();
 *       setState(0); // reset to unlocked state
 *     }
 *   }
 *
 *   // The sync object does all the hard work. We just forward to it.
 *   private final Sync sync = new Sync();
 *
 *   public void lock()                { sync.acquire(1); }
 *   public boolean tryLock()          { return sync.tryAcquire(1); }
 *   public void unlock()              { sync.release(1); }
 *   public Condition newCondition()   { return sync.newCondition(); }
 *   public boolean isLocked()         { return sync.isHeldExclusively(); }
 *   public boolean hasQueuedThreads() { return sync.hasQueuedThreads(); }
 *   public void lockInterruptibly() throws InterruptedException {
 *     sync.acquireInterruptibly(1);
 *   }
 *   public boolean tryLock(long timeout, TimeUnit unit)
 *       throws InterruptedException {
 *     return sync.tryAcquireNanos(1, unit.toNanos(timeout));
 *   }
 * }}</pre>
 *
 * <p>这是一个闩锁类，就像一个{@link java.util.concurrent.CountDownLatch CountDownLatch}，只不过它只需要一个{@code signal}来触发。""因为锁存器是非排他性的，所以它使用{@code shared}获取和释放方法。
 * <p>Here is a latch class that is like a
 * {@link java.util.concurrent.CountDownLatch CountDownLatch}
 * except that it only requires a single {@code signal} to
 * fire. Because a latch is non-exclusive, it uses the {@code shared}
 * acquire and release methods.
 *
 *  <pre> {@code
 * class BooleanLatch {
 *
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     boolean isSignalled() { return getState() != 0; }
 *
 *     protected int tryAcquireShared(int ignore) {
 *       return isSignalled() ? 1 : -1;
 *     }
 *
 *     protected boolean tryReleaseShared(int ignore) {
 *       setState(1);
 *       return true;
 *     }
 *   }
 *
 *   private final Sync sync = new Sync();
 *   public boolean isSignalled() { return sync.isSignalled(); }
 *   public void signal()         { sync.releaseShared(1); }
 *   public void await() throws InterruptedException {
 *     sync.acquireSharedInterruptibly(1);
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {

    private static final long serialVersionUID = 7373984972572414691L;

    protected AbstractQueuedSynchronizer() { }

    /**
     * 等待队列节点类
     *
     * "等待队列是“CLH”（Craig，Landin和Hagersten）锁队列的变体。 ""CLH锁通常用于自旋锁。""我们将它们用于阻止同步器，但使用相同的基本策略来保存其节点前驱中某个线程的某些控制信息。""每个节点中的“状态”字段会跟踪线程是否应该被阻塞。""节点在其前一版本发布时发出信号。""队列中的每个节点都作为一个特定通知样式的监视器，持有一个等待线程。""状态字段不控制线程是否被授予锁等。""线程可能会尝试获取它是否在队列中第一个。""但首先并不能保证成功;""它只会给予抗争的权利。""所以当前发布的竞争者线程可能需要重新等待。"
     *
     * <p>The wait queue is a variant of a "CLH" (Craig, Landin, and
     * Hagersten) lock queue. CLH locks are normally used for
     * spinlocks.  We instead use them for blocking synchronizers, but
     * use the same basic tactic of holding some of the control
     * information about a thread in the predecessor of its node.  A
     * "status" field in each node keeps track of whether a thread
     * should block.  A node is signalled when its predecessor
     * releases.  Each node of the queue otherwise serves as a
     * specific-notification-style monitor holding a single waiting
     * thread. The status field does NOT control whether threads are
     * granted locks etc though.  A thread may try to acquire if it is
     * first in the queue. But being first does not guarantee success;
     * it only gives the right to contend.  So the currently released
     * contender thread may need to rewait.
     *
     * 为了排队进入CLH锁，你可以将它原子级地拼接成新的尾部。""为了出队，您只需设置头部字段
     *
     * <p>To enqueue into a CLH lock, you atomically splice it in as new
     * tail. To dequeue, you just set the head field.
     * <pre>
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * </pre>
     *
     * <p>Insertion into a CLH queue requires only a single atomic
     * operation on "tail", so there is a simple atomic point of
     * demarcation from unqueued to queued. Similarly, dequeuing
     * involves only updating the "head". However, it takes a bit
     * more work for nodes to determine who their successors are,
     * in part to deal with possible cancellation due to timeouts
     * and interrupts.
     *
     * <p>The "prev" links (not used in original CLH locks), are mainly
     * needed to handle cancellation. If a node is cancelled, its
     * successor is (normally) relinked to a non-cancelled
     * predecessor. For explanation of similar mechanics in the case
     * of spin locks, see the papers by Scott and Scherer at
     * http://www.cs.rochester.edu/u/scott/synchronization/
     *
     * <p>We also use "next" links to implement blocking mechanics.
     * The thread id for each node is kept in its own node, so a
     * predecessor signals the next node to wake up by traversing
     * next link to determine which thread it is.  Determination of
     * successor must avoid races with newly queued nodes to set
     * the "next" fields of their predecessors.  This is solved
     * when necessary by checking backwards from the atomically
     * updated "tail" when a node's successor appears to be null.
     * (Or, said differently, the next-links are an optimization
     * so that we don't usually need a backward scan.)
     *
     * <p>Cancellation introduces some conservatism to the basic
     * algorithms.  Since we must poll for cancellation of other
     * nodes, we can miss noticing whether a cancelled node is
     * ahead or behind us. This is dealt with by always unparking
     * successors upon cancellation, allowing them to stabilize on
     * a new predecessor, unless we can identify an uncancelled
     * predecessor who will carry this responsibility.
     *
     * <p>CLH queues need a dummy header node to get started. But
     * we don't create them on construction, because it would be wasted
     * effort if there is never contention. Instead, the node
     * is constructed and head and tail pointers are set upon first
     * contention.
     *
     * <p>Threads waiting on Conditions use the same nodes, but
     * use an additional link. Conditions only need to link nodes
     * in simple (non-concurrent) linked queues because they are
     * only accessed when exclusively held.  Upon await, a node is
     * inserted into a condition queue.  Upon signal, the node is
     * transferred to the main queue.  A special value of status
     * field is used to mark which queue a node is on.
     *
     * <p>Thanks go to Dave Dice, Mark Moir, Victor Luchangco, Bill
     * Scherer and Michael Scott, along with members of JSR-166
     * expert group, for helpful ideas, discussions, and critiques
     * on the design of this class.
     */
    static final class Node {
        /** 标记：在共享模式下，处于等待状态的节点 */
        static final Node SHARED = new Node();
        /** 标记：在排他模式下，处于等待状态的节点 */
        static final Node EXCLUSIVE = null;

        /** 节点等待状态： value to indicate thread has cancelled */
        static final int CANCELLED =  1;
        /** 节点等待状态： 后继者的线程需要被执行*/
        static final int SIGNAL    = -1;
        /** 节点等待状态：表明此节点正在条件队列中排队 */
        static final int CONDITION = -2;
        /**
         * 节点等待状态：指示下一个acquireShared应无条件传播
         *
         */
        static final int PROPAGATE = -3;

        /**
         * waitStatus 取值说明：
         *   SIGNAL: 因为当前节点的后继者正在被阻塞或者将要被通过park命令阻塞，所以当前需要执行释放或取消操作
         * 当它被unpark退出阻塞时。为了避免竞争，acquire方法会首先表明他们需要一个signal，然后执行原子获取，然后如果失败
         * 则被阻塞
         *
         *   CANCELLED: 表示当前的节点因为timeout 或 interrupt才进入此状态，节点永远不会再变成其它状态，尤其对应的线程不会再变成阻塞状态
         *
         *   CONDITION: 指示当前的节点处于condition queue中。此节点不会在同步队列中使用，直到节点被transferred，此时节点的状态设置为0
         *
         *   PROPAGATE: A releaseShared应该被传播给其它节点。此状态只能设置在head节点，并且通过doReleaseShared设置，从而保证传播继续，即使有其它操作进行干扰
         *
         *   0: 不是以上的状态
         *
         * 非负值意味着节点不需要被signal，这意味大部分代码不需要检查特定的值，只需要执行signal
         *
         * 初始值：
         *  在normal sync nodes，此值为0
         *  在condition nodes中，此值为在CONDITION
         *
         */
        volatile int waitStatus;

        /**
         * 前驱节点
         *
         *  在执行enqueuing方法时分配节点的pre值，在只执行dequeuing设置pre值为空
         *
         *
         *
         * 执行添加时，有不理解的可以看看这个
         *
         * 另外，在
                   *取消前任，我们短路的同时
                   *找到一个永不存在的未取消的人
                   *因为头节点永远不会被取消：节点变成
                   *仅因成功获得而取得成果。 一个
                   *取消的线程永远不会成功获取，并且只有一个线程
                   *取消自己，而不是任何其他节点。

         *
         * Link to predecessor node that current node/thread relies on
         * for checking waitStatus. Assigned during enqueuing, and nulled
         * out (for sake of GC) only upon dequeuing.  Also, upon
         * cancellation of a predecessor, we short-circuit while
         * finding a non-cancelled one, which will always exist
         * because the head node is never cancelled: A node becomes
         * head only as a result of successful acquire. A
         * cancelled thread never succeeds in acquiring, and a thread only
         * cancels itself, not any other node.
         */
        volatile Node prev;

        /**
         * 后继节点
         *
         * 执行添加时，有不理解的可以看看这个
         *
         * Link to the successor node that the current node/thread
         * unparks upon release. Assigned during enqueuing, adjusted
         * when bypassing cancelled predecessors, and nulled out (for
         * sake of GC) when dequeued.  The enq operation does not
         * assign next field of a predecessor until after attachment,
         * so seeing a null next field does not necessarily mean that
         * node is at end of queue. However, if a next field appears
         * to be null, we can scan prev's from the tail to
         * double-check.  The next field of cancelled nodes is set to
         * point to the node itself instead of null, to make life
         * easier for isOnSyncQueue.
         */
        volatile Node next;

        /**
         *
         * 此节点的对应的线程
         *
         */
        volatile Thread thread;

        /**
         * 指向在condition queue中等待的节点，或者特殊值SHARED
         *
         * condition queue只能使用排他模式访问，所有此节点通过一个简单链接队列。此队列中的节点通过re-acquire可以转移到队列中
         *
         */
        Node nextWaiter;

        /**
         * 如果使用提共享模式，则返回true
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * 返回前驱节点
         */
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        Node() {    // Used to establish initial head or SHARED marker
        }

        Node(Thread thread, Node mode) {     // Used by addWaiter
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) { // Used by Condition
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    /**
     * 头节点
     * 除了初始化，只能通过setHead方法修改
     *
     */
    private transient volatile Node head;

    /**
     * 尾节点
     * 除了初始化，只能通过enq方法修改
     *
     */
    private transient volatile Node tail;

    /**
     * 同步状态，变量必须带volatile语义
     */
    private volatile int state;

    /**
     * 返回同步状态
     *
     */
    protected final int getState() {
        return state;
    }

    /**
     * 设置同步状态
     */
    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * 通过CAS原子的设置同步状态
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that the actual
     *         value was not equal to the expected value.
     */
    protected final boolean compareAndSetState(int expect, int update) {
        // See below for intrinsics setup to support this
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // Queuing utilities

    /**
     * 设置自旋的纳秒数。对于非常短的超时，自旋要比使用park方法要响应快
     *
     */
    static final long spinForTimeoutThreshold = 1000L;

    /**
     * 将节点插入到队列中，如果队列不存在，则先初始队列
     *
     * @param node the node to insert
     * @return node's predecessor(前驱节点)
     */
    private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            if (t == null) { // Must initialize
                if (compareAndSetHead(new Node())) // 头节点是空节点，不是有线程等待的节点
                    tail = head;
            } else {
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }

    /**
     * 根据当前的线程和模式创建节点并插入到队列中
     *
     * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
     * @return the new node
     */
    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        enq(node);
        return node;
    }

    /**
     * 设置队列head值，只能被acquire方法调用
     *
     * @param node the node
     */
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    /**
     * 如果当前节点有后继节点，则唤醒它
     *
     * @param node the node
     */
    private void unparkSuccessor(Node node) {
        /*
         * 如果节点状态为负数（如值为signal），尝试清楚预期发signal信号
         * 如果失败或状态由于等待线程而改变，也是可以的
         *
         */
        int ws = node.waitStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        /*
         * 唤醒后续者里的线程，通常是下一个节点。
         * 如果后续节点的状态为cancelled或null，则从队列的尾巴开始向前找，直到找到一个非cancelled状态的后续者
         *
         */
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        if (s != null)
            LockSupport.unpark(s.thread);
    }

    /**
     * 共享模式下的Release操作：signals后续节点并且保证传播
     * 对于排他模式，如果后续者的状态为signal，则Release就是upark head节点的后继者，
     */
    private void doReleaseShared() {
        /*
         * 保证release可以被传播，即使有其它的线程中执行acquires/releases。
         * 如果头节点的状态为SIGNAL，则这个方法会尝试调用unparkSuccessor方法处理头节点
         * 如果不是，则需要设置状态为PROPAGATE，保证release之后PROPAGATE能够继续
         *
         * 另外，还需要通过循环保证在执行上面逻辑过程中有新的结点加入。
         * 如果在通过CAS设置失败后需要重新检查
         *
         */
        for (;;) {
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases
                    unparkSuccessor(h);
                }
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // loop on failed CAS
            }
            if (h == head)                   // loop if head changed
                break;
        }
    }

    /**
     * 将node设置为队列的头，如果传入结点的后续者处于共享模式，且(传入的参数propagate > 0 或 新/旧head节点的状态为PROPAGATE)
     * ，则执行传播
     *
     * @param node the node
     * @param propagate the return value from a tryAcquireShared
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // Record old head for check below
        setHead(node);
        /*
         *  如果满足下如下两个条件，则signal队列中的下一个节点
         *      1. 如果下一个节点处于共享模式或下一个节点为null
         *      2. 传入的参数propagate > 0 或 (原head节点为null或状态<0) 或 (新的头节点 null 或 状态 < 0)
         */
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
            (h = head) == null || h.waitStatus < 0) {
            Node s = node.next;
            if (s == null || s.isShared())
                doReleaseShared();
        }
    }

    // Utilities for various versions of acquire

    /**
     * 取消当前正在进行的acquire操作
     *
     * @param node the node
     */
    private void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        if (node == null)
            return;

        node.thread = null;

        // 路过状态为cancelled的前驱者
        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary.
        Node predNext = pred.next;

        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        node.waitStatus = Node.CANCELLED;

        // 如果传入的节点是tail，则将这个节点从队列删除并设置此节点的前驱节点设置为tail
        if (node == tail && compareAndSetTail(node, pred)) {
            // 将pre节点设置为新的tail,通过CAS将其的next成员变量设置为null
            compareAndSetNext(pred, predNext, null);
        } else {
            /**
             * 满足同时如下条件，则需要设置前驱节点的pre值,如果传入节点的next不为空且状态<0
             *  1. 前驱节点不是头节点
             *  2. 前驱节点里的线程不为空
             *  3. 前驱节点的状态为SIGNAL或(状态<0且CAS设置值SIGNAL成功)
             *
             * 否则：唤醒传入节点的后续节点进行处理
             *
             */
            int ws;
            if (pred != head &&
                ((ws = pred.waitStatus) == Node.SIGNAL ||
                 (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                pred.thread != null) {
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(pred, predNext, next);
            } else {
                unparkSuccessor(node);
            }
            // 此节点从队列中删除后，同时设置它的next为空
            node.next = node; // help GC
        }
    }

    /**
     * 如果一个节点acquire失败后,调用此方法设置状态为SIGNAL
     *
     * 两个参数的关系:Requires that pred == node.prev.
     *
     * @param pred node's predecessor holding status
     * @param node the node
     * @return {@code true} if thread should block
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            /*
             * 前驱节点已经设置为SIGNAL，可以安全被park了
             */
            return true;
        if (ws > 0) {
            /*
             * 如果前驱状态为cancelled，则跳过，直到找到非cancelled节点
             */
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            /*
             * 设置节点值为 SIGNAL
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             */
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    /**
     * 中断当前的线程
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * park当前的线程，然后检查当前的线程是否被中断
     */
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    /*
     * Various flavors of acquire, varying in exclusive/shared and
     * control modes.  Each is mostly the same, but annoyingly
     * different.  Only a little bit of factoring is possible due to
     * interactions of exception mechanics (including ensuring that we
     * cancel if tryAcquire throws exception) and other control, at
     * least not without hurting performance too much.
     */

    /**
     * 抢资源
     *
     * 如果节点的线程有被中断，则返回值为true
     *
     * Acquires in exclusive uninterruptible mode for thread already in
     * queue. Used by condition wait methods as well as acquire.
     *
     */
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true; // 标记是否没有拿到资源
        try {
            boolean interrupted = false; // 标记线程等待过程中是否被中断过
            for (;;) {
                final Node p = node.predecessor();
                // 如果前驱节点是head，即该结点已成老二，那么尝试获取资源
                if (p == head && tryAcquire(arg)) {
                    // 拿到资源后，将head指向该结点
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                // 如果一个节点acquire失败后,调用此方法设置状态为SIGNAL
                // 然后设置SIGNAL成功，则阻塞当前线程，直到被唤醒
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    // 如果等待过程中被中断过，就将interrupted标记为true
                    interrupted = true;
            }
        } finally {
            if (failed)
                // 取消当前正在进行的acquire操作
                cancelAcquire(node);
        }
    }

    /**
     * 在排它中断模式下，获取资源方法
     */
    private void doAcquireInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * 排他定时模式获取锁
     * 如果获取成功，则返回true
     *
     * 相对于tryAcquire，此方法会阻塞当前线程进行等待。
     * 此方法实际也是调用tryAcquire来获取锁
     *
     */
    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        // 将节点插入到队列中，并设置模式为排他性
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                // 如果前驱节点为head，则尝试获取
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                // 如果一个节点acquire失败后,调用此方法设置状态为SIGNAL，如果时间未超时，则park当前线程
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * 共享非中断模式获取锁
     *   和doAcquireSharedInterruptibly不同之处，如果被中断，则自己中断自己
     */
    private void doAcquireShared(int arg) {
        // 将节点插入到队列中，并设置模式为共享
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                // 如果前驱节点为head，则尝试获取
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * 在共享非中断模式下获取锁
     *  和doAcquireShared不同之处，如果被中断，则抛出异常
     */
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * 共享模式下，在指定时间内获取锁
     * 如果成功，则返回true
     *
     */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return true;
                    }
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    // Main exported methods

    /**
     * 在排他模式下，尝试获取锁
     * 此方法应查询对象的状态是否允许在排他模式下获取它，如果是，则获取它
     *
     * 此方法总是由执行获取的线程调用。 如果此方法报告失败，则获取方法可以将该线程排队（如果该线程尚未排队），直到通过来自其他某个线程的发布发出信号。
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 在排他模式下，尝试设置状态为release
     *
     * 此方法总是由执行获取的线程调用。
     *
     *  如果对象已经被完全释放，则返回true。
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     *
     */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 在共享模式下，尝试获取锁
     * 此方法应查询对象的状态是否允许在共享模式下获取它，如果是，则获取它
     *
     *  此方法总是由执行获取的线程调用。 如果此方法报告失败，则获取方法可以将该线程排队（如果该线程尚未排队），直到通过来自其他某个线程的发布发出信号。
     *
     *  返回结果：
     *      a. 负数表示执行失败
     *      b. 0：在共享模式下，获取锁成功，但是接下来，其它线程获取共享锁会失败
     *      c. 正数：在共享模式下，获取锁成功，但是接下来，其它线程也可能获取共享锁会成功
     */
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 在共享模式下，尝试设置状态为释放
     *
     * 此方法总是由执行获取的线程调用。
     *
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this release of shared mode may permit a
     *         waiting acquire (shared or exclusive) to succeed; and
     *         {@code false} otherwise
     *
     */
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 如果同步器被当前调用线程独占，则返回true
     *
     * 在ConditionObject中的非等待方法会调用此方法，等待方法会调用release有一份
     *
     * 此方法内部只被ConditionObject的方法调用，所以如果没有使用conditions，则不需要定义此方法
     *
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    /**
     * 在排他模式、忽略中断模式下获取锁
     * 第一次尝试调用tryAcquire，如果成功，则返回success。
     * 如果失败，则将线程入队，在线程成功获取锁前，可以重复被阻塞和接触阻塞
     *
     * 这个方法用于实现Lock#lock
     *
     */
    public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }

    /**
     * 在排他模式、如果当前线程被中断就抛出异常模式下获取锁
     *
     * 使用tryAcquire尝试获取锁，如果成功，则返回
     * 否则则将线程入队，在线程成功获取锁或线程被中断前，可以重复被阻塞和接触阻塞
     *
     * 这个方法可以被用于实现Lock#lockInterruptibly
     *
     */
    public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (!tryAcquire(arg))
            doAcquireInterruptibly(arg);
    }

    /**
     *  在排他模式、如果当前线程被中断就抛出异常、超时模式下获取锁
     *
     *  实现逻辑：
     *  首先确认当前线的中断状态，如果被中断则抛出异常
     *  然后调用tryAcquire，如果成功，则返回
     *  如果失败，则否则则将线程入队，在线程成功获取锁或线程被中断或超时前，可以重复被阻塞和解除阻塞
     *
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquire(arg) ||
            doAcquireNanos(arg, nanosTimeout);
    }

    /**
     * 排他模式下的释放锁操作
     * 如果有为一个或多个线程解除阻塞，则返回true
     *
     * 此方法用于实现Lock#unlock
     *
     */
    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }

    /**
     * 在共享模式、忽略中断下获取锁
     *
     * 首先调用tryAcquireShared获取锁，如果成功则返回
     * 如果失败，则否则则将线程入队，在调用tryAcquireShared成功前，可以重复被阻塞和解除阻塞
     *
     */
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }

    /**
     * 在共享、如果当前线程被中断抛出异常模式下获取锁
     *
     */
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        // 首先调用tryAcquireShared获取锁，如果成功则返回
        if (tryAcquireShared(arg) < 0)
            // 如果失败，则否则则将线程入队，doAcquireSharedInterruptibly，可以重复被阻塞和解除阻塞
            doAcquireSharedInterruptibly(arg);
    }

    /**
     * 在共享模式、如果当前线程被中断就抛出异常、超时模式下获取锁
     *
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        // 首先调用tryAcquireShared获取锁，如果成功则返回
        // 如果失败，则将线程入队，在线程成功获取锁或线程被中断或超时前，可以重复被阻塞和解除阻塞
        return tryAcquireShared(arg) >= 0 ||
            doAcquireSharedNanos(arg, nanosTimeout);
    }

    /**
     *
     * 共享模式下的释放锁操作
     *
     * 如果有为一个或多个线程解除阻塞，则返回true
     *
     */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

    // Queue inspection methods

    /**
     * 查询队列是否可能有线程在等待获取锁，
     * 如果有，则返回true
     *
     * Note that
     * because cancellations due to interrupts and timeouts may occur
     * at any time, a {@code true} return does not guarantee that any
     * other thread will ever acquire.
     *
     * @return {@code true} if there may be other threads waiting to acquire
     */
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     *
     * 查询是否有线程曾争夺过这个同步器; 即是否acquire方法曾经被阻塞
     * 如果是，则返回true
     *
     */
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * 获取队列中的第一个线程（等待时间最长）。如果队列为空，则返回null
     *
     */
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * Version of getFirstQueuedThread called when fastpath fails
     */
    private Thread fullGetFirstQueuedThread() {
        /*
         * The first node is normally head.next. Try to get its
         * thread field, ensuring consistent reads: If thread
         * field is nulled out or s.prev is no longer head, then
         * some other thread(s) concurrently performed setHead in
         * between some of our reads. We try this twice before
         * resorting to traversal.
         */
        Node h, s;
        Thread st;
        if (((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null) ||
            ((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null))
            return st;

        /*
         * Head's next field might not have been set yet, or may have
         * been unset after setHead. So we must check to see if tail
         * is actually first node. If not, we continue on, safely
         * traversing from tail back to head to find first,
         * guaranteeing termination.
         */

        Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)
                firstThread = tt;
            t = t.prev;
        }
        return firstThread;
    }

    /**
     * 如果指定的线程中队列中，则返回true
     *
     */
    public final boolean isQueued(Thread thread) {
        if (thread == null)
            throw new NullPointerException();
        for (Node p = tail; p != null; p = p.prev)
            if (p.thread == thread)
                return true;
        return false;
    }

    /**
     * 以下情况返回true:
     *  第一个结点存在且正在独占模式下等待，返回true
     *
     *  如果此方法返回true，然后当前线程试图以共享模式获取（即，此方法从{#tryAcquireShared}调用），则确保当前线程不是第一个排队线程。
     *
     *  此方法仅用作ReentrantReadWriteLock中的启发式。
     *
     * Returns {@code true} if the apparent first queued thread, if one
     * exists, is waiting in exclusive mode.  If this method returns
     * {@code true}, and the current thread is attempting to acquire in
     * shared mode (that is, this method is invoked from {@link
     * #tryAcquireShared}) then it is guaranteed that the current thread
     * is not the first queued thread.  Used only as a heuristic in
     * ReentrantReadWriteLock.
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null &&
            (s = h.next)  != null &&
            !s.isShared()         &&
            s.thread != null;
    }

    /**
     * 查询队列中是否有比当前线程等待时间更长的线程
     *
     * 注意：
     *  由于中断和超时有可能导致任务被取消会发生在任何时候，返回true不能保证其它线程会比当前线程早获取锁
     *  同时，由于队列为空，所以在此方法返回{false}时，另一个线程有可能先获取锁
     *
     * 此方法被公平锁使用，以避免AbstractQueuedSynchronizer#barging的闯入
     * 此类同步器的{#tryAcquire}方法应返回false代码，并且其{#tryAcquireShared}方法应返回一个负值。
     * 只有这是一个可重入获取，此方法返回{true}
     *
     * 返回值:
     *  true：表示在等待队列中有比当前线程排在更前面的线程
     *  false: 当前线程排在队列的头位置或队列为空
     * @return {@code true} if there is a queued thread preceding the
     *         current thread, and {@code false} if the current thread
     *         is at the head of the queue or the queue is empty
     * @since 1.7
     */
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;
        return h != t &&
            ((s = h.next) == null || s.thread != Thread.currentThread());
    }


    // Instrumentation and monitoring methods

    /**
     * 返回等待队列中节点的估值数量
     * 此方法用于监视system state，不是为了同步控制
     *
     */
    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread != null)
                ++n;
        }
        return n;
    }

    /**
     * 返回在队列中等待的线程。
     * 因为队列中的线程是动态变化的，返回的线程列表只是尽力而为的估计。
     *
     * This method is designed to facilitate construction of subclasses that provide more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
        return list;
    }

    /**
     * 在排它模式下，返回可能在等待队列中的节点列表
     *  因为队列中的线程是动态变化的，返回的线程列表只是尽力而为的估计。
     *
     *  同#getQueuedThreads
     */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     *  在共享模式下，返回可能在等待队列中的节点列表
     *      因为队列中的线程是动态变化的，返回的线程列表只是尽力而为的估计。
     *
     *  同：#getQueuedThreads
     *
     */
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a string identifying this synchronizer, as well as its state.
     * The state, in brackets, includes the String {@code "State ="}
     * followed by the current value of {@link #getState}, and either
     * {@code "nonempty"} or {@code "empty"} depending on whether the
     * queue is empty.
     *
     * @return a string identifying this synchronizer, as well as its state
     */
    public String toString() {
        int s = getState();
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
            "[State = " + s + ", " + q + "empty queue]";
    }


    // Internal support methods for Conditions

    /**
     *  如果指定节点正在同步队列并等待重新获取，则返回true。
     *  这个节点最初总是放在条件队列(a condition queue)中
     */
    final boolean isOnSyncQueue(Node node) {
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;
        if (node.next != null) // If has successor, it must be on queue
            // 如果有后此节点有后继节点，则肯定在队列中
            return true;
        /*
         * node.prev can be non-null, but not yet on queue because
         * the CAS to place it on queue can fail. So we have to
         * traverse from tail to make sure it actually made it.  It
         * will always be near the tail in calls to this method, and
         * unless the CAS failed (which is unlikely), it will be
         * there, so we hardly ever traverse much.
         */
        return findNodeFromTail(node);
    }

    /**
     * 从尾巴开始查找这个节点是否在队列中，如果是，则返回true
     * 只能被isOnSyncQueue方法调用
     *
     */
    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (;;) {
            if (t == node)
                return true;
            if (t == null)
                return false;
            t = t.prev;
        }
    }

    /**
     * 将节点从条件队列转移到同步队列，如果成功，则返回true
     *
     */
    final boolean transferForSignal(Node node) {
        /*
         * 如果设置节点状态失败，则表示此节点已经被取消
         */
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        /*
         * 将节点插入到队列中，设置节点的前驱节点的状态为Node.SIGNAL以指示线程（可能）在等待
         * 如果节点被取消或尝试设置waitStatus失败，则唤醒此节点重新同步
         *
         */
        // 将节点插入到队列中，如果队列不存在，则先初始队列，并返回前驱节点
        Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * 自己的理解：
     *  转移节点，如果节点状态为CONDITION且设置节点的状态为0，如果成功，则将节点插入到同步队列中。（后续condition队列会删除状态为0的记录）
     *  如果执行了将节点插入到同步队列中，则返回true.
     *
     * Transfers node, if necessary, to sync queue after a cancelled wait.
     * Returns true if thread was cancelled before being signalled.
     *
     * @param node the node
     * @return true if cancelled before the node was signalled
     */
    final boolean transferAfterCancelledWait(Node node) {
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            // 设置节点的状态为0成功，则插入到队列中
            enq(node);
            return true;
        }
        /*
         * 如果我们失去了一个signal()，那么我们就不能继续下去，直到它完成它的enq()。
         * 在incomplete transfer过程中执行取消既罕见又短暂，所以使用自旋
         *
         */
        while (!isOnSyncQueue(node))
            Thread.yield(); // yield
        return false;
    }

    /**
     * 对当前的锁进行释放，如果失败，则设置当前节点状态为CANCELLED，并抛出异常
     * 使用当前的state值调用release方法，如果成功此方法会返回保存的state值
     * 如果失败，设置当前节点状态为CANCELLED，并抛出异常
     *
     * @param node the condition node for this wait
     * @return previous sync state
     */
    final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            int savedState = getState();
            // 排他模式下的释放锁操作
            if (release(savedState)) {
                failed = false;
                return savedState;
            } else {
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed)
                node.waitStatus = Node.CANCELLED;
        }
    }

    // Instrumentation methods for conditions: conditions的仪表方法

    /**
     * 查询给定的ConditionObject中是否使用本同步器，如果是，则返回true
     *
     * @param condition the condition
     *
     */
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * 查询指定的condition下面有无线程在等待执行。condition和当前的同步器关联
     *
     * 注意：因为超时、中断任何时候都会发生，返回true并不保证将来执行signal会唤醒任何线程
     * 此方法主要用于监视system state
     *
     */
    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.hasWaiters();
    }

    /**
     * 查询指定的condition下面有线程在等待执行的数量。condition和当前的同步器关联
     * 注意：因为超时、中断任何时候都会发生，返回的值只是估值
     * 此方法主要用于监视system state，不用于同步控制
     *
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }

    /**
     * 查询指定的condition下面等待执行线程的列表。condition和当前的同步器关联
     * 注意：因为超时、中断任何时候都会发生，返回的值只是估值
     * 此方法主要用于监视system state，不用于同步控制
     *
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitingThreads();
    }

    /**
     * 此Condition类是AbstractQueuedSynchronizer的Lock的基础实现
     *
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        /** condition队列中的第一个节点 */
        private transient Node firstWaiter;
        /** condition队列中的最后一个节点. */
        private transient Node lastWaiter;

        /**
         * Creates a new {@code ConditionObject} instance.
         */
        public ConditionObject() { }

        // Internal methods

        /**
         * 添加一个新的等待节点到condition等待队列中末尾，并返回这个节点
         *
         */
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            // 如果最后一个节点是cancelled状态，则清除它.
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter; // 设置新的最后一个节点
            }
            // 创建一个等待队列
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            if (t == null)
                firstWaiter = node;
            else
                t.nextWaiter = node;
            lastWaiter = node;
            return node;
        }

        /**
         * 从condition队列中将头节点删除，并将此节点转移到同步队列中。
         * 循环队列操作，直到节点转移到同步队列成功或condition队列为空
         *
         * @param first condition队列中的非空头节点
         */
        private void doSignal(Node first) {
            do {
                if ( (firstWaiter = first.nextWaiter) == null) // 将头节点的下一个节点设置为头节点
                    lastWaiter = null; // 如果firstWaiter节点为null，则lastWaiter肯定也为空
                first.nextWaiter = null; //　设置一下节点的字段为空
            } while (!transferForSignal(first) && // 将节点从条件队列转移到同步队列，如果成功，则结束
                     (first = firstWaiter) != null); // 如果队列为空，则结束
        }

        /**
         * 将所有结点从condition中移除，并尝试将所有节点移到等待队列
         *
         * @param first condition队列中的非空头节点
         */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                transferForSignal(first); // 将节点从条件队列转移到同步队列，如果成功，则返回true
                first = next;
            } while (first != null);
        }

        /**
         * 从condition队列中删除状态为cancelled的等待节点
         * 只有在持有锁的时候才可以调用此方法
         *
         * 【
         * 此方法在取消节点操作发生期间以及新的等待节点增加之前调用。
         * 在没有signal信号的情况下，需要使用此方法来避免垃圾留存
         * 所以即使它可能需要全部遍历，它只有在没有信号的情况下发生超时或取消时才会发挥作用。
         * 它遍历所有节点，而不是停止在特定目标上，以便在取消风暴期间不需要许多重新遍历就可以将所有指向垃圾节点的链接解除链接。
         * 】
         *
         * This is called when
         * cancellation occurred during condition wait, and upon
         * insertion of a new waiter when lastWaiter is seen to have
         * been cancelled. This method is needed to avoid garbage
         * retention in the absence of signals. So even though it may
         * require a full traversal, it comes into play only when
         * timeouts or cancellations occur in the absence of
         * signals. It traverses all nodes rather than stopping at a
         * particular target to unlink all pointers to garbage nodes
         * without requiring many re-traversals during cancellation
         * storms.
         */
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter; // 指向本次需要检查的节点
            Node trail = null; // 指向上一个状态为CONDITION的节点
            while (t != null) { // 队列非空
                Node next = t.nextWaiter;
                if (t.waitStatus != Node.CONDITION) { // 当前节点为cancelled状态的节点
                    t.nextWaiter = null;
                    if (trail == null) // 如果trail为空，表示当前正在处理的节点为头节点，需要重新设置新的节点
                        firstWaiter = next;
                    else
                        trail.nextWaiter = next; // 如果trail非空，则需要将上一个处理的节点的nextWaiter指向下一个节点
                    if (next == null)
                        lastWaiter = trail; // 如果没有一下节点，则已经到达最后一个节点，循环结束
                }
                else
                    trail = t; // 指向上一个状态为CONDITION的节点，值
                t = next; // 循环一下节点
            }
        }

        // public methods

        /**
         * 将等候时间最长的线程（如果存在）从等待队列转移到拥有锁的等待队列中
         *
         */
        public final void signal() {
            if (!isHeldExclusively()) // 如果同步器被当前调用线程独占，则返回true
                throw new IllegalMonitorStateException(); //
            Node first = firstWaiter;
            if (first != null)
                doSignal(first); // 从condition队列中将头节点删除，并将此节点转移到同步队列中。
        }

        /**
         * 将等待队列中的所有线程转移到拥有锁的等待队列。
         *
         */
        public final void signalAll() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignalAll(first);
        }

        /**
         * 无中断的condition等待
         *  - 保存由getState方法返回的lock状态
         *  - 使用saved state作为release的参数并调用，如果失败则抛出IllegalMonitorStateException
         *  - 阻塞直到被signal
         *  - 通通调用特定版本的acquire方法来进行重新获取，参数为saved state
         *
         */
        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter(); // 添加一个新的等待节点到等待队列中
            int savedState = fullyRelease(node); // 释放当前的节点，如果成功此方法会返回保存的state值
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                // 如果指定节点不在同步队列中，则
                LockSupport.park(this);
                if (Thread.interrupted())
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted)
                selfInterrupt();
        }

        /*
         * For interruptible waits, we need to track whether to throw
         * InterruptedException, if interrupted while blocked on
         * condition, versus reinterrupt current thread, if
         * interrupted while blocked waiting to re-acquire.
         */

        /** Mode meaning to reinterrupt on exit from wait */
        private static final int REINTERRUPT =  1;
        /** Mode meaning to throw InterruptedException on exit from wait */
        private static final int THROW_IE    = -1;

        /**
         * 检查中断：
         *  如果在signal之前被中断，则返回THROW_IE
         *  如果在signal之后被中断，则返回REINTERRUPT
         *  如果没有被中断，则返回0
         */
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                0;
        }

        /**
         * 如果中断模式为THROW_IE，则抛出InterruptedException
         * 如果中断模式为REINTERRUPT，则线程执行自己中断自己
         */
        private void reportInterruptAfterWait(int interruptMode)
            throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }

        /**
         * 实现可中断的condition wait.
         * <ol>
         * <li> 如果当前的线程被中断，则抛出throw InterruptedException.
         * <li> 保存由#getState方法返回的锁state.
         * <li> 使用上面的锁state作为参数调用#release方法，如果失败，则返回IllegalMonitorStateException
         * <li> 阻塞当前线程直到线程被唤醒或中断
         * <li> 使用上面的锁state作为参数调用#acquire方法重新获取锁
         * <li> 在第四步，在阻塞时被中断，则抛出 InterruptedException.
         * </ol>
         */
        public final void await() throws InterruptedException {
            // 如果当前的线程被中断，则抛出throw InterruptedException.
            if (Thread.interrupted())
                throw new InterruptedException();
            // 添加一个新的等待节点到condition等待队列中
            Node node = addConditionWaiter();
            // 对当前的锁进行释放，如果失败，则设置当前节点状态为CANCELLED，并抛出异常
            int savedState = fullyRelease(node);
            int interruptMode = 0;

            while (!isOnSyncQueue(node)) {  // 如果指定节点不在同步队列，则进入循环。直到当前当前节点被放入等待队列
                LockSupport.park(this); // 阻塞当前线程
                // 线程被唤醒后检查中断的状态，如果发到节点已经被中断，则break
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) // 如果没有被中断，则返回0
                    break;
            }
            // 开始在等待队列中排除,并进行抢锁
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;  // 获取资源失败，则返回false,表示线程被中断
            if (node.nextWaiter != null) // clean up if cancelled
                unlinkCancelledWaiters(); // 从condition队列中删除状态为cancelled的等待节点
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode); // 对两种中断的情况进行处理
        }

        /**
         * 超时版本的await
         * Implements timed condition wait.
         * <ol>
         * <li> 如果当前的线程被中断，则抛出throw InterruptedException.
         * <li> 保存由#getState方法返回的锁state.
         * <li> 使用上面的锁state作为参数调用#release方法，如果失败，则返回IllegalMonitorStateException
         * <li> 阻塞当前线程直到线程被唤醒或中断或超时
         * <li> 使用上面的锁state作为参数调用#acquire方法重新获取锁
         * <li> 在第四步，在阻塞时被中断，则抛出 InterruptedException.
         * </ol>
         * 返回值为剩余的等待时间
         *
         */
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            // 如果当前的线程被中断，则抛出throw InterruptedException.
            if (Thread.interrupted())
                throw new InterruptedException();
            // 添加一个新的等待节点到condition等待队列中
            Node node = addConditionWaiter();
            // 对当前的锁进行释放，如果失败，则设置当前节点状态为CANCELLED，并抛出异常
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {   // 如果指定节点不在同步队列，则进入循环。直到当前当前节点被放入等待队列或超时
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }

        /**
         * 直到指定时间超时版本的await
         *
         * Implements absolute timed condition wait.
         * <ol>
         * <li> 如果当前的线程被中断，则抛出throw InterruptedException.
         * <li> 保存由#getState方法返回的锁state.
         * <li> 使用上面的锁state作为参数调用#release方法，如果失败，则返回IllegalMonitorStateException
         * <li> 阻塞当前线程直到线程被唤醒或中断或超时
         * <li> 使用上面的锁state作为参数调用#acquire方法重新获取锁
         * <li> 在第四步，在阻塞时被中断，则抛出 InterruptedException.
         * <li> 在第四步，在阻塞时被超时，则返回 false，否则返回true
         * </ol>
         *
         */
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        //  support for instrumentation

        /**
         * 如果condition是被指定的同步对象创建，则返回true
         *
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        /**
         * 查询是否有线程等待在这个condition下面，如果是，则返回true
         */
        protected final boolean hasWaiters() {
            // 如果同步器被当前调用线程独占，则返回true
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) // 找到节点的状态为CONDITION
                    return true;
            }
            return false;
        }

        /**
         * 获取在condition中等待节点数量的估值
         *
         * Implements {@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}.
         *
         */
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    ++n;
            }
            return n;
        }

        /**
         * 返回所有可能在condition队列上等待的节点
         *
         */
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }

    /**
     * Setup to support compareAndSet. We need to natively implement
     * this here: For the sake of permitting future enhancements, we
     * cannot explicitly subclass AtomicInteger, which would be
     * efficient and useful otherwise. So, as the lesser of evils, we
     * natively implement using hotspot intrinsics API. And while we
     * are at it, we do the same for other CASable fields (which could
     * otherwise be done with atomic field updaters).
     */
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * CAS head field. Used only by enq.
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS waitStatus field of a node.
     */
    private static final boolean compareAndSetWaitStatus(Node node,
                                                         int expect,
                                                         int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                                        expect, update);
    }

    /**
     * CAS next field of a node.
     */
    private static final boolean compareAndSetNext(Node node,
                                                   Node expect,
                                                   Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
