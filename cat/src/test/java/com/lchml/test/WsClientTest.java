package com.lchml.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by huangrongyou@yixin.im on 2018/9/17.
 */
public class WsClientTest {

    public static void main(String[] args) {
        Executor executor = Executors.newCachedThreadPool();
        int num = 1;
        while(num-- > 0) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    staticWsclient();
                }
            });
        }
    }

    private static void staticWsclient() {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap boot = new Bootstrap();
        try {
            boot.option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_BACKLOG, 1024 * 1024 * 10)
                    .group(group)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline p = socketChannel.pipeline();
                            p.addLast(new HttpServerCodec());
                            p.addLast(new HttpObjectAggregator(1024 * 1024 * 10));
                            p.addLast(new WebSocketServerCompressionHandler());
                            // 处理websocket的 这个处理程序为您运行websocket服务器做了所有繁重的工作。
                            p.addLast(new WebSocketServerProtocolHandler("/webcat", null, true, 1000000));
                            //    p.addLast("hookedHandler", new WebSocketClientHandler());
                            p.addLast(new WsChannelInboundHandler());

                        }
                    });
            URI websocketURI = new URI("ws://127.0.0.1:8081/test/hello");
            HttpHeaders httpHeaders = new DefaultHttpHeaders();
            //进行握手
            //   WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(websocketURI, WebSocketVersion.V13, (String)null, true,httpHeaders);
            System.out.println("connect");

            int num = 10;
            while(num-- > 0) {
                a(boot, websocketURI);
            }
        }catch (Exception e){
            e.printStackTrace();
            group.shutdownGracefully();
        }
    }

    public static void a(Bootstrap boot,URI websocketURI) throws InterruptedException {
        ChannelFuture channelFuture = boot.connect(websocketURI.getHost(), websocketURI.getPort()).sync();
        channelFuture.addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future future)
                    throws Exception {
                if (future.isSuccess()) {
                    System.out.println("webcat ws server started listening at {} ");
                } else {
                    System.out.println("webcat ws server started fail! port={}, cause={}");
                }
            }
        });
        channelFuture.channel().closeFuture().sync();
    }

    static class WsChannelInboundHandler extends ChannelInboundHandlerAdapter{
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
         //   super.channelActive(ctx);



            String json = "{'name':1}";
            System.out.println("add-" + json);
            TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(json);
            ctx.writeAndFlush(textWebSocketFrame);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("msg " + msg);
            super.channelRead(ctx, msg);
        }
    }
}
