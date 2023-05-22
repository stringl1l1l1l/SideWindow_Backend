package example.Entity;

public class ReceiveWindow {
    private int posBeg;
    public int posCur; // posCur为最后按序到达的位置的后一位置
    private int posEnd;
    private final SegmentInfo[] segmentList;
    private int cacheSize;
    private int windowSize;

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
        for(int i = Global.INIT_SEG_NO; i < size; i++)
            insertSegInfo(i);
    }

    public void insertSegInfo(int segNo)
    {
        assert (cacheSize != Global.MAX_CACHE_SIZE);
        segmentList[cacheSize++] = new SegmentInfo(segNo);
    }

    /**
     * @param newSize 新窗口大小
     *  改变接收窗口大小，并移动结束指针，要求移动后的结束指针不能小于当前指针
     */
    public synchronized void changeWindowSize(int newSize) {
        this.posEnd = this.posBeg + newSize;
        this.windowSize = newSize;
    }

    public synchronized void capturePack(Segment packSeg) {
        if(packSeg.type == Global.TYPE_PACK) {
            // 如果序号落入接收窗口内，且不是重复报文段，且按序到达，则将报文段放入接收窗口
            if (!segmentList[this.posCur].isAck
                    && segmentList[this.posCur].segNo == packSeg.segNo) {

                segmentList[this.posCur].segment = packSeg;
                segmentList[this.posCur].isAck = true;
                this.posCur++;
            }
        }
    }

    /**
     *
     * @return 当前窗口是否有报文段需要发送ACK，如果有返回应发送的ACK号，否则返回-1
     */
    public synchronized int needACK() {
        return (this.posBeg == this.posCur) ? -1 : this.segmentList[this.posCur].segNo;
    }

    public synchronized void hasSendACK() {
//        for(int i = posBeg; i < posCur; i++)
//            segmentList[i].isAck = true;
        int offset = this.posCur - this.posBeg;
        this.posBeg = this.posCur;
        this.posEnd += offset;
    }

    /**
     * 打印接收缓存中所有已接收内容
     */
    public void printReceivedData() {
        StringBuilder stringBuilder = new StringBuilder(Global.MAX_DATA_SIZE);
        for(SegmentInfo info : segmentList) {
            if(info.segment == null)
                break;
            stringBuilder.append(info.segment.data);
        }
        System.out.println("_______________________接收端已接收内容_______________________\n"
                + stringBuilder
                + "\n_______________________接收端已接收内容_______________________"
        );
    }
}
