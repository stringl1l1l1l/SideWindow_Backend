package example.Controller;

import example.Model.JsonPack;
import example.utils.GsonUtils;
import org.apache.log4j.Logger;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint("/server_socket")
public class ServerWebSocket {
    public static Session session = null;
    private final Logger log = Logger.getLogger(ServerWebSocket.class);

    @OnOpen
    public void onOpen(Session session) {
        log.info("ServerWebSocket已启动");
        ServerWebSocket.session = session;
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到消息：" + message);
    }

    @OnClose
    public void onClose(Session session) {
        // 连接关闭时执行的代码
    }

    @OnError
    public void onError(Throwable error) {
        // 发生错误时执行的代码
    }
}
