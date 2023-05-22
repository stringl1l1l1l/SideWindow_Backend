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
    public static final int MAX_CACHE_SIZE = 2000;
    public static final int PERIOD_MS = 3000;
    public static final int MAX_DATA_SIZE = 5000;
    public static final int INIT_SEG_NO = 100;

}
