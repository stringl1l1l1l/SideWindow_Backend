package example.Entity;

public class SegmentInfo {
    public Segment segment;
    public int segNo;
    public boolean isSend;
    public boolean isAck;

    public SegmentInfo(int segNo) {
        this.segNo = segNo;
        isSend = false;
        isAck = false;
        segment = null;
    }

    public SegmentInfo(Segment segment) {
        this.segNo = segment.segNo;
        this.segment = segment;
        this.isSend = false;
        this.isAck = false;
    }

}
