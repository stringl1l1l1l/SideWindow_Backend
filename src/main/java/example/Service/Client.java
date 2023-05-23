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
        int byteRead;
        int i = 0;
        int recentMinusOneCnt = 0; // 检查是否读到了连续的4个-1
        do {
            byteRead = in.read();
            buffer[i++] = (byte) byteRead;

            if (byteRead != 255)
                recentMinusOneCnt = 0;
            else
                recentMinusOneCnt++;
        } while (recentMinusOneCnt < 4);

        byte[] newBuffer = new byte[i];
        System.arraycopy(buffer, 0, newBuffer, 0, i);

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
                                                    "Hello Server, ACK" + ackNo);
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
