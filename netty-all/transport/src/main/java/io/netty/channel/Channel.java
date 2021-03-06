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
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeMap;

import java.net.InetSocketAddress;
import java.net.SocketAddress;


/**

 Netty网络操作抽象类，它聚合了以下功能：
 1. 提供chanel当前的状态（channel打开、连接）
 2. channel的配置参数ChannelConfig(如接收buffer大小)
 3. 支持通道I/O操作，如读、写、连接和绑定
 4. ChannelPipeline处理所有和channel相关所有的I/O事件

 所有的I/O操作都是异步：这意味所有的I/O调用会立刻返回并且不保证I/O操作已经完成当方法返回时。建议开发者方法返回ChannelFutrue实例用于通知I/O操作的是否成功、失败或取消

 Chaneels是分层次的：Channel可以有一个parent。对于SocketChannel，如果被ServerSockerChannel创建，则parent是ServerSockerChannel
 释放资源：调用Channel的close()释放资源

 */
public interface Channel extends AttributeMap, ChannelOutboundInvoker, Comparable<Channel> {

    // 返回全球唯一Channel id
    ChannelId id();

    // 返回Channel注册的EventLoop
    EventLoop eventLoop();

    // 返回channel的parent.Channel可以有一个parent。对于SocketChannel，如果被ServerSockerChannel创建，则parent是ServerSockerChannel
    Channel parent();

    // 返回channel的配置对象
    ChannelConfig config();

    // 如果Channel打开，则返回true
    boolean isOpen();

    // 如果Channel被注册到EventLoop，则返回true
    boolean isRegistered();

    // 如果Channel处于活动状态且有连接，则返回true
    boolean isActive();

    // 返回Channel上的ChannelMetadata，ChannelMetadata用于描述Channel
    ChannelMetadata metadata();

    // 返回绑定在本channel上的local address（如果channel绑定，则返回null）
    // 返回的SocketAddress可以向下转化为InetSocketAddress对象，从而获取更详细信息
    SocketAddress localAddress();

    // 返回绑定在本Channel上的remote address（如果channel绑定，则返回null。）
    // 返回的SocketAddress可以向下转化为InetSocketAddress对象，从而获取更详细信息
    /**
     *
     * @return the remote address of this channel.
     *         {@code null} if this channel is not connected.
     *         If this channel is not connected but it can receive messages
     *         from arbitrary remote addresses (e.g. {@link DatagramChannel},
     *         use {@link DatagramPacket#recipient()} to determine
     *         the origination of the received message as this method will
     *         return {@code null}.
     */
    SocketAddress remoteAddress();

    // 返回ChannelFuture，当channel被关闭时，此ChannelFuture会被通知
    // This method always returns the same future instance.
    ChannelFuture closeFuture();

    // 当且仅当I / O线程立即执行请求的写操作时，返回true。
    // 当此方法返回false时发出的任何写入请求都会排队，直到I / O线程准备好处理排队的写入请求。
    boolean isWritable();

    /**
     * 返回在调用isWritable()返回false，之前最多可以写入多少字节，返回值为非负数
     * 如果isWritable()为false，此值返回0
     */
    long bytesBeforeUnwritable();

    /**
     * Get how many bytes must be drained from underlying buffers until {@link #isWritable()} returns {@code true}.
     * This quantity will always be non-negative. If {@link #isWritable()} is {@code true} then 0.
     */
    long bytesBeforeWritable();

    // 返回一个内部专用对象，用于提供unsafe操作
    Unsafe unsafe();

    // 返回关联的ChannelPipeline
    ChannelPipeline pipeline();

    // 返回指定的{@link ByteBufAllocator}，用于分配ByteBuf。
    ByteBufAllocator alloc();

    @Override
    Channel read();

    @Override
    Channel flush();

    /**
     * Unsafe操作不允许被用户的代码调用.
     * 这些方法只用于实现实际的transport,必须被IO线程调用.
     * 以下方法是例外
     * <ul>
     *   <li>{@link #localAddress()}</li>
     *   <li>{@link #remoteAddress()}</li>
     *   <li>{@link #closeForcibly()}</li>
     *   <li>{@link #register(EventLoop, ChannelPromise)}</li>
     *   <li>{@link #deregister(ChannelPromise)}</li>
     *   <li>{@link #voidPromise()}</li>
     * </ul>
     */
    interface Unsafe {

        // 返回指定的RecvByteBufAllocator.Handle,用于分配接收数据的ByteBuf
        RecvByteBufAllocator.Handle recvBufAllocHandle();

        // 返回本地的SocketAddress,如果没有则为null
        SocketAddress localAddress();

        // 返回远端的SocketAddress,如果没有则为null
        SocketAddress remoteAddress();

        // 注册ChannelPromise上的Channel，并在注册完成后通知ChannelFuture。
        void register(EventLoop eventLoop, ChannelPromise promise);

        /**
         * Bind the {@link SocketAddress} to the {@link Channel} of the {@link ChannelPromise} and notify
         * it once its done.
         */
        // 绑定SocketAddress到ChannelPromise的Channel上，完成后并通知它
        void bind(SocketAddress localAddress, ChannelPromise promise);

        /**
         * Connect the {@link Channel} of the given {@link ChannelFuture} with the given remote {@link SocketAddress}.
         * If a specific local {@link SocketAddress} should be used it need to be given as argument. Otherwise just
         * pass {@code null} to it.
         *
         * The {@link ChannelPromise} will get notified once the connect operation was complete.
         */
        void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);

        // 断开ChannelPromise的Channel,当完成后需要通知ChannelPromise
        void disconnect(ChannelPromise promise);

        // 关闭ChannelPromise的Channel,当完成后通知ChannelPromise
        void close(ChannelPromise promise);

        // 立即关闭Channel,并且不触发任务事件.可能只在注册失败有用
        void closeForcibly();

        // 将ChannelPromise上的Channel从EventLoop上反注册,当完成后,通知ChannelPromise
        void deregister(ChannelPromise promise);

        /**
         * Schedules a read operation that fills the inbound buffer of the first {@link ChannelInboundHandler} in the
         * {@link ChannelPipeline}.  If there's already a pending read operation, this method does nothing.
         */
        // 安排ChannelPipeline里的第一个ChannelInboundHandler填
        void beginRead();

        // 安排写操作
        void write(Object msg, ChannelPromise promise);

        /**
         * Flush out all write operations scheduled via {@link #write(Object, ChannelPromise)}.
         */
        void flush();

        /**
         * Return a special ChannelPromise which can be reused and passed to the operations in {@link Unsafe}.
         * It will never be notified of a success or error and so is only a placeholder for operations
         * that take a {@link ChannelPromise} as argument but for which you not want to get notified.
         */
        ChannelPromise voidPromise();

        /**
         * Returns the {@link ChannelOutboundBuffer} of the {@link Channel} where the pending write requests are stored.
         */
        ChannelOutboundBuffer outboundBuffer();
    }
}
