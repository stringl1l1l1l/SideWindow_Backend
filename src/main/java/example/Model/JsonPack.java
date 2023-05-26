package example.Model;

import example.Entity.ExtraInfo;
import example.Entity.Segment;
import example.Entity.SegmentInfo;

public class JsonPack {
    public int status;
    public String msg;
    public SegmentInfo data;
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

    public JsonPack(int status, String msg, SegmentInfo data, ExtraInfo extra) {
        this.status = status;
        this.msg = msg;
        this.data = data;
        this.extra = extra;
    }
}
