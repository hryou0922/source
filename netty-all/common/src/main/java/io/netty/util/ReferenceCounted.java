/*
 * Copyright 2013 The Netty Project
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
package io.netty.util;

/**
 * 需要显式释放的引用计数对象(A reference-counted object that requires explicit deallocation.)。
 *
 * 当实例化新的ReferenceCounted时，它引用计数以1开始。调用{@link #retain()}增加引用计数;{@link #release()}减少引用计数;
 * 如果引用计数减少到0，则将显式释放该对象，并且访问解除分配的对象通常会导致访问冲突
 *
 * 如果一个实现{@link ReferenceCounted}的对象，是个包含{@link ReferenceCounted}对象的容器，
 * 当容器的引用计数变为0时，包含的对象也将通过{@link #release（）}释放
 * <p>
 * If an object that implements {@link ReferenceCounted} is a container of other objects that implement
 * {@link ReferenceCounted}, the contained objects will also be released via {@link #release()} when the container's
 * reference count becomes 0.
 * </p>
 *
 */
public interface ReferenceCounted {
    // 返回这个对象的引用计数。如值为0，表示此对象已经被释放
    int refCnt();

    // 对象的引用计数加1
    ReferenceCounted retain();

    // 对象的引用计数增加指定的值
    ReferenceCounted retain(int increment);

    /**
     * Records the current access location of this object for debugging purposes.
     * If this object is determined to be leaked, the information recorded by this operation will be provided to you
     * via {@link ResourceLeakDetector}.  This method is a shortcut to {@link #touch(Object) touch(null)}.
     */
    ReferenceCounted touch();

    /**
     * Records the current access location of this object with an additional arbitrary information for debugging
     * purposes.  If this object is determined to be leaked, the information recorded by this operation will be
     * provided to you via {@link ResourceLeakDetector}.
     */
    ReferenceCounted touch(Object hint);

    // 引用计数减1，如果引用计数变为0，则释放此对象
    // 只有当且当引用计数变为0且释放此对象，此时返回true
    boolean release();

    // 引用计数减少指定值，如果引用计数变为0，则释放此对象
    // 只有当且当引用计数变为0且释放此对象，此时返回true
    boolean release(int decrement);
}
