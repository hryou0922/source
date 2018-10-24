/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.buffer;

/**
 * 此类负责分配缓冲区。 此接口的实现应该是线程安全的。
 */
public interface ByteBufAllocator {

    ByteBufAllocator DEFAULT = ByteBufUtil.DEFAULT_ALLOCATOR;

    /**
     * 分配一个ByteBuf. 缓冲区的类型（a direct or heap buffer）由ByteBufAllocator的实现类决定
     */
    ByteBuf buffer();

    /**
     * 分配一个初始容量为initialCapacity的ByteBuf,缓冲区的类型(a direct or heap buffer)由ByteBufAllocator的实现类决定
     */
    ByteBuf buffer(int initialCapacity);

    /**
     * 分配一个初始容量为initialCapacity,最大容量为maxCapacity的ByteBuf,缓冲区的类型（a direct or heap buffer）由ByteBufAllocator的实现类决定
     */
    ByteBuf buffer(int initialCapacity, int maxCapacity);

    /**
     * Allocate a {@link ByteBuf}, preferably a direct buffer which is suitable for I/O.
     */
    ByteBuf ioBuffer();

    /**
     * Allocate a {@link ByteBuf}, preferably a direct buffer which is suitable for I/O.
     */
    ByteBuf ioBuffer(int initialCapacity);

    /**
     * 分配一个初始容量为initialCapacity,最大容量为maxCapacity的ByteBuf， 缓冲区使用direct buffer ,因为directbuffer的IO操作性能更高
     */
    ByteBuf ioBuffer(int initialCapacity, int maxCapacity);

    /**
     * Allocate a heap {@link ByteBuf}.
     */
    ByteBuf heapBuffer();

    /**
     * Allocate a heap {@link ByteBuf} with the given initial capacity.
     */
    ByteBuf heapBuffer(int initialCapacity);

    /**
     * 分配一个初始容量为initialCapacity,最大容量为maxCapacity的 heap ByteBuf
     */
    ByteBuf heapBuffer(int initialCapacity, int maxCapacity);

    /**
     * Allocate a direct {@link ByteBuf}.
     */
    ByteBuf directBuffer();

    /**
     * Allocate a direct {@link ByteBuf} with the given initial capacity.
     */
    ByteBuf directBuffer(int initialCapacity);

    /**
     * 分配一个初始容量为initialCapacity,最大容量为maxCapacity的direct ByteBuf
     */
    ByteBuf directBuffer(int initialCapacity, int maxCapacity);

    /**
     * Allocate a {@link CompositeByteBuf}.
     * If it is a direct or heap buffer depends on the actual implementation.
     */
    CompositeByteBuf compositeBuffer();

    /**
     * 分配一个最大容量为maxCapacity的CompositeByteBuf,内存类型（a direct or heap buffer）由ByteBufAllocator的实现类决定是否使用了直接内存内存池
     */
    CompositeByteBuf compositeBuffer(int maxNumComponents);

    /**
     * Allocate a heap {@link CompositeByteBuf}.
     */
    CompositeByteBuf compositeHeapBuffer();

    /**
     * Allocate a heap {@link CompositeByteBuf} with the given maximum number of components that can be stored in it.
     */
    CompositeByteBuf compositeHeapBuffer(int maxNumComponents);

    /**
     * Allocate a direct {@link CompositeByteBuf}.
     */
    CompositeByteBuf compositeDirectBuffer();

    /**
     * Allocate a direct {@link CompositeByteBuf} with the given maximum number of components that can be stored in it.
     */
    CompositeByteBuf compositeDirectBuffer(int maxNumComponents);

    /**
     * Returns {@code true} if direct {@link ByteBuf}'s are pooled
     */
    boolean isDirectBufferPooled();

    /**
     * Calculate the new capacity of a {@link ByteBuf} that is used when a {@link ByteBuf} needs to expand by the
     * {@code minNewCapacity} with {@code maxCapacity} as upper-bound.
     *
     * AbstractByteBufAllocator的实现原理:
     *  首先需要重新设计下扩展后的容量（== writeIndex + minWirteableBytes），也就是满足要求的最小的容量
     *  首先设置阈值为 4M，当需要的新容量正好等于阈值时，则使用阈值作为新的缓冲区容量
     *  如果新申请的内存空间大于阈值，不能采用倍增的方式（防止内存膨胀和浪费）扩张内存，采用每次步进4M的方式进行内存扩张。
     *  扩张的时候需要对扩张后的内存和最大内存进行比较，如果大于缓冲区的最大长度，则使用maxCapacity作为扩容后的缓冲区容量
     *  如果扩容后的新容量小于阈值，则以64为计数进行倍增，直到倍增后的结果大于或等于需要的容量值
     *
     *  采用倍增的原因：见<Netty权威指南> p335
     *  采用先倍增后步进行的原因：见<Netty权威指南> p335
     *
     */
    int calculateNewCapacity(int minNewCapacity, int maxCapacity);
 }
