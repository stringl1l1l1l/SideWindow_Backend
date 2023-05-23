package example.Service;

import example.Entity.Global;
import example.Entity.ReceiveWindow;
import example.Entity.Segment;
import example.Entity.SendWindow;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
    private ServerSocket server;
    private Socket client;
    private OutputStream out;
    private DataInputStream in;
    public SendWindow sendWindow;
    public ReceiveWindow receiveWindow;

    public Server() {
        sendWindow = new SendWindow();
        receiveWindow = new ReceiveWindow();
    }

    public void start(int port) throws IOException, InterruptedException {
        server = new ServerSocket(port);
        System.out.printf("服务端启动，端口%d\n", port);
        client = server.accept();// 阻塞，等待客户端连接，连接成功后返回连接到的客户端Socket
        System.out.println("连接成功");
        // 通过它向客户端输出
        out = client.getOutputStream();
        // 通过它获取客户端输入
        in = new DataInputStream(client.getInputStream());
    }

    /**
     * 发送报文段解析出的字节流
     * @param stream 字节流
     *
     */
    public void sendByteStream(byte[] stream) throws IOException
    {
        System.out.println("已发送字节流：");
        System.out.println(Arrays.toString(stream));
        System.out.println("已发送报文：" + new Segment().deserialize(stream));
        out.write(stream);
    }

    /**
     * 发送一个任意长度字符串，存入发送缓存区
     * @param msg 任意长度的字符串
     */
    public void sendMsg(String msg) {
        Segment segment = new Segment(msg);
        ArrayList<Segment> pieces = segment.slice(Global.SLICE_SIZE);
        for(Segment piece : pieces)
            this.sendWindow.insertSegment(piece);
    }

    /**
     * 读取客户端输入的字节流，并解析成报文段
     * @return 解析出的报文段
     *
     */
    public Segment readByteStream2Segment() throws IOException
    {
        byte[] buffer = new byte[2048];
        byte byteRead = -1;
        int i = 0;
        while ((byteRead = in.readByte()) != -1)
            buffer[i++] = byteRead;
        buffer[i++] = -1; // 读到-1会拒收，所以末尾加个-1

        byte[] newBuffer = new byte[i];
        for(int j = 0; j < i; j++)
            newBuffer[j] = buffer[j];

        Segment segment = new Segment();
        segment.deserialize(newBuffer);
        return segment;
    }

    public void stop() throws IOException {
        in.close();
        out.close();
        client.close();
        server.close();
    }

    public static void main(String[] args) throws IOException {
        // 初始化部分，启动发送端监听连接，并将一些报文段写入发送端缓存
        Server server = new Server();
        // 开启服务器前先初始化发送缓存，因为服务器开启后会阻塞该线程
        server.sendMsg("windowSize = Global.REC_WIND;\n"
                                + "        posBeg = 0;\n"
                                + "        posCur = 1;\n"
                                + "        posEnd = windowSize;\n"
                                + "        cacheSize = 0;\n"
                                + "        segmentList = new SegmentInfo[Global.MAX_CACHE_SIZE];");
        server.sendWindow.printSendWindow();

        try {
            server.start(Global.SERVER_PORT);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        Timer timer = new Timer();
        // 定时器线程，每隔一定时间扫描一次发送窗口的已发送部分，检查是否已经ACK
        TimerTask task =
                new TimerTask() {
                    @Override
                    public void run() {
                        // 定时时间到，如果当前发送窗口中有未确认报文段，就将它重传
                        if (!server.sendWindow.isAllAck()) {
                            for (int i = server.sendWindow.getPosBeg();
                                    i < server.sendWindow.getPosCur();
                                    i++) {
                                Segment curSeg = server.sendWindow.getSpecifiedSegment(i);
                                try {
                                    System.out.println(Global.PERIOD_MS/1000 + "s内未收到ACK，超时重传：");
                                    server.sendByteStream(curSeg.serialize());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                };
        timer.schedule(task, Global.PERIOD_MS, Global.PERIOD_MS);

        // 子线程，持续监听接受端是否有报文发送过来，如果是ACK报文就收下，并做一些判断处理
        Thread ackListenThd = new Thread() {
            @Override
            public void run() {
                while(true) {
                    try {
                        // 当前有数据发送过来
                        if (server.in.available() != 0) {
                            Segment segment = server.readByteStream2Segment();
                            System.out.println("服务端接收到报文: " + segment.toString());
                            server.sendWindow.captureACK(segment);
                            server.sendWindow.printSendWindow();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        ackListenThd.start();

        // 主线程持续监听是否有可发送报文
        while(true) {
            // 获取当前所有可发送的报文段
            if(server.sendWindow.isAvailable()) {
                ArrayList<Segment> segments = server.sendWindow.getAvailable();
                if (!segments.isEmpty()) {
                    // 如有可发送报文段，将这些报文段序列化成字节流，发送到接收端
                    for (Segment curSeg : segments) {
                        server.sendByteStream(curSeg.serialize());
                    }
                }
            }
        }
    }
}
