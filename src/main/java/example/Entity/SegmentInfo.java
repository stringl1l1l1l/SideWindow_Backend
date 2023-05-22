package example.Entity;

public class SegmentInfo {
    Segment segment;
    boolean isSend;
    boolean isAck;
    long timeStamp;

    public SegmentInfo(Segment segment) {
        this.segment = segment;
        this.isSend = false;
        this.isAck = false;
        this.timeStamp = System.currentTimeMillis();
    }

}
