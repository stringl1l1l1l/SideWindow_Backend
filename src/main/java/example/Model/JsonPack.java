package example.Model;

import example.Entity.ExtraInfo;
import example.Entity.Segment;
import example.Entity.SegmentInfo;

import java.util.ArrayList;
import java.util.List;

public class JsonPack {
    public int status;
    public String msg;
    public ArrayList<SegmentInfo> segInfoList;
    public ExtraInfo extra;

    public JsonPack() {}

    public JsonPack(ExtraInfo extra) {
        this.extra = extra;
    }

    public JsonPack(int status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    public JsonPack(int status, String msg, ExtraInfo extra) {
        this.status = status;
        this.msg = msg;
        this.extra = extra;
    }

    public JsonPack(int status, String msg, ArrayList<Segment> list, ExtraInfo extra) {
        this.status = status;
        this.msg = msg;
        this.segInfoList = new ArrayList<>();
        for (Segment segment : list) {
            this.segInfoList.add(new SegmentInfo(segment));
        }
        this.extra = extra;
    }

    public JsonPack(int status, String msg, List<Segment> segList) {
        this.status = status;
        this.msg = msg;
        this.segInfoList = new ArrayList<>();
        for (Segment segment : segList) {
            this.segInfoList.add(new SegmentInfo(segment));
        }
    }
}
