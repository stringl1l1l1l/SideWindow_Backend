package example.Entity;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class Segment {

    public static final int SOLID_LEN = 25;
    private static int accelerate = Global.INIT_SEG_NO;
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

    public Segment(String data) {
        this.type = Global.TYPE_PACK;
        this.segNo = accelerate++;
        this.ackNo = 0;
        this.data = data;
        this.winSize = Global.SEND_WIND;
        this.checksum = 0;
        this.len = SOLID_LEN + data.length();
        this.segStream = serialize();
    }

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
     * 将报文段信息转为字节流，不足一个字节补零，末尾添加TAIL表示界符
     */
    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(Global.MAX_DATA_SIZE);
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

    /**
     *
     * @param piece_size 分片后的报文数据段最大长度(单位: char 2Byte)
     * @return 分片后应传输的报文段列表
     */
    public ArrayList<Segment> slice(int piece_size) {

        accelerate--;
        int data_size = this.data.length();
        ArrayList<Segment> arrayList = new ArrayList<>();
        char[] charStrData = this.data.toCharArray();
        int k = 0; // data指针
        while(k < data_size)
        {
            int cnt = Math.min(piece_size, data_size - k);
            String piece_data = String.copyValueOf(charStrData, k, cnt);
            Segment piece = new Segment(piece_data);
            arrayList.add(piece);
            k += cnt;
        }
        return arrayList;
    }

    void calculateCheckSum() {

    }

    @Override
    public String toString() {
        return "Segment{" +
                "type=" + ((type == Global.TYPE_ACK) ? "ACK" : "PACK") +
                ", segNo=" + segNo +
                ", ackNo=" + ackNo +
                ", data='" + data + '\'' +
                ", len=" + len +
                ", winSize=" + winSize +
                ", checksum=" + checksum +
                '}';
    }
}
