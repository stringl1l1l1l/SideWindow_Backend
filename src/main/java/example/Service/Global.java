package example.Service;

import example.Entity.Segment;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class Global {
    public static int SERVER_PORT = 6666;
    public static int CLIENT_PORT = 6666;
    public static String CLIENT_IP = "127.0.0.1";
    public static int MSS = 100;
    public static int TYPE_ACK = 0;
    public static int TYPE_PACK = 1;
    public static int SEND_WIND = 4;
    public static int REC_WIND = 4;
    public static int MAX_CACHE_SIZE = 2000; // 缓冲中窗口最大个数
    public static int PERIOD_MS = 3000; // 定时周期
    public static int MAX_DATA_SIZE = 5000; // 一次允许发送的最大字符串长度
    public static int INIT_SEG_NO = 100; // 初始报文段序号
    public static int SLICE_SIZE = 10; // 分片数据部分大小
    public static int RECV_OK = 0;
    public static int RECV_LOST = 1;
    public static int RECV_ERROR = 2;
    public static int RECV_REPEAT = 3;
    public static int RECV_BAD_SEQUENCE = 4;
    public static int RECV_OTHER = -1;
    public static Semaphore hasSendPack = new Semaphore(0);
    public static Semaphore readyToSend = new Semaphore(0);
    public static Semaphore receiveDone = new Semaphore(0);
    public static ExecutorService executor = Executors.newCachedThreadPool();
}
