package example.Service.server;

import example.Controller.ClientWebSocket;
import example.Entity.ExtraInfo;
import example.Entity.Segment;
import example.Entity.SegmentInfo;
import example.Service.Global;
import example.utils.GsonUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/** 超时重传扫描器, 定时器线程, 每隔一定时间扫描一次发送窗口的已发送部分，检查是否已经ACK */
public class Scanner extends TimerTask {
    private final Server server;
    private Timer timer;
    private final Logger log = Logger.getLogger(Scanner.class);

    public Scanner(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        // 定时时间到，如果当前发送窗口中有未确认报文段，就将它重传
        try {
            if (!server.sendWindow.isAllAck()) {
                for (int i = server.sendWindow.getPosBeg();
                        i < server.sendWindow.getPosCur();
                        i++) {
                    Segment curSeg = server.sendWindow.getSpecifiedSegment(i);
                    SegmentInfo info = new SegmentInfo(curSeg, false, false, true);
                    ArrayList<SegmentInfo> tmp = new ArrayList<>();
                    tmp.add(info);
                    String res =
                            GsonUtils.msg2Json(
                                    200, "返回重传报文和窗口", tmp, new ExtraInfo(server.sendWindow));
                    ClientWebSocket.session.getBasicRemote().sendText(res);
                    log.error(Global.PERIOD_MS / 1000 + "s内未收到ACK，超时重传：");
                    server.sendByteStream(curSeg.serialize());
                }
            }
        } catch (IOException e) {
            this.cancel();
        }
    }

    public void start() {
        timer = new Timer();
        timer.schedule(this, Global.PERIOD_MS, Global.PERIOD_MS);
        log.info("已启动定时扫描重传线程");
    }

    @Override
    public boolean cancel() {
        if (timer != null) timer.cancel();
        return true;
    }
}
