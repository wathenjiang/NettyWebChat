package session;


import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author SpongeCaptain
 * @date 2020/6/7 20:16
 */
public class SessionUtil {
    //key 为 userId,value 为 userId 对应的 Channel
    private static final HashMap<Long, Channel> userIdToChannelMap = new HashMap();
    //key 为 groupId,value 为 userId 构成的数组
    private static final Map<Long, Long[]> groupIdToGroupMember = new ConcurrentHashMap();

    public static void bindSession(Session session, Channel channel) {
        userIdToChannelMap.put(session.getUserId(), channel);
        channel.attr(AttributeKeys.SESSION).set(session);
    }

    public static void UnbindSession(Channel channel) {

        final String userName = channel.attr(AttributeKeys.SESSION).get().getUserName();
        final Long userId = channel.attr(AttributeKeys.SESSION).get().getUserId();
        System.out.println("name: "+userName + " id: "+userId +" 登出，并释放相关资源" );

        userIdToChannelMap.remove(getSession(channel).getUserId());
        channel.attr(AttributeKeys.SESSION).set(null);

    }

    public static Channel getChannel(Long userId) {
        return userIdToChannelMap.get(userId);
    }

    public static Session getSession(Channel channel) {

        return channel.attr(AttributeKeys.SESSION).get();
    }

    public static boolean isLogin(Channel channel) {
        if (null != channel && channel.hasAttr(AttributeKeys.SESSION)) {
            return true;
        } else {
            return false;
        }

    }

    public static boolean ifGroupExists(Long groupId) {
        if (groupIdToGroupMember.containsKey(groupId)) {
            return true;
        } else {
            return false;
        }

    }

    public static void createGroup(Long groupId, Long[] groupMember) {
        groupIdToGroupMember.put(groupId, groupMember);
    }

    public static Long[] getGroupMember(Long groupId) {
        return groupIdToGroupMember.get(groupId);
    }


}
