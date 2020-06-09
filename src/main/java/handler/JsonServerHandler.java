package handler;

import io.netty.channel.ChannelHandler;
import service.ChatService;
import service.ChatServiceImpl;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author SpongeCaptain
 * @date 2020/6/8 10:30
 */
@ChannelHandler.Sharable
public class JsonServerHandler extends SimpleChannelInboundHandler<JSONObject> {

    final static ChatService chatService= new ChatServiceImpl();

    public static final JsonServerHandler INSTANCE  = new JsonServerHandler();

    protected JsonServerHandler(){}


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JSONObject param) throws Exception {

        String type = (String) param.get("type");

        if ("SEND_TO_ONE".equals(type)) {
            chatService.sendToOne(param,ctx);
        } else if ("CREATE_GROUP".equals(type)) {
            chatService.createGroup(param,ctx);
        } else if ("SEND_TO_GROUP".equals(type)) {
            chatService.sendToGroup(param,ctx);
        } else if ("FILE_MSG_GROUP_SENDING".equals(type)) {
        }else{
            chatService.typeError(param,ctx);
        }
    }
}
