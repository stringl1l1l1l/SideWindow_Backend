package example.Service;

import example.Entity.Global;
import example.Entity.ReceiveWindow;
import example.Entity.Segment;
import example.Entity.SendWindow;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public void sendByteStream(byte[] stream) throws IOException
    {
        System.out.println("已发送字节流：");
        System.out.println(Arrays.toString(stream));
        out.write(stream);
    }

    public String readByteStream2String() throws IOException
    {
        return in.readUTF();
    }

    public Segment readByteStream2Segment() throws IOException
    {
        byte[] buffer = new byte[2048];
        byte byteRead = -1;
        int i = 0;
        while ((byteRead = in.readByte()) != -1)
            buffer[i++] = byteRead;

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
        Server server = new Server();

        for (int i = 0; i < 6; i++) {
            Segment segment = new Segment(Global.TYPE_PACK, 0, "hello" + i);
            server.sendWindow.insertSegment(segment);
        }
        server.sendWindow.printSendWindow();

        try {
            server.start(Global.SERVER_PORT);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        Thread ackListenThd = new Thread() {
            // 子线程，持续监听是否有报文发送过来
            @Override
            public void run() {
                while(true) {
                    try {
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

        // 获取当前所有可发送的报文段
        while(true) {
            ArrayList<Segment> segments = server.sendWindow.getAvailable();
            if (!segments.isEmpty()) {
                // 将这些报文段打包成字节流，发送到接收端
                byte[] stream = new byte[2048];

                int i = 0;
                for (int j = 0; j < segments.size(); j++) {
                    Segment curSeg = segments.get(j);
                    byte[] curSegBytes = curSeg.serialize();
                    for (int k = 0; k < curSeg.len; k++) {
                        stream[i++] = curSegBytes[k];
                    }
                }
                stream = Arrays.copyOfRange(stream, 0, i);
                server.sendByteStream(stream);
            }
        }
    }
}
