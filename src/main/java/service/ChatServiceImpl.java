package service;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import session.AttributeKeys;
import session.Session;
import session.SessionUtil;

/**
 * @author SpongeCaptain
 * @date 2020/6/8 12:06
 */
public class ChatServiceImpl implements ChatService {

    public  void login(JSONObject param, ChannelHandlerContext ctx) {
        //先进行是否重复登录的判定
        if (SessionUtil.isLogin(ctx.channel())) {
            ctx.channel().write(new TextWebSocketFrame("Sys:您已登录，不需要重复登录!"));
            return;
        }

        //1. 创建与此登录用户相关的 Session
        final Session session = new Session();
        Long userId = Long.parseLong((String) param.get("userId"));
        final String userName = (String) param.get("userName");
        session.setUserId(userId);
        session.setUserName(userName);

        //2. 将此连接对一个的 Channel 与 Session 绑定，以及将 userId 和 Channel 作为一对键值对存储于 Map 中
        final Channel channel = ctx.channel();

        SessionUtil.bindSession(session, channel);

        channel.write(new TextWebSocketFrame("Sys:登录成功! userId:" + userId + " userName:" + userName));
        System.out.println("name: "+userName + " id: "+userId +" 登录成功" );
    }

    public void logout(ChannelHandlerContext ctx) {
        SessionUtil.UnbindSession(ctx.channel());
        ctx.channel().close();
    }

    public void sendToOne(JSONObject param, ChannelHandlerContext ctx) {
        //1. 得到发送者的用户信息
        final Long fromUserId = Long.parseLong((String) param.get("fromUserId"));
        final String fromUserName = (String) param.get("fromUserName");
        //2. 得到接收者的用户信息
        final Long toUserId = Long.parseLong((String) param.get("toUserId"));
        //3. 得到需要转发的信息
        final String message = (String) param.get("message");
        //4. 根据接收方是否在线来进行转发逻辑的处理
        final Channel ReceiverChannel = SessionUtil.getChannel(toUserId);
        //在线
        if (null != ReceiverChannel) {
            //这里使用 writeAndFlush 比较好，否则会造成消息发送延迟
            ReceiverChannel.writeAndFlush(new TextWebSocketFrame("用户 " + fromUserName + " 给您发来消息：" + message));
        }
        //不在线
        else {
            ctx.channel().write(new TextWebSocketFrame("Sys:对方不在线，发送无效!"));
        }

    }

    //发送一个用户时要求其在线，但是创造一个群组并不要求此用户在线
    public void createGroup(JSONObject param, ChannelHandlerContext ctx) {

        //1. 查询此 groupId 是否已经创建好了
        final long groupId = Long.parseLong((String) param.get("groupId"));
        if (SessionUtil.ifGroupExists(groupId)) {
            ctx.channel().write(new TextWebSocketFrame("Sys: 此GroupId 已经被占用，请另选 groupId！"));
            return;
        }
        //2. 如果 groupId 对应的组还没有创建,则创建组

        String groupMessage = (String) param.get("message");

        final String[] split = groupMessage.split(",");
        if (split.length < 2) {
            ctx.channel().write(new TextWebSocketFrame("Sys：小组成员至少应当有两个及以上！"));
            return;
        }
        final Long[] groupMember = new Long[split.length];
        try {

            for (int i = 0; i < split.length; i++) {
                groupMember[i] = Long.parseLong(split[i]);
                //如果此时用户在线，那么就告知此用户你被拉入群中
                final Channel channel = SessionUtil.getChannel(groupMember[i]);
                if (SessionUtil.isLogin(channel)&&channel!=ctx.channel()) {

                    final String fromUserId = (String) param.get("fromUserId");
                    channel.writeAndFlush(new TextWebSocketFrame("Sys：您被用户：" + fromUserId + "拉入群：" + groupId));
                }

            }
            SessionUtil.createGroup(groupId, groupMember);
        } catch (NumberFormatException e) {
            ctx.channel().write(new TextWebSocketFrame("Sys：请输入正确格式的小组成员信息"));
            return;
        }

        ctx.channel().write(new TextWebSocketFrame(
                "Sys：创建小组成功 " + "groupId：" + groupId + " 小组成员有：" + groupMessage));

    }

    public void sendToGroup(JSONObject param, ChannelHandlerContext ctx) {
        final long toGroupId = Long.parseLong((String) param.get("toGroupId"));
        //没有此群组并未创建
        if (!SessionUtil.ifGroupExists(toGroupId)) {
            ctx.channel().write(new TextWebSocketFrame("Sys:输入的 groupId 并不存在！"));
            return;
        }
        //此群组已经创建了,则查询群组里的用户信息
        Long[] groupMember = SessionUtil.getGroupMember(toGroupId);
        final String fromUserName = (String) param.get("fromUserName");
        final String message = (String) param.get("message");
        for (Long toUserId : groupMember
        ) {
            //这里的遍历逻辑是，如果用户在线，那么就将信息转发给该用户，否则就不发了（没有不在线不能发送的提示）
            final Channel channel = SessionUtil.getChannel(toUserId);
            if (null != channel) {
                channel.writeAndFlush(new TextWebSocketFrame(
                        "group"+toGroupId +"/"+ fromUserName + "： " + message));
            }
        }
    }

    public void typeError(JSONObject param, ChannelHandlerContext ctx) {
        ctx.channel().write(new TextWebSocketFrame("JSON Type 错误！"));
    }
}
