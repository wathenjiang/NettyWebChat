package service;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author SpongeCaptain
 * @date 2020/6/8 12:03
 */
public interface ChatService {

    void login(JSONObject param, ChannelHandlerContext ctx);

    void logout(ChannelHandlerContext ctx);

    void sendToOne(JSONObject param, ChannelHandlerContext ctx);

    void createGroup(JSONObject param, ChannelHandlerContext ctx);

    void sendToGroup(JSONObject param, ChannelHandlerContext ctx);

    void typeError(JSONObject param, ChannelHandlerContext ctx);

}
