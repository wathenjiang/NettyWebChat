package handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import session.SessionUtil;

/**
 * @author SpongeCaptain
 * @date 2020/6/8 16:43
 * <p>
 * 鉴权的逻辑比较简单：就是看此 Channel 有没有对应的 Session 实例有注册，如果有那么就通过检查，并将自身从 pipeline 上删除
 * 否则，向用户返回一个其没有登录的错误提示。
 */
@ChannelHandler.Sharable
public class AuthHandler extends SimpleChannelInboundHandler {

    public static final AuthHandler INSTANCE = new AuthHandler();

    protected AuthHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (!SessionUtil.isLogin(ctx.channel())) {
            ctx.channel().write(new TextWebSocketFrame("Sys:您未登录，请先登录！"));
        } else {
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(msg);
        }

    }
}
