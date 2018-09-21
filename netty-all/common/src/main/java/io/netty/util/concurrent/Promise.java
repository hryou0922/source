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
package io.netty.util.concurrent;

/**
 * 特殊的Future，支持writable
 */
public interface Promise<V> extends Future<V> {

    /**
     * 标记futrue成功并且通知所有的监听者
     * 如果future已经被标记成功或失败，则会抛出异常IllegalStateException
     */
    Promise<V> setSuccess(V result);

    /**
     * 标记futrue成功并且通知所有的监听者
     * 有且只能标记此future为成功，才返回true。其它返回false: 包括futrue已经标记过了（失败或成功）
     */
    boolean trySuccess(V result);

    /**
     * 标记futrue失败并且通知所有的监听者
     * 如果future已经被标记成功或失败，则会抛出异常IllegalStateException
     */
    Promise<V> setFailure(Throwable cause);

    /**
     * 标记futrue失败并且通知所有的监听者
     * 有且只能标记此future为失败，才返回true。其它返回false: 包括futrue已经标记过了（失败或成功）
     */
    boolean tryFailure(Throwable cause);

    /**
     * 标记此future无法被取消
     * 如果此future成功被标记无法被取消或已经被标记为无法被取消，则返回true
     * 如果此future已经被取消，则返回false
     * Make this future impossible to cancel.
     *
     * @return {@code true} if and only if successfully marked this future as uncancellable or it is already done
     *         without being cancelled.  {@code false} if this future has been cancelled already.
     */
    boolean setUncancellable();

    @Override
    Promise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    Promise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    Promise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    Promise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    Promise<V> await() throws InterruptedException;

    @Override
    Promise<V> awaitUninterruptibly();

    @Override
    Promise<V> sync() throws InterruptedException;

    @Override
    Promise<V> syncUninterruptibly();
}
