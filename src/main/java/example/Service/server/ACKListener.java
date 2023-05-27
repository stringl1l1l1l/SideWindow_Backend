package example.Service.server;

import example.Entity.Segment;
import org.apache.log4j.Logger;

import java.io.IOException;

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
                    server.sendWindow.captureACK(segment);
                    server.sendWindow.printSendWindow();
                }
            } catch (IOException e) {
                this.interrupt();
                e.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void start() {
        super.start();
        log.info("已启动ACK接收线程");
    }
}
