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
        log.info("已启动");
        ServerWebSocket.session = session;
        //        try {
        //            session.getBasicRemote().sendText(GsonUtils.msg2Json(200, "serverSocket已启动"));
        //        } catch (IOException e) {
        //            e.printStackTrace();
        //        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 收到消息时执行的代码
        log.info("收到消息：" + message);
        //        // 向客户端发送消息
        //        try {
        //            session.getBasicRemote().sendText(GsonUtils.msg2Json(200, "这是来自服务器的消息"));
        //        } catch (IOException e) {
        //            e.printStackTrace();
        //        }
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
