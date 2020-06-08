package handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import session.SessionUtil;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpUtil.setContentLength;

/**
 * @author SpongeCaptain
 * @date 2020/6/6 23:09
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handShaker;

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
//        System.out.println("收到消息：" + msg);
        /** 传统http接入:此 HTTP 请求为第一次升级协议用的 HTTP 请求 **/
        if (msg instanceof FullHttpRequest) {
            //调用 handleHttpRequest 方法处理 HTTP 请求
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        }/** websocket接入:此时请求已经升级完毕 **/
        else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    //当事件的消息类型为 WebSocketFrame 会调用此方法进行处理
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        /** 断开连接处理 **/
        if (frame instanceof CloseWebSocketFrame) {
            SessionUtil.UnbindSession(ctx.channel());
            //会负责关闭 Channel 实例以及释放其资源，比如与其配对的 pipeline
            handShaker.close(ctx.channel(), ((CloseWebSocketFrame) frame).retain());
            return;
        }
        /** ping处理 **/
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        /** 非文本不支持 **/
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported !", frame.getClass().getName()));
        }
        String reqMsg = ((TextWebSocketFrame) frame).text();
        //这里的代码用于处理客户端发来的心跳
        if ("ping".equals(reqMsg)) {
//            System.out.println(reqMsg);
            ctx.channel().write(new TextWebSocketFrame("pong"));
        }else{
            /** 对文本信息处理，并响应客户端：将字符串向后传播 **/
//            System.out.println(reqMsg);
            ctx.fireChannelRead(reqMsg);
        }

    }

    //当事件的消息类型为 FullHttpRequest 请求时，会调用此方法
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        //if() 内的消息表示 HTTP 消息不正常，返回客户端一个提示其错误响应的消息
        if (!req.decoderResult().isSuccess()
                || (!"websocket".equals(req.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        //如果 HTTP 消息解码正常且 HTTP 消息头的 Upgrade 栏值为 websocket，那么就在应用层建立 WebSocket 握手连接
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://localhost:8080/websocket", null, false
        );
        handShaker = wsFactory.newHandshaker(req);
        if (handShaker == null) {
            /** 版本不支持 **/
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            /** 握手建立连接(这会负责向客户端发送 HTTP 响应：表示协议升级成功) **/
            final ChannelFuture handshake = handShaker.handshake(ctx.channel(), req);
            if (handshake.isSuccess()){
                System.out.println("从 HTTP 升级为 WebSocket 协议成功！");
            }
        }
    }

    //此方法用于封装 HTTP 响应的回复，正如上面所说，WebSocket 握手建立后不需要调用此方法，其自动就会发送
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse resp) {
        if (resp.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(resp.status().toString(), CharsetUtil.UTF_8);
            resp.content().writeBytes(buf);
            buf.release();
            setContentLength(resp, resp.content().readableBytes());
        }
        ChannelFuture f = ctx.channel().writeAndFlush(resp);
        if (!isKeepAlive(req) || resp.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
}