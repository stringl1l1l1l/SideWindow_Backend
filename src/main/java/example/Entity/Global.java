package example.Entity;

import java.util.concurrent.Semaphore;

public class Global {
    public static final int SERVER_PORT = 6666;
    public static final String SERVER_IP = "127.0.0.1";
    public static final int CLIENT_PORT = 6666;
    public static final String CLIENT_IP = "127.0.0.1";
    public static final int MSS = 100;
    public static final int TYPE_ACK = 0;
    public static final int TYPE_PACK = 1;
    public static final int SEND_WIND = 4;
    public static final int REC_WIND = 4;
    public static final int MAX_CACHE_SIZE = 2000; // 缓冲中窗口最大个数
    public static final int PERIOD_MS = 3000; // 定时周期
    public static final int MAX_DATA_SIZE = 5000; // 一次允许发送的最大字符串长度
    public static final int INIT_SEG_NO = 100; // 初始报文段序号
    public static final int SLICE_SIZE = 10; // 分片数据部分大小

}
