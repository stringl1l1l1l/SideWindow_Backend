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
    }

    public Segment readByteStream2Segment() throws IOException
    {
        byte[] buffer = new byte[1024];
        byte byteRead = -1;
        int i = 0;
        while ((byteRead = in.readByte()) != -1)
            buffer[i++] = byteRead;

        byte[] newBuffer = new byte[i];
        for(int j = 0; j < i; j++)
            newBuffer[j] = buffer[j];

        System.out.println(Arrays.toString(newBuffer));
        Segment segment = new Segment();
        segment.deserialize(newBuffer);
        return segment;
    }

    public void sendByteStream(byte[] stream) throws IOException
    {
        out.write(stream);
        System.out.println("已发送字节流：");
        System.out.println(Arrays.toString(stream));
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
        Thread clientListenThd = new Thread() {
              // 子线程，持续监听是否有报文发送过来
              @Override
              public void run() {

                while (true) {
                  try {
                    if (client.in.available() != 0) {
                      Segment segment = client.readByteStream2Segment();
                      System.out.println("客户端接收到报文: " + segment.toString());
                    }
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                }

              }
            };
        clientListenThd.start();

        Semaphore semaphore = new Semaphore(Global.SEND_WIND);
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // 定时任务的具体操作
               semaphore.release();
            }
        };
        // 延迟 1 秒后开始执行任务，每隔 1 秒执行一次
        timer.schedule(task, 1000, 1000);

        int i = 101;
        while(true) {
          try {
              Segment segment1 = new Segment(Global.TYPE_ACK, i, 4, 4, "HELLO" + i);
              client.sendByteStream(segment1.segStream);
              semaphore.acquire();
          } catch (IOException | InterruptedException e) {
            e.printStackTrace();
          }
          i++;
        }
    }
}
