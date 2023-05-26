package example.Entity;

import example.Service.Global;
import org.apache.log4j.Logger;

import java.util.Arrays;

public class ReceiveWindow {
    private int posBeg;
    public int posCur; // posCur为最后按序到达的位置的后一位置
    private int posEnd;
    private final SegmentInfo[] segmentList;
    private int cacheSize;
    private int windowSize;
    private Logger log = Logger.getLogger(ReceiveWindow.class);

    public ReceiveWindow() {
        windowSize = Global.REC_WIND;
        posBeg = 0;
        posCur = 0;
        posEnd = windowSize;
        cacheSize = 0;
        segmentList = new SegmentInfo[Global.MAX_CACHE_SIZE];
    }

    public ReceiveWindow(int size) {
        windowSize = Global.REC_WIND;
        posBeg = 0;
        posCur = 0;
        posEnd = windowSize;
        cacheSize = 0;
        segmentList = new SegmentInfo[Global.MAX_CACHE_SIZE];
        for (int i = Global.INIT_SEG_NO; i < size; i++) {
            insertSegInfo(i);
        }
    }

    public void insertSegInfo(int segNo) {
        assert (cacheSize != Global.MAX_CACHE_SIZE);
        segmentList[cacheSize++] = new SegmentInfo(segNo);
    }

    /**
     * @param newSize 新窗口大小 改变接收窗口大小，并移动结束指针，要求移动后的结束指针不能小于当前指针
     */
    public synchronized void changeWindowSize(int newSize) {
        this.posEnd = this.posBeg + newSize;
        this.windowSize = newSize;
    }

    public synchronized void capturePack(Segment packSeg) {
        if (packSeg.type == Global.TYPE_PACK) {
            // 如果序号落入接收窗口内，且不是重复报文段，且未出错，且按序到达，则将报文段放入接收窗口
            if (segmentList[this.posCur].isAck) {
                log.error("重复报文段");
            } else if (packSeg.hasError()) {
                log.error("报文段出错");
            } else if (segmentList[this.posCur].segNo != packSeg.segNo) {
                log.error("报文段序号没有按序到达，期望报文序号为：" + segmentList[this.posCur].segNo);
            } else {
                segmentList[this.posCur].segment = packSeg;
                segmentList[this.posCur].isAck = true;
                this.posCur++;
            }
        }
    }

    /**
     * @return 当前窗口是否有报文段需要发送ACK，如果有返回应发送的ACK号，否则返回-1
     */
    public synchronized int needACK() {
        return (this.posBeg == this.posCur) ? -1 : this.segmentList[this.posCur].segNo;
    }

    public synchronized void hasSendACK() {
        //        for(int i = posBeg; i < posCur; i++)
        //            segmentList[i].isAck = true;
        this.posBeg = this.posCur;
        this.posEnd = this.posBeg + windowSize;
    }

    public int[] printRecWindow() {
        int[] win = new int[posEnd - posBeg];
        int pos = 0;
        for (int i = posBeg; i < posEnd; i++) win[pos++] = segmentList[i].segNo;
        log.info("当前接收窗口状态为：");
        log.info("beg: " + this.posBeg + " cur: " + this.posCur + ", end: " + this.posEnd);
        log.info("接收窗口：" + Arrays.toString(win));
        return win;
    }

    /** 打印接收缓存中所有已接收内容 */
    public void printReceivedData() {
        StringBuilder stringBuilder = new StringBuilder(Global.MAX_DATA_SIZE);
        for (SegmentInfo info : segmentList) {
            if (info.segment == null) break;
            stringBuilder.append(info.segment.data);
        }
        System.out.println(
                "_______________________接收端已接收内容_______________________\n"
                        + stringBuilder
                        + "\n_______________________接收端已接收内容_______________________");
    }
}
