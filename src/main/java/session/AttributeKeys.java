package session;

import io.netty.util.AttributeKey;

/**
 * @author SpongeCaptain
 * @date 2020/6/7 20:19
 */
public interface AttributeKeys {

    AttributeKey<Session> SESSION  =  AttributeKey.newInstance("session");
}
