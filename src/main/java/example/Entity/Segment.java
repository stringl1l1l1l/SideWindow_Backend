package example.Entity;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Segment {

    private final int SOLID_LEN = 25;
    private static int accelerate = 100;
    public int type;
    public int segNo;
    public int ackNo;
    public String data;
    public int winSize;
    public int len;
    public int checksum;
    private final byte TAIL = -1;

    public Segment() {
    }

    public byte[] segStream;

    public Segment(int type, int ackNo, String data) {
        this.type = type;
        this.segNo = accelerate++;
        this.ackNo = ackNo;
        this.data = data;
        this.winSize = Global.SEND_WIND;
        this.checksum = 0;
        this.len = SOLID_LEN + data.length();
        this.segStream = serialize();
    }

    public Segment(int type, int ackNo, int winSize, int checksum, String data) {
        this.type = type;
        this.segNo = accelerate++;
        this.ackNo = ackNo;
        this.data = data;
        this.winSize = winSize;
        this.checksum = checksum;
        this.len = SOLID_LEN + data.length();
        this.segStream = serialize();
    }

    /**
     * 将报文段信息转为字节流，不足一个字节补零
     */
    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(Global.MSS);
        buffer.putInt(type);
        buffer.putInt(segNo);
        buffer.putInt(ackNo);
        buffer.putInt(len);
        buffer.putInt(winSize);
        buffer.putInt(checksum);
        buffer.put(data.getBytes(StandardCharsets.UTF_8));
        buffer.put(TAIL);
        // 不要多余的字节
        return Arrays.copyOfRange(buffer.array(), 0, buffer.position());
    }

    public void deserialize(byte[] stream) {
        ByteBuffer buffer = ByteBuffer.wrap(stream);
        this.type = buffer.getInt();
        this.segNo = buffer.getInt();
        this.ackNo = buffer.getInt();
        this.len = buffer.getInt();
        this.winSize = buffer.getInt();
        this.checksum = buffer.getInt();
        // 读取字符串
        this.data = new String(buffer.array(), buffer.position(), buffer.remaining(), StandardCharsets.UTF_8);
    }

    void calculateCheckSum() {

    }

    @Override
    public String toString() {
        return "Segment{" +
                "type=" + type +
                ", segNo=" + segNo +
                ", ackNo=" + ackNo +
                ", data='" + data + '\'' +
                ", len=" + len +
                ", winSize=" + winSize +
                ", checksum=" + checksum +
                '}';
    }
}
