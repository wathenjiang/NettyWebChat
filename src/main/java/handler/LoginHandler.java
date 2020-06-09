package handler;

import io.netty.channel.ChannelHandler;
import service.ChatService;
import service.ChatServiceImpl;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * @author SpongeCaptain
 * @date 2020/6/8 16:52
 * <p>
 * 做了 3 件事：
 * 1. 解析 WebSocket 传来的字符串数据为 JSONObject 实例，如果不符合 JSON 格式，那么就返回用于其发错信息的提示
 * 2. 如果是登录请求，那么就在这里处理逻辑
 * 3. 如果是其他类型的请求，那么就向后传播（首先传播向 AuthHandler 其会进行鉴权工作）
 */
@ChannelHandler.Sharable
public class LoginHandler extends SimpleChannelInboundHandler<String> {

    final static ChatService chatService = new ChatServiceImpl();

    public static final LoginHandler INSTANCE = new LoginHandler();

    protected LoginHandler() {
    }


    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

        JSONObject param = null;
        try {
            param = JSONObject.parseObject(msg);
        } catch (Exception e) {
            ctx.channel().write(new TextWebSocketFrame("Sys:数据格式错误"));
            e.printStackTrace();
        }
        if (param == null) {
            ctx.channel().write(new TextWebSocketFrame("Sys:数据格式错误"));
            return;
        }

        String type = (String) param.get("type");
        if ("LOGIN".equals(type)) {
            chatService.login(param, ctx);
        } else {
            ctx.fireChannelRead(param);
        }

    }
}
