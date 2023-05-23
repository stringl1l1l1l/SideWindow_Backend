package example.Entity;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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
    private final int TAIL = 0xffffffff;

    public byte[] segStream;

    public Segment() {

    }

    public Segment(Segment segment) {
        this.segStream = segment.segStream;
        this.checksum = segment.checksum;
        this.segNo = segment.segNo;
        this.type = segment.type;
        this.data = segment.data;
        this.ackNo = segment.ackNo;
        this.winSize = segment.winSize;
        this.len = segment.len;
    }

    public Segment(String data) {
        this.type = Global.TYPE_PACK;
        this.segNo = accelerate++;
        this.ackNo = 0;
        this.data = data;
        this.winSize = Global.SEND_WIND;
        this.len = SOLID_LEN + data.length();
        this.checksum = 0;
        this.segStream = serialize();
        this.calculateCheckSum();
    }

    public Segment(int type, int ackNo, String data) {
        this.type = type;
        this.segNo = accelerate++;
        this.ackNo = ackNo;
        this.data = data;
        this.winSize = Global.SEND_WIND;
        this.checksum = 0;
        this.len = SOLID_LEN + data.length();
        this.calculateCheckSum();
        this.segStream = serialize();
    }

    public Segment(int type, int ackNo, int winSize, int checksum, String data) {
        this.type = type;
        this.segNo = accelerate++;
        this.ackNo = ackNo;
        this.data = data;
        this.winSize = winSize;
        this.len = SOLID_LEN + data.length();
        this.checksum = 0;
        this.calculateCheckSum();
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
        buffer.putInt(TAIL);
        // 不要多余的字节
        byte[] bytes = Arrays.copyOfRange(buffer.array(), 0, buffer.position());
        this.segStream = bytes;
        return bytes;
    }

    public Segment deserialize(byte[] stream) {
        ByteBuffer buffer = ByteBuffer.wrap(stream);
        this.segStream = stream;

        this.type = buffer.getInt();
        this.segNo = buffer.getInt();
        this.ackNo = buffer.getInt();
        this.len = buffer.getInt();
        this.winSize = buffer.getInt();
        this.checksum = buffer.getInt();
        // 读取字符串
        this.data = new String(buffer.array(), buffer.position(), buffer.remaining() - 4, StandardCharsets.UTF_8);
        return this;
    }

    /**
     *
     * @param piece_size 分片后的报文数据段最大长度(单位: char 2Byte)
     * @return 分片后应传输的报文段列表
     */
    public ArrayList<Segment> slice(int piece_size) {
        // 被分片的报文段序号作废
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

    public void calculateCheckSum() {
        long checksum = 0;
        ByteBuffer buffer = ByteBuffer.wrap(this.serialize());
        while (buffer.remaining() >= 4) {
            checksum += buffer.getInt();
        }

        // 最后几字节不足一int，补0
        if (buffer.hasRemaining()) {
            int lastInt = 0;
            int remain = buffer.remaining();
            for(int i = 0; i < remain; i++) {
                byte b = buffer.get();
                lastInt |= (b << (8 * (3 - i)));
            }
            checksum += lastInt;
        }

        // 将溢出的位回卷至低位
        checksum += (checksum >> 32);
        this.checksum = ~((int)checksum);
    }

    /**
     * 根据收到的报文段计算校验和是否一致，判断是否出错
     * @return 错误返回true
     */
    public boolean hasError() {
        int recCheckSum = this.checksum;

        // 将检验和字段置0，重新计算检验和
        this.checksum = 0;
        this.calculateCheckSum();

        // 查看新计算的检验和是否和原来一致
        int calCheckSum = this.checksum;
        this.checksum = recCheckSum;// 恢复检验和字段
        if (recCheckSum != calCheckSum) {
            System.out.println("接收到的校验和: " + recCheckSum + ", " + "计算出的校验和: " + calCheckSum);
            System.out.println("该报文段出错了: " + this.toString());
        }
        return recCheckSum != calCheckSum;
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
