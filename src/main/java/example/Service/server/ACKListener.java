package example.Service.server;

import example.Controller.ServerWebSocket;
import example.Entity.ExtraInfo;
import example.Entity.Segment;
import example.Entity.SegmentInfo;
import example.Service.Global;
import example.utils.GsonUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;

/** 子线程，当服务端发送数据后，持续监听接受端是否有报文发送过来，如果是ACK报文就收下，并做一些判断处理 */
public class ACKListener extends Thread {
    private final Server server;
    private final Logger log = Logger.getLogger(ACKListener.class);

    public ACKListener(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (Thread.interrupted()) break;
                // 当前有未确认报文，且有数据发送过来
                if (!server.sendWindow.isAllAck() && server.in.available() != 0) {
                    Segment segment = server.readByteStream2Segment();
                    log.info("服务端接收到报文: " + segment.toString());
                    int recvStatus = server.sendWindow.captureACK(segment);
                    server.sendWindow.printSendWindow();
                    // 接收到一个ACK报文，返回给前端
                    if (recvStatus == Global.RECV_OK) {
                        ArrayList<SegmentInfo> tmp = new ArrayList<>();
                        SegmentInfo info = new SegmentInfo(segment);
                        tmp.add(info);
                        if (ServerWebSocket.session != null) {
                            String res =
                                    GsonUtils.msg2Json(
                                            201,
                                            "返回已接收ACK报文和窗口",
                                            tmp,
                                            new ExtraInfo(server.sendWindow));
                            try {
                                ServerWebSocket.session.getBasicRemote().sendText(res);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    server.sendWindow.changeWindowSize(Global.SEND_WIND); // 接收到报文后重新调整发送窗口
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    @Override
    public synchronized void start() {
        super.start();
        log.info("已启动ACK接收线程");
    }
}
