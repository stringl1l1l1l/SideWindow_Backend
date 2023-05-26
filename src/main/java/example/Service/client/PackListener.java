package example.Service.client;

import example.Controller.ServerServlet;
import example.Entity.Segment;
import example.Service.Global;
import org.apache.log4j.Logger;

import java.io.IOException;

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
                    client.receiveWindow.capturePack(segment);
                    client.receiveWindow.printRecWindow();
                }
                // 接收端返回对按序到达的最后一个报文段的ACK
                else if ((ackNo = client.receiveWindow.needACK()) != -1) {
                    Segment segment =
                            new Segment(Global.TYPE_ACK, ackNo, 4, 4, "Hello Server, ACK" + ackNo);
                    log.info("客户端发送确认报文：" + segment.toString());
                    client.sendByteStream(segment.segStream);
                    client.receiveWindow.hasSendACK();
                    client.receiveWindow.printReceivedData();
                    client.receiveWindow.printRecWindow();
                }
            } catch (IOException e) {
                this.interrupt();
            }
        }
    }

    @Override
    public synchronized void start() {
        log.info("报文接收线程已启动");
        super.start();
    }
}
