package example.Service;

import example.Entity.Global;
import example.Entity.ReceiveWindow;
import example.Entity.Segment;
import example.Entity.SendWindow;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

public class Client {
    private Socket clientSocket;
    private OutputStream out;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private DataInputStream in;
    private SendWindow sendWindow;
    private ReceiveWindow receiveWindow;

    public void startConnection(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        out = clientSocket.getOutputStream();
        printWriter = new PrintWriter(clientSocket.getOutputStream(),true);
        in = new DataInputStream(clientSocket.getInputStream());
        receiveWindow = new ReceiveWindow(Global.MAX_CACHE_SIZE);
    }

    public Segment readByteStream2Segment() throws IOException
    {
        byte[] buffer = new byte[1024];
        byte byteRead = -1;
        int i = 0;
        while ((byteRead = in.readByte()) != -1)
            buffer[i++] = byteRead;
        buffer[i++] = -1; // 读到-1会拒收，所以末尾加个-1

        byte[] newBuffer = new byte[i];
        for(int j = 0; j < i; j++)
            newBuffer[j] = buffer[j];

        System.out.println("客户端接收到字节流: " + Arrays.toString(newBuffer));
        return new Segment().deserialize(newBuffer);
    }

    public void sendByteStream(byte[] stream) throws IOException
    {
        out.write(stream);
//        System.out.println("已发送字节流：");
//        System.out.println(Arrays.toString(stream));
    }

    public void sendMessage(String msg) throws IOException {
        printWriter.println(msg);
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        System.out.println("客户端开始连接");
        client.startConnection(Global.CLIENT_IP, Global.CLIENT_PORT);
        Thread clientListenThd =
                new Thread() {
                    // 子线程，持续监听是否有报文发送过来
                    @Override
                    public void run() {
                        while (true) {
                            int ackNo = -1;
                            try {
                                // 发送端发送未完毕，继续
                                if (client.in.available() != 0) {
                                    Segment segment = client.readByteStream2Segment();
                                    System.out.println("客户端接收到报文: " + segment.toString());
                                    client.receiveWindow.capturePack(segment);
                                }
                                // 接收端返回对按序到达的最后一个报文段的ACK
                                else if ((ackNo = client.receiveWindow.needACK()) != -1) {
                                    Segment segment =
                                            new Segment(
                                                    Global.TYPE_ACK,
                                                    ackNo,
                                                    4,
                                                    4,
                                                    "Hello Server, ack " + ackNo);
                                    System.out.println("确认报文：" + segment.toString());
                                    client.sendByteStream(segment.segStream);
                                    client.receiveWindow.hasSendACK();
                                    client.receiveWindow.printReceivedData();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
        clientListenThd.start();
    }
}
