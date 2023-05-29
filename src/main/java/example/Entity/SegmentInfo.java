package example.Entity;

import example.Service.Global;

public class SegmentInfo {
    public Segment segment;
    public int segNo;
    public boolean isSend;
    public boolean isAck;
    public boolean isRepeat;
    public int random;
    public int recvStatus;

    public SegmentInfo(int segNo) {
        this.segNo = segNo;
        this.isSend = false;
        this.isAck = false;
        this.isRepeat = false;
        segment = null;
    }

    public SegmentInfo(Segment segment) {
        this.segNo = segment.segNo;
        this.segment = segment;
        this.isSend = false;
        this.isAck = false;
        this.isRepeat = false;
    }

    public SegmentInfo(Segment segment, boolean isAck, boolean isSend, int recvStatus) {
        this.segNo = segment.segNo;
        this.segment = segment;
        this.isSend = isSend;
        this.isAck = isAck;
        this.recvStatus = recvStatus;
    }

    public SegmentInfo(Segment segment, int recvStatus, int random) {
        this.segNo = segment.segNo;
        this.segment = segment;
        this.isSend = false;
        this.isAck = false;
        this.recvStatus = recvStatus;
        this.random = random;
    }

    public SegmentInfo(Segment segment, boolean isAck, boolean isSend, boolean isRepeat) {
        this.segNo = segment.segNo;
        this.segment = segment;
        this.isSend = isSend;
        this.isAck = isAck;
        this.isRepeat = isRepeat;
    }
}
