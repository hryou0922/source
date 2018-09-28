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

import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.PlatformDependent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 *
 * Big endian Java堆缓冲区实现
 * 建议使用UnpooledByteBufAllocator#heapBuffer(int, int)、Unpooled#buffer(int)和Unpooled#wrappedBuffer(byte[])，而不是显式调用构造函数。
 *
 ==========
 此类是基于堆内存进行内存分配的字节缓冲区，它没有基于对象池技术实现，这意味着每次I/O的读写都会创建一个新UnplooledHeapByteBuf，频繁进行大块内存的分配和回收对性能会造成一定影响，但是相比于堆 外内存的申请和释放，它的成本还是会低一些

 相比于PooledByteBuf，UnplooledHeapByteBuf的实现原理更加简单，也不容易出现内存管理方法的问题，因此在满足性能的情况下，推荐使用UnplooledHeapByteBuf


 */
public class UnpooledHeapByteBuf extends AbstractReferenceCountedByteBuf {

    /**
     * 它聚合了一个ByteBufAllocator，用于UnpooledHeapByteBuf的内存分配，紧接着定义了一个byte数组作为缓冲区，最后定义了一个ByteBuf类型和tmpNioBuf变量用于实现Netty ByteBuf到JDK NIO ByteBuffer的转换
     使用JDK的ByteBuffer替换byte数组也是可行的，直接使用byte数组的根本原因就是提升性能和更加便捷地进行位操作。JDK的ByteBuffer底层实现也是byte数组
     */
    private final ByteBufAllocator alloc;
    byte[] array;
    private ByteBuffer tmpNioBuf;

    /**
     * Creates a new heap buffer with a newly allocated byte array.
     *
     * @param initialCapacity the initial capacity of the underlying byte array
     * @param maxCapacity the max capacity of the underlying byte array
     */
    public UnpooledHeapByteBuf(ByteBufAllocator alloc, int initialCapacity, int maxCapacity) {
        super(maxCapacity);

        checkNotNull(alloc, "alloc");

        if (initialCapacity > maxCapacity) {
            throw new IllegalArgumentException(String.format(
                    "initialCapacity(%d) > maxCapacity(%d)", initialCapacity, maxCapacity));
        }

        this.alloc = alloc;
        setArray(allocateArray(initialCapacity));
        setIndex(0, 0);
    }

    /**
     * Creates a new heap buffer with an existing byte array.
     *
     * @param initialArray the initial underlying byte array
     * @param maxCapacity the max capacity of the underlying byte array
     */
    protected UnpooledHeapByteBuf(ByteBufAllocator alloc, byte[] initialArray, int maxCapacity) {
        super(maxCapacity);

        checkNotNull(alloc, "alloc");
        checkNotNull(initialArray, "initialArray");

        if (initialArray.length > maxCapacity) {
            throw new IllegalArgumentException(String.format(
                    "initialCapacity(%d) > maxCapacity(%d)", initialArray.length, maxCapacity));
        }

        this.alloc = alloc;
        setArray(initialArray);
        setIndex(0, initialArray.length);
    }

    protected byte[] allocateArray(int initialCapacity) {
        return new byte[initialCapacity];
    }

    protected void freeArray(byte[] array) {
        // NOOP
    }

    private void setArray(byte[] initialArray) {
        array = initialArray;
        tmpNioBuf = null;
    }

    @Override
    public ByteBufAllocator alloc() {
        return alloc;
    }

    @Override
    public ByteOrder order() {
        return ByteOrder.BIG_ENDIAN;
    }

    /**
     * 如果基于堆内存实现的ByteBuf，它返回false
     */
    @Override
    public boolean isDirect() {
        return false;
    }

    @Override
    public int capacity() {
        return array.length;
    }

    /**

     动态扩展缓冲区
     1. 对新容量进行合法性校验，如果大于容量上限或小于0，则抛出IllegalArgumentException
     2. 判断新的容量值是否大于当前的缓冲区，如果大于则需要进行动态扩展，通过 byte[] newArray = new byte[newCapacity] 创建新的缓冲区字节数组，然后通过System.arraycopy进行内存复制，将旧的字节数组复制到新创建的字节数组中，然后调用setArray替换旧的字节数组

     4. 如果新的容量小于当前的缓冲区容量不需要动态进行扩展，但是需要截取当前缓冲区创建一个新的子缓冲区：
         a. 首先判断下读索引是否小于新的容量值，如果小于进一步判断写索引是否大于新的容量值，如果大于则将写索引设置为新容量值（防止越界）
         b. 更新完成写索引之后通过内存身后System.arraycopy将当前可读的字节数组复制到新创建的子缓冲区中
     5. 当动态扩容完成后，需要将原先的视图tmpNioBuf设置为空
     *
     */
    @Override
    public ByteBuf capacity(int newCapacity) {
        checkNewCapacity(newCapacity);

        int oldCapacity = array.length;
        byte[] oldArray = array;
        if (newCapacity > oldCapacity) {
            byte[] newArray = allocateArray(newCapacity);
            System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
            setArray(newArray);
            freeArray(oldArray);
        } else if (newCapacity < oldCapacity) {
            byte[] newArray = allocateArray(newCapacity);
            int readerIndex = readerIndex();
            if (readerIndex < newCapacity) {
                int writerIndex = writerIndex();
                if (writerIndex > newCapacity) {
                    writerIndex(writerIndex = newCapacity);
                }
                System.arraycopy(oldArray, readerIndex, newArray, readerIndex, writerIndex - readerIndex);
            } else {
                setIndex(newCapacity, newCapacity);
            }
            setArray(newArray);
            freeArray(oldArray);
        }
        return this;
    }

    /**
     * 由于UnpooledHeapByteBuf基于字节数组实现，所有它返回值主true
     */
    @Override
    public boolean hasArray() {
        return true;
    }

    /**
     * 由于UnpooledHeapByteBuf基于字节数组实现，所以它的返回值是内部的字节数组成功变量
     */
    @Override
    public byte[] array() {
        ensureAccessible();
        return array;
    }

    @Override
    public int arrayOffset() {
        return 0;
    }

    @Override
    public boolean hasMemoryAddress() {
        return false;
    }

    @Override
    public long memoryAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        checkDstIndex(index, length, dstIndex, dst.capacity());
        if (dst.hasMemoryAddress()) {
            PlatformDependent.copyMemory(array, index, dst.memoryAddress() + dstIndex, length);
        } else if (dst.hasArray()) {
            getBytes(index, dst.array(), dst.arrayOffset() + dstIndex, length);
        } else {
            dst.setBytes(dstIndex, array, index, length);
        }
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        checkDstIndex(index, length, dstIndex, dst.length);
        System.arraycopy(array, index, dst, dstIndex, length);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuffer dst) {
        checkIndex(index, dst.remaining());
        dst.put(array, index, dst.remaining());
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
        ensureAccessible();
        out.write(array, index, length);
        return this;
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        ensureAccessible();
        return getBytes(index, out, length, false);
    }

    @Override
    public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
        ensureAccessible();
        return getBytes(index, out, position, length, false);
    }

    private int getBytes(int index, GatheringByteChannel out, int length, boolean internal) throws IOException {
        ensureAccessible();
        ByteBuffer tmpBuf;
        if (internal) {
            tmpBuf = internalNioBuffer();
        } else {
            tmpBuf = ByteBuffer.wrap(array);
        }
        return out.write((ByteBuffer) tmpBuf.clear().position(index).limit(index + length));
    }

    private int getBytes(int index, FileChannel out, long position, int length, boolean internal) throws IOException {
        ensureAccessible();
        ByteBuffer tmpBuf = internal ? internalNioBuffer() : ByteBuffer.wrap(array);
        return out.write((ByteBuffer) tmpBuf.clear().position(index).limit(index + length), position);
    }

    @Override
    public int readBytes(GatheringByteChannel out, int length) throws IOException {
        checkReadableBytes(length);
        int readBytes = getBytes(readerIndex, out, length, true);
        readerIndex += readBytes;
        return readBytes;
    }

    @Override
    public int readBytes(FileChannel out, long position, int length) throws IOException {
        checkReadableBytes(length);
        int readBytes = getBytes(readerIndex, out, position, length, true);
        readerIndex += readBytes;
        return readBytes;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        checkSrcIndex(index, length, srcIndex, src.capacity());
        if (src.hasMemoryAddress()) {
            PlatformDependent.copyMemory(src.memoryAddress() + srcIndex, array, index, length);
        } else  if (src.hasArray()) {
            setBytes(index, src.array(), src.arrayOffset() + srcIndex, length);
        } else {
            src.getBytes(srcIndex, array, index, length);
        }
        return this;
    }

    /**
     * 首先是合法性校验
     调用System.arraycopy进行字节数组的复制
     */
    @Override
    public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        checkSrcIndex(index, length, srcIndex, src.length);
        System.arraycopy(src, srcIndex, array, index, length);
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuffer src) {
        ensureAccessible();
        src.get(array, index, src.remaining());
        return this;
    }

    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        ensureAccessible();
        return in.read(array, index, length);
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        ensureAccessible();
        try {
            return in.read((ByteBuffer) internalNioBuffer().clear().position(index).limit(index + length));
        } catch (ClosedChannelException ignored) {
            return -1;
        }
    }

    @Override
    public int setBytes(int index, FileChannel in, long position, int length) throws IOException {
        ensureAccessible();
        try {
            return in.read((ByteBuffer) internalNioBuffer().clear().position(index).limit(index + length), position);
        } catch (ClosedChannelException ignored) {
            return -1;
        }
    }

    @Override
    public int nioBufferCount() {
        return 1;
    }

    /**
     转换JDK ByteBuffer
     因为ByteBuf基于byte数组实现，NIO的ByteBuffer提供wrap方法，可以将byte数组转换成ByteBuffer对象
     UnpooledHeapByteBuf唯一不同的是它还设备ByteBuffer的slice方法。由于每次调用nioBuffer都会创建一个新的ByteBuffer，因此此处的slice方法起不到重用缓冲区内容的效果，只能保证读写索引的独立性
     */
    @Override
    public ByteBuffer nioBuffer(int index, int length) {
        ensureAccessible();
        return ByteBuffer.wrap(array, index, length).slice();
    }

    @Override
    public ByteBuffer[] nioBuffers(int index, int length) {
        return new ByteBuffer[] { nioBuffer(index, length) };
    }

    @Override
    public ByteBuffer internalNioBuffer(int index, int length) {
        checkIndex(index, length);
        return (ByteBuffer) internalNioBuffer().clear().position(index).limit(index + length);
    }

    @Override
    public byte getByte(int index) {
        ensureAccessible();
        return _getByte(index);
    }

    @Override
    protected byte _getByte(int index) {
        return HeapByteBufUtil.getByte(array, index);
    }

    @Override
    public short getShort(int index) {
        ensureAccessible();
        return _getShort(index);
    }

    @Override
    protected short _getShort(int index) {
        return HeapByteBufUtil.getShort(array, index);
    }

    @Override
    public short getShortLE(int index) {
        ensureAccessible();
        return _getShortLE(index);
    }

    @Override
    protected short _getShortLE(int index) {
        return HeapByteBufUtil.getShortLE(array, index);
    }

    @Override
    public int getUnsignedMedium(int index) {
        ensureAccessible();
        return _getUnsignedMedium(index);
    }

    @Override
    protected int _getUnsignedMedium(int index) {
        return HeapByteBufUtil.getUnsignedMedium(array, index);
    }

    @Override
    public int getUnsignedMediumLE(int index) {
        ensureAccessible();
        return _getUnsignedMediumLE(index);
    }

    @Override
    protected int _getUnsignedMediumLE(int index) {
        return HeapByteBufUtil.getUnsignedMediumLE(array, index);
    }

    @Override
    public int getInt(int index) {
        ensureAccessible();
        return _getInt(index);
    }

    @Override
    protected int _getInt(int index) {
        return HeapByteBufUtil.getInt(array, index);
    }

    @Override
    public int getIntLE(int index) {
        ensureAccessible();
        return _getIntLE(index);
    }

    @Override
    protected int _getIntLE(int index) {
        return HeapByteBufUtil.getIntLE(array, index);
    }

    @Override
    public long getLong(int index) {
        ensureAccessible();
        return _getLong(index);
    }

    @Override
    protected long _getLong(int index) {
        return HeapByteBufUtil.getLong(array, index);
    }

    @Override
    public long getLongLE(int index) {
        ensureAccessible();
        return _getLongLE(index);
    }

    @Override
    protected long _getLongLE(int index) {
        return HeapByteBufUtil.getLongLE(array, index);
    }

    @Override
    public ByteBuf setByte(int index, int value) {
        ensureAccessible();
        _setByte(index, value);
        return this;
    }

    @Override
    protected void _setByte(int index, int value) {
        HeapByteBufUtil.setByte(array, index, value);
    }

    @Override
    public ByteBuf setShort(int index, int value) {
        ensureAccessible();
        _setShort(index, value);
        return this;
    }

    @Override
    protected void _setShort(int index, int value) {
        HeapByteBufUtil.setShort(array, index, value);
    }

    @Override
    public ByteBuf setShortLE(int index, int value) {
        ensureAccessible();
        _setShortLE(index, value);
        return this;
    }

    @Override
    protected void _setShortLE(int index, int value) {
        HeapByteBufUtil.setShortLE(array, index, value);
    }

    @Override
    public ByteBuf setMedium(int index, int   value) {
        ensureAccessible();
        _setMedium(index, value);
        return this;
    }

    @Override
    protected void _setMedium(int index, int value) {
        HeapByteBufUtil.setMedium(array, index, value);
    }

    @Override
    public ByteBuf setMediumLE(int index, int   value) {
        ensureAccessible();
        _setMediumLE(index, value);
        return this;
    }

    @Override
    protected void _setMediumLE(int index, int value) {
        HeapByteBufUtil.setMediumLE(array, index, value);
    }

    @Override
    public ByteBuf setInt(int index, int   value) {
        ensureAccessible();
        _setInt(index, value);
        return this;
    }

    @Override
    protected void _setInt(int index, int value) {
        HeapByteBufUtil.setInt(array, index, value);
    }

    @Override
    public ByteBuf setIntLE(int index, int   value) {
        ensureAccessible();
        _setIntLE(index, value);
        return this;
    }

    @Override
    protected void _setIntLE(int index, int value) {
        HeapByteBufUtil.setIntLE(array, index, value);
    }

    @Override
    public ByteBuf setLong(int index, long  value) {
        ensureAccessible();
        _setLong(index, value);
        return this;
    }

    @Override
    protected void _setLong(int index, long value) {
        HeapByteBufUtil.setLong(array, index, value);
    }

    @Override
    public ByteBuf setLongLE(int index, long  value) {
        ensureAccessible();
        _setLongLE(index, value);
        return this;
    }

    @Override
    protected void _setLongLE(int index, long value) {
        HeapByteBufUtil.setLongLE(array, index, value);
    }

    @Override
    public ByteBuf copy(int index, int length) {
        checkIndex(index, length);
        byte[] copiedArray = new byte[length];
        System.arraycopy(array, index, copiedArray, 0, length);
        return new UnpooledHeapByteBuf(alloc(), copiedArray, maxCapacity());
    }

    private ByteBuffer internalNioBuffer() {
        ByteBuffer tmpNioBuf = this.tmpNioBuf;
        if (tmpNioBuf == null) {
            this.tmpNioBuf = tmpNioBuf = ByteBuffer.wrap(array);
        }
        return tmpNioBuf;
    }

    @Override
    protected void deallocate() {
        freeArray(array);
        array = EmptyArrays.EMPTY_BYTES;
    }

    @Override
    public ByteBuf unwrap() {
        return null;
    }
}
