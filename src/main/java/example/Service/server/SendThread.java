package example.Service.server;

import example.Controller.ServerWebSocket;
import example.Entity.ExtraInfo;
import example.Entity.Segment;
import example.Service.Global;
import example.utils.GsonUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

public class SendThread extends Thread {
    private final Server server;
    private final Logger log = Logger.getLogger(SendThread.class);

    public SendThread(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        // 主线程持续监听是否有可发送报文
        ArrayList<Segment> tmp = new ArrayList<>();
        while (!Thread.interrupted()) {
            try {
                Global.readyToSend.acquire();
                // 获取当前所有可发送的报文段
                if (server.sendWindow.available() != 0) {
                    String res =
                            GsonUtils.msg2Json(
                                    HttpServletResponse.SC_OK,
                                    "返回已发送报文段",
                                    tmp,
                                    new ExtraInfo(server.sendWindow));
                    if (ServerWebSocket.session != null) {
                        log.info("向webSocket发送数据");
                        ServerWebSocket.session.getBasicRemote().sendText(res);
                        tmp.clear();
                    } else {
                        log.error("未检测到webSocket");
                    }
                    server.sendWindow.hasReceiveACK();
                    ArrayList<Segment> segments = server.sendWindow.getAvailable();
                    if (!segments.isEmpty()) {
                        // 如有可发送报文段，将这些报文段序列化成字节流，发送到接收端
                        for (Segment curSeg : segments) {
                            server.sendByteStream(curSeg.serialize());
                        }
                        tmp.addAll(segments);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    @Override
    public synchronized void start() {
        super.start();
    }
}
