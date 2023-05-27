package example.Service.server;

import example.Service.Global;
import example.Entity.ReceiveWindow;
import example.Entity.Segment;
import example.Entity.SendWindow;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;

public class Server {
    private ServerSocket server;
    private Socket client;
    public OutputStream out;
    public DataInputStream in;
    public SendWindow sendWindow;

    private final Logger log = Logger.getLogger(Server.class);

    public void start(int port) throws IOException {
        sendWindow = new SendWindow();
        server = new ServerSocket();
        server.setReuseAddress(true); // 开启复用TIME_WAIT状态端口
        server.bind(new InetSocketAddress(port));
        log.info("服务端启动，端口 " + port);
        client = server.accept(); // 阻塞当前线程，等待客户端连接，连接成功后返回连接到的客户端Socket
        log.info("监听到客户端连接");
        // 通过它向客户端输出
        out = client.getOutputStream();
        // 通过它获取客户端输入
        in = new DataInputStream(client.getInputStream());
    }

    /**
     * 发送报文段解析出的字节流
     *
     * @param stream 字节流
     */
    public void sendByteStream(byte[] stream) throws IOException {
        //        log.info("已发送字节流：");
        //        log.info(Arrays.toString(stream));
        out.write(stream);
        log.info("已发送报文：" + new Segment().deserialize(stream));
    }

    /**
     * 发送一个任意长度字符串，存入发送缓存区
     *
     * @param msg 任意长度的字符串
     */
    public synchronized void sendMsg(String msg) {
        Segment segment = new Segment(msg);
        ArrayList<Segment> pieces = segment.slice(Global.SLICE_SIZE);
        for (Segment piece : pieces) this.sendWindow.insertSegment(piece);
        this.sendWindow.changeWindowSize(Global.SEND_WIND);
    }

    /**
     * 读取客户端输入的字节流，并解析成报文段
     *
     * @return 解析出的报文段
     */
    public Segment readByteStream2Segment() {
        byte[] buffer = new byte[2048];
        int byteRead;
        int i = 0;
        int recentMinusOneCnt = 0; // 检查是否读到了连续的4个-1
        try {
            do {
                byteRead = in.read();
                buffer[i++] = (byte) byteRead;
                if (byteRead != 255) recentMinusOneCnt = 0;
                else recentMinusOneCnt++;
            } while (recentMinusOneCnt < 4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] newBuffer = new byte[i];
        System.arraycopy(buffer, 0, newBuffer, 0, i);
        Segment segment = new Segment();
        segment.deserialize(newBuffer);
        return segment;
    }

    public void stop() throws IOException {
        if (in != null) in.close();
        if (out != null) out.close();
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.setReuseAddress(true);
            server.close();
        }

        in = null;
        out = null;
        client = null;
        server = null;
        Segment.acceleratePACK = Global.INIT_SEG_NO;
        log.info("服务端已关闭");
    }

    public static void main(String[] args) throws IOException {
        // 初始化部分，启动发送端监听连接，并将一些报文段写入发送端缓存
        Server server = new Server();
        String data =
                "    /**\n"
                        + "     * 发送报文段解析出的字节流\n"
                        + "     * @param stream 字节流\n"
                        + "     *\n"
                        + "     */\n"
                        + "    public void sendByteStream(byte[] stream) throws IOException\n"
                        + "    {\n"
                        + "        System.out.println(\"已发送字节流：\");\n"
                        + "        System.out.println(Arrays.toString(stream));\n"
                        + "        System.out.println(\"已发送报文：\" + new Segment().deserialize(stream));\n"
                        + "        out.write(stream);\n"
                        + "    }\n"
                        + "\n"
                        + "    /**\n"
                        + "     * 发送一个任意长度字符串，存入发送缓存区\n"
                        + "     * @param msg 任意长度的字符串\n"
                        + "     */\n"
                        + "    public void sendMsg(String msg) {\n"
                        + "        Segment segment = new Segment(msg);\n"
                        + "        ArrayList<Segment> pieces = segment.slice(Global.SLICE_SIZE);\n"
                        + "        for(Segment piece : pieces)\n"
                        + "            this.sendWindow.insertSegment(piece);\n"
                        + "        this.sendWindow.changeWindowSize(Global.SEND_WIND);\n"
                        + "    }\n"
                        + "\n"
                        + "    /**\n"
                        + "     * 读取客户端输入的字节流，并解析成报文段\n"
                        + "     * @return 解析出的报文段\n"
                        + "     *\n"
                        + "     */\n"
                        + "    public Segment readByteStream2Segment() throws IOException\n"
                        + "    {\n"
                        + "        byte[] buffer = new byte[2048];\n"
                        + "        int byteRead;\n"
                        + "        int i = 0;\n"
                        + "        int recentMinusOneCnt = 0; // 检查是否读到了连续的4个-1\n"
                        + "        do {\n"
                        + "            byteRead = in.read();\n"
                        + "            buffer[i++] = (byte) byteRead;\n"
                        + "\n"
                        + "            if (byteRead != 255)\n"
                        + "                recentMinusOneCnt = 0;\n"
                        + "            else\n"
                        + "                recentMinusOneCnt++;\n"
                        + "        } while (recentMinusOneCnt < 4);\n"
                        + "\n"
                        + "        byte[] newBuffer = new byte[i];\n"
                        + "        System.arraycopy(buffer, 0, newBuffer, 0, i);\n"
                        + "\n"
                        + "        Segment segment = new Segment();\n"
                        + "        segment.deserialize(newBuffer);\n"
                        + "        return segment;\n"
                        + "    }\n"
                        + "\n"
                        + "    public void stop() throws IOException {\n"
                        + "        in.close();\n"
                        + "        out.close();\n"
                        + "        client.close();\n"
                        + "        server.close();\n"
                        + "    }";
        server.sendMsg(data);
        server.sendWindow.printSendWindow();
        server.start(Global.SERVER_PORT);

        Timer timer = new Timer();
        timer.schedule(new Scanner(server), Global.PERIOD_MS, Global.PERIOD_MS);

        ACKListener ackListener = new ACKListener(server);
        ackListener.start();

        SendThread sendThread = new SendThread(server);
        sendThread.start();
    }
}
