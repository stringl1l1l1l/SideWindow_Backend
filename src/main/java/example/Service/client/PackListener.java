package example.Service.client;

import example.Controller.ClientWebSocket;
import example.Controller.ServerServlet;
import example.Entity.ExtraInfo;
import example.Entity.Segment;
import example.Entity.SegmentInfo;
import example.Service.Global;
import example.utils.GsonUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

public class PackListener extends Thread {
    private final Client client;
    private final Logger log = Logger.getLogger(PackListener.class);

    public PackListener(Client client) {
        this.client = client;
    }

    @Override
    public void run() {
        while (true) {
            int ackNo = -1;
            try {
                // 发送端发送未完毕，继续
                if (Thread.interrupted()) {
                    log.info("客户端监听线程已断开");
                    break;
                }
                if (client.in.available() != 0) {
                    Segment segment = client.readByteStream2Segment();
                    log.info("客户端接收到报文: " + segment.toString());
                    int revcStatus = client.receiveWindow.capturePack(segment);
                    client.receiveWindow.printRecWindow();
                    if (ClientWebSocket.session != null) {
                        SegmentInfo info = new SegmentInfo(segment, false, false, revcStatus);
                        ArrayList<SegmentInfo> tmp = new ArrayList<>();
                        tmp.add(info);
                        String res =
                                GsonUtils.msg2Json(
                                        200,
                                        "返回已接收报文，数据和接收窗口",
                                        tmp,
                                        new ExtraInfo(
                                                client.receiveWindow.getReceivedData(),
                                                client.receiveWindow));
                        ClientWebSocket.session.getBasicRemote().sendText(res);
                    }
                }
                // 接收端返回对按序到达的最后一个报文段的ACK
                else if ((ackNo = client.receiveWindow.needACK()) != -1) {
                    Segment segment = new Segment(Global.TYPE_ACK, ackNo, 4, 4, "ACK" + ackNo);
                    log.info("客户端发送确认报文：" + segment.toString());
                    client.sendByteStream(segment.segStream);
                    client.receiveWindow.hasSendACK();
                    client.receiveWindow.printReceivedData();
                    client.receiveWindow.printRecWindow();

                    if (ClientWebSocket.session != null) {
                        SegmentInfo info = new SegmentInfo(segment);
                        ArrayList<SegmentInfo> tmp = new ArrayList<>();
                        tmp.add(info);
                        String res = GsonUtils.msg2Json(201, "返回已发送ACK", tmp);
                        ClientWebSocket.session.getBasicRemote().sendText(res);
                    }
                    // 全局通知此时接收端的接收内容已更新
                    Global.receiveDone.release();
                }
            } catch (IOException e) {
                this.interrupt();
                e.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void start() {
        log.info("报文接收线程已启动");
        super.start();
    }
}
