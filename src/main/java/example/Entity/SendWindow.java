package example.Entity;

import example.Controller.ServerWebSocket;
import example.Service.Global;
import example.utils.GsonUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class SendWindow {
    private int posBeg;
    private int posEnd;
    private int posCur;
    private final SegmentInfo[] segmentList;
    private int cacheSize;
    private int windowSize;

    private final transient Logger log = Logger.getLogger(SendWindow.class);

    public int getPosBeg() {
        return posBeg;
    }

    public int getPosEnd() {
        return posEnd;
    }

    public int getPosCur() {
        return posCur;
    }

    public Segment getSpecifiedSegment(int index) {
        return segmentList[index].segment;
    }

    public SendWindow() {
        windowSize = Global.SEND_WIND;
        posCur = posBeg = 0;
        posEnd = 0;
        cacheSize = 0;
        segmentList = new SegmentInfo[Global.MAX_CACHE_SIZE];
        for (int i = Global.INIT_SEG_NO; i < Global.INIT_SEG_NO + 100; i++) {
            segmentList[i - Global.INIT_SEG_NO] = new SegmentInfo(i); // 调用构造函数进行初始化
        }
    }

    public void insertSegment(Segment segment) {
        assert (cacheSize != Global.MAX_CACHE_SIZE);
        segmentList[cacheSize++] = new SegmentInfo(segment);
    }

    /**
     * 倒序遍历当前已发送窗口，找到ACK确认的报文段，将之前的报文段表明为已确认，并移动发送窗口
     *
     * @param ackSeg 确认报文
     */
    public synchronized int captureACK(Segment ackSeg) {
        // 当前接收到了ACK报文
        if (ackSeg.type == Global.TYPE_ACK) {
            boolean flag = false; // 用于标识ACK确认号是否落在已发送窗口中，如果没有，什么也不做
            if (ackSeg.hasError()) {
                log.error("报文段出错");
                return Global.RECV_ERROR;
            }
            int i; // i记录被确认报文位置，segmentList[i].segment.segNo == ackSeg.ackNo - 1
            for (i = posCur - 1; i >= posBeg; i--) {
                // 倒序找到已发送的，与(确认号 - 1)一致且没有被重复确认的报文段
                if (segmentList[i].isSend
                        && !segmentList[i].isAck
                        && segmentList[i].segment.segNo == ackSeg.ackNo - 1) {
                    flag = true;
                    break;
                }
            }
            // 根据ACK报文返回的接收窗口大小调整发送窗口和尾指针
            changeWindowSize(Math.min(ackSeg.winSize, this.windowSize));
            if (flag) {
                log.info("ACK" + ackSeg.ackNo + "已确认");
                for (int j = posBeg; j <= i; j++) segmentList[j].isAck = true;
                this.posBeg = i + 1; // 调整发送窗口到确认报文后
                return Global.RECV_OK;
            }
        }
        return Global.RECV_OTHER;
    }

    /**
     * @return 当前窗口有多少报文段可以发送
     */
    public synchronized int available() {
        return this.posEnd - this.posCur;
    }

    /**
     * @return 当前发送窗口是否有缓存没被发送
     */
    public boolean hasCached() {
        return this.cacheSize > this.posEnd;
    }
    /**
     * 获取当前发送窗口所有可用的报文
     *
     * @return 待发送的报文段列表
     */
    public synchronized ArrayList<SegmentInfo> getAvailable() {
        ArrayList<SegmentInfo> list = new ArrayList<>();
        while (posCur < posEnd) {
            segmentList[posCur].isSend = true;
            list.add(segmentList[posCur]);
            posCur++;
        }
        return list;
    }

    /**
     * @param newSize 新窗口大小 改变发送窗口大小，并移动结束指针，要求移动后的结束指针不能小于当前指针
     */
    public synchronized void changeWindowSize(int newSize) {
        int newEnd = Math.min(this.posBeg + this.windowSize, this.cacheSize);
        assert (newEnd >= posCur);
        this.windowSize = newSize;
        this.posEnd = newEnd;
    }

    /**
     * 打印当前发送窗口状态信息
     *
     * @return
     */
    public synchronized void printSendWindow() {
        int[] win1 = new int[posCur - posBeg], win2 = new int[posEnd - posCur];
        int pos1 = 0, pos2 = 0;
        for (int i = posBeg; i < posCur; i++) win1[pos1++] = segmentList[i].segment.segNo;
        for (int i = posCur; i < posEnd; i++) win2[pos2++] = segmentList[i].segment.segNo;
        log.info("当前发送窗口状态为：");
        log.info("beg: " + this.posBeg + ", cur: " + this.posCur + ", end: " + this.posEnd);
        log.info("已发送报文：" + Arrays.toString(win1));
        log.info("未发送报文：" + Arrays.toString(win2));
    }

    public synchronized boolean isAllAck() {
        return this.posBeg == this.posCur;
    }
}
