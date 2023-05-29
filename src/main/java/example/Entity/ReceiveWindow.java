package example.Entity;

import example.Service.Global;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Random;

public class ReceiveWindow {
    private static transient int N = 3;
    private int posBeg;
    public transient int posCur; // posCur为最后按序到达的位置的后一位置
    private int posEnd;
    private SegmentInfo[] segmentList;
    private int cacheSize;
    private int windowSize;
    public transient int randomNum = 0;
    private final transient Logger log = Logger.getLogger(ReceiveWindow.class);

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

    public int getPosBeg() {
        return posBeg;
    }

    public int getPosCur() {
        return posCur;
    }

    public int getPosEnd() {
        return posEnd;
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

    public synchronized int capturePack(Segment packSeg) {
        Random random = new Random();
        int randomNumber = random.nextInt(4); // 生成 0 到 3 的随机数
        this.randomNum = randomNumber;
        // 若随机情况为正常
        if (randomNumber == Global.RECV_OK) {
            if (packSeg.type == Global.TYPE_PACK) {
                int flag = 0;
                if (packSeg.hasError()) {
                    log.error("报文段出错");
                    return Global.RECV_ERROR;
                }
                // 如果序号落入接收窗口内，且不是重复报文段，且未出错，则将报文段放入接收窗口，并调整指针
                for (int i = posBeg; i < posEnd; i++) {
                    if (segmentList[i].segNo == packSeg.segNo) {
                        if (segmentList[i].isAck) {
                            log.error("重复报文段");
                            return Global.RECV_REPEAT;
                        } else {
                            flag = 1;
                            segmentList[i].segment = packSeg;
                            segmentList[i].isAck = true;
                            break;
                        }
                    }
                }
                if (flag == 1) {
                    // 接收报文后，重新调整posCur到连续接收的位置
                    int i = posCur;
                    while (i != posEnd && segmentList[i].isAck) i++;
                    posCur = i;
                    return Global.RECV_OK;
                } else return Global.RECV_REJECT;
            }
        }
        return randomNumber;
    }

    /**
     * @return 当前窗口是否已累计到指定个数报文段？如果是返回应发送的ACK号，否则返回-1
     */
    public synchronized int needACK() {
        int acceptCnt = this.posCur - this.posBeg;
        if (this.posCur == this.posEnd || acceptCnt >= ReceiveWindow.N)
            return segmentList[this.posCur].segNo;
        else return -1;
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

    public String getReceivedData() {
        StringBuilder stringBuilder = new StringBuilder(Global.MAX_DATA_SIZE);
        for (SegmentInfo info : segmentList) {
            if (info.segment != null) stringBuilder.append(info.segment.data);
            else break;
        }
        return stringBuilder.toString();
    }

    public void clearReceivedCache() {
        for (SegmentInfo info : segmentList) {
            if (info.segment != null) info.segment.data = "";
            else break;
        }
    }

    /** 打印接收缓存中所有已接收内容 */
    public void printReceivedData() {
        System.out.println(
                "_______________________接收端已接收内容_______________________\n"
                        + getReceivedData()
                        + "\n_______________________接收端已接收内容_______________________");
    }
}
