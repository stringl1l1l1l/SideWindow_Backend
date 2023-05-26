package example.Service.server;

import example.Entity.Segment;
import org.apache.log4j.Logger;

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
        while (true) {
            try {
                // 获取当前所有可发送的报文段
                if (Thread.interrupted()) {
                    log.info("发送线程已中断");
                    break;
                }
                if (server.sendWindow.isAvailable()) {
                    ArrayList<Segment> segments = server.sendWindow.getAvailable();
                    if (!segments.isEmpty()) {
                        // 如有可发送报文段，将这些报文段序列化成字节流，发送到接收端
                        for (Segment curSeg : segments) {
                            server.sendByteStream(curSeg.serialize());
                        }
                    }
                }
            } catch (Exception e) {
                this.interrupt();
            }
        }
    }

    @Override
    public synchronized void start() {
        super.start();
    }
}
