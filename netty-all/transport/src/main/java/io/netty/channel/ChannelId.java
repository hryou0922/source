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

package io.netty.channel;

import java.io.Serializable;

/**
 * 表示 Channel 的全局唯一标识符。
 *
 * ID由以下变量生成：
 *  - MAC地址(EUI-48 or EUI-64) 或 网络适配器，最好是全球唯一的，
 *  - 当前进程ID
 *  - System#currentTimeMillis()
 *  - System#nanoTime()
 *  - a random 32-bit integer
 *  - 一个顺序递增的32位整数
 *
 * The global uniqueness of the generated identifier mostly depends on the MAC address and the current process ID,
 * which are auto-detected at the class-loading time in best-effort manner.  If all attempts to acquire them fail,
 * a warning message is logged, and random values will be used instead.  Alternatively, you can specify them manually
 * via system properties:
 * <ul>
 * <li>{@code io.netty.machineId} - hexadecimal representation of 48 (or 64) bit integer,
 *     optionally separated by colon or hyphen.</li>
 * <li>{@code io.netty.processId} - an integer between 0 and 65535</li>
 * </ul>
 * </p>
 */
public interface ChannelId extends Serializable, Comparable<ChannelId> {
    /**
     * Returns the short but globally non-unique string representation of the {@link ChannelId}.
     */
    String asShortText();

    /**
     * Returns the long yet globally unique string representation of the {@link ChannelId}.
     */
    String asLongText();
}
