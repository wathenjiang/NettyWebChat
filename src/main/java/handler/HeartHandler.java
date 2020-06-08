package handler;


import service.ChatService;
import service.ChatServiceImpl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author SpongeCaptain
 * @date 2020/6/8 17:24
 */
public class HeartHandler extends IdleStateHandler {
    private static ChatService chatService = new ChatServiceImpl();
    //如果 10 秒没有来自客户端的任何消息，那么关闭服务端处的 Channel，客户端（前端）会默认每隔 4 秒发一个心跳包
    /**
     * 注意事项：一般网页关闭页面会主动地回调地发送一个 CloseWebSocketFrame 帧，关闭之后因为管道、任务队列等资源都会被释放，所以不会在调用此方法
     * 可以利用虚拟机打开网页的情况下暂停 VM 模拟客户端突然下线的情况(将电脑的 wifi 关闭也可以导致虚拟机与本机强制失去连接)
     */
    private static final int READER_IDLE_TIME = 10;

    public HeartHandler() {
        super(READER_IDLE_TIME, 0, 0, TimeUnit.SECONDS);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
        System.out.println(READER_IDLE_TIME + "秒内未读到数据，关闭连接");
        chatService.logout(ctx);
    }
}
