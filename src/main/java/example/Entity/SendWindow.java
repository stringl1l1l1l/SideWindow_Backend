package example.Entity;

import example.Entity.Segment;

import java.util.ArrayList;
import java.util.Arrays;

public class SendWindow {
    private int posBeg;
    private int posEnd;
    private int posCur;
    private final SegmentInfo[] segmentList;
    private int cacheSize;
    private int windowSize;

    public int getPosBeg() {
        return posBeg;
    }

    public int getPosEnd() {
        return posEnd;
    }

    public int getPosCur() {
        return posCur;
    }

    public Segment getSpecifiedSegment (int index) {
        return segmentList[index].segment;
    }

    public SendWindow()
    {
        windowSize = Global.SEND_WIND;
        posCur = posBeg = 0;
        posEnd = windowSize;
        cacheSize = 0;
        segmentList = new SegmentInfo[Global.MAX_CACHE_SIZE];
    }

    public void insertSegment(Segment segment)
    {
        assert (cacheSize != Global.MAX_CACHE_SIZE);
        segmentList[cacheSize++] = new SegmentInfo(segment);
    }

    /**
     *  倒序遍历当前已发送窗口，找到ACK确认的报文段，将之前的报文段表明为已确认，并移动发送窗口
     * @param ackSeg 确认报文
     */
    public synchronized void captureACK(Segment ackSeg)
    {
        // 当前接收到了ACK报文
        if(ackSeg.type == Global.TYPE_ACK) {
            boolean flag = false; // 用于标识ACK确认号是否落在已发送窗口中，如果没有，什么也不做
            for (int i = posCur - 1; i >= posBeg; i--) {
                // 找到已发送的，与(确认号 - 1)一致且没有被重复确认的报文段
                if(segmentList[i].isSend
                        && !ackSeg.hasError()
                        && !segmentList[i].isAck
                        && segmentList[i].segment.segNo == ackSeg.ackNo - 1)
                {
                    flag = true;
                    // 移动发送窗口的起始指针到确认号报文处
                    this.posBeg = i + 1;
                    break;
                }
            }
            // 根据ACK报文返回的接收窗口大小调整发送窗口
            changeWindowSize(ackSeg.winSize);
            // 移动发送窗口的结尾指针，重新调整发送窗口
            if (flag) {
                this.posEnd = Math.min(this.posBeg + this.windowSize, this.cacheSize);
                System.out.println("ACK" + ackSeg.ackNo + "已确认");
            }
        }
    }

    /**
     *
     * @return 当前窗口是否有报文段可以发送
     */
    public synchronized boolean isAvailable() {
        return this.posEnd != this.posCur;
    }

    /**
     * 获取当前发送窗口所有可用的报文
     * @return 待发送的报文段列表
     */
    public synchronized ArrayList<Segment> getAvailable()
    {
        ArrayList<Segment> list = new ArrayList<>();
        while(posCur < posEnd) {
            list.add(segmentList[posCur].segment);
            segmentList[posCur].isSend = true;
            posCur++;
        }
        return list;
    }

    /**
     * @param newSize 新窗口大小
     *  改变发送窗口大小，并移动结束指针，要求移动后的结束指针不能小于当前指针
     */
    public synchronized void changeWindowSize(int newSize) {
        int newEnd = this.posBeg + newSize;
        assert (newEnd >= posCur);
        this.windowSize = newSize;
    }

    /**
     * 打印当前发送窗口状态信息
     */
    public synchronized void printSendWindow() {
        int[] win1 = new int[posCur - posBeg], win2 = new int[posEnd - posCur];
        int pos1 = 0, pos2 = 0;
        for(int i = posBeg; i < posCur; i++)
            win1[pos1++] = segmentList[i].segment.segNo;
        for(int i = posCur; i < posEnd; i++)
            win2[pos2++] = segmentList[i].segment.segNo;
        System.out.println("当前发送窗口状态为：");
        System.out.println("beg: " + this.posBeg + ", cur: "+ this.posCur + ", end: " + this.posEnd);
        System.out.println("已发送报文：" + Arrays.toString(win1));
        System.out.println("未发送报文：" + Arrays.toString(win2));
    }

    public synchronized boolean isAllAck() {
        return this.posBeg == this.posCur;
    }
}
