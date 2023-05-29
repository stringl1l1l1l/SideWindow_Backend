package example.Service.server;

import example.Controller.ServerWebSocket;
import example.Entity.ExtraInfo;
import example.Entity.Segment;
import example.Entity.SegmentInfo;
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
        while (!Thread.interrupted()) {
            try {
                //                log.info("发送线程正在运行");
                // 若有缓存没发送，则继续运行发送线程，不用获取信号量
                if (!server.sendWindow.hasCached()) Global.readyToSend.acquire();
                // 获取当前所有可发送的报文段
                if (server.sendWindow.available() != 0) {
                    ArrayList<SegmentInfo> segmentInfo = server.sendWindow.getAvailable();
                    if (!segmentInfo.isEmpty()) {
                        // 如有可发送报文段，将这些报文段序列化成字节流，发送到接收端
                        for (SegmentInfo curSegInfo : segmentInfo) {
                            server.sendByteStream(curSegInfo.segment.serialize());
                        }
                    }
                    server.sendWindow.printSendWindow();
                    String res =
                            GsonUtils.msg2Json(
                                    HttpServletResponse.SC_OK,
                                    "返回已发送报文段",
                                    segmentInfo,
                                    new ExtraInfo(server.sendWindow));
                    if (ServerWebSocket.session != null) {
                        log.info("向webSocket发送数据");
                        ServerWebSocket.session.getBasicRemote().sendText(res);
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
