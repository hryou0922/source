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
package io.netty.handler.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.internal.TypeParameterMatcher;

import java.util.List;

/**
 * ChannelInboundHandlerAdapter 将一个message编码为另一个message
 *
 * For example here is an implementation which decodes a {@link String} to an {@link Integer} which represent
 * the length of the {@link String}.
 *
 * <pre>
 *     public class StringToIntegerDecoder extends
 *             {@link MessageToMessageDecoder}<{@link String}> {
 *
 *         {@code @Override}
 *         public void decode({@link ChannelHandlerContext} ctx, {@link String} message,
 *                            List<Object> out) throws {@link Exception} {
 *             out.add(message.length());
 *         }
 *     }
 * </pre>
 *
 * Be aware that you need to call {@link ReferenceCounted#retain()} on messages that are just passed through if they
 * are of type {@link ReferenceCounted}. This is needed as the {@link MessageToMessageDecoder} will call
 * {@link ReferenceCounted#release()} on decoded messages.
 *
 */
public abstract class MessageToMessageDecoder<I> extends ChannelInboundHandlerAdapter {

    private final TypeParameterMatcher matcher;

    /**
     * Create a new instance which will try to detect the types to match out of the type parameter of the class.
     */
    protected MessageToMessageDecoder() {
        matcher = TypeParameterMatcher.find(this, MessageToMessageDecoder.class, "I");
    }

    /**
     * Create a new instance
     *
     * @param inboundMessageType    The type of messages to match and so decode
     */
    protected MessageToMessageDecoder(Class<? extends I> inboundMessageType) {
        matcher = TypeParameterMatcher.get(inboundMessageType);
    }

    /**
     * Returns {@code true} if the given message should be handled. If {@code false} it will be passed to the next
     * {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     */
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return matcher.match(msg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 先通过CodecOutputList创建一个新的可循环利用的CodecOutputList,然后对解码的消息类型进行判断,通过类型参数校验器看是否是可接收的类型,如果是则校验通过
        CodecOutputList out = CodecOutputList.newInstance();
        try {
            if (acceptInboundMessage(msg)) {
                @SuppressWarnings("unchecked")
                I cast = (I) msg;
                try {
                    // 校验通过之后,直接调用decode抽象方法,由具体实现子类进行消息解码
                    decode(ctx, cast, out);
                } finally {
                    // 解码完成之后,调用ReferenceCountUtil的release方法来释放被解码的msg对象
                    ReferenceCountUtil.release(cast);
                }
            } else {
                // 如果需要解码的对象不是当前解码器可以接收和处理的类型,则将它加入到CodecOutputList中不进行解码。
                out.add(msg);
            }
        } catch (DecoderException e) {
            throw e;
        } catch (Exception e) {
            throw new DecoderException(e);
        } finally {
            // 最后,对CodecOutputList进行遍历,循环调用ChannelHandlerContext的fireChannelRead方法,通知后续的ChannelHandler继续进行处理。循环通知完成之后,通过recycle方法释放CodecOutputList对象。
            int size = out.size();
            for (int i = 0; i < size; i ++) {
                ctx.fireChannelRead(out.getUnsafe(i));
            }
            out.recycle();
        }
    }

    /**
     * Decode from one message to an other. This method will be called for each written message that can be handled
     * by this encoder.
     *
     * @param ctx           the {@link ChannelHandlerContext} which this {@link MessageToMessageDecoder} belongs to
     * @param msg           the message to decode to an other one
     * @param out           the {@link List} to which decoded messages should be added
     * @throws Exception    is thrown if an error occurs
     */
    protected abstract void decode(ChannelHandlerContext ctx, I msg, List<Object> out) throws Exception;
}
