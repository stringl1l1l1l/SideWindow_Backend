package example.Service.client;

import example.Service.Global;
import example.Entity.ReceiveWindow;
import example.Entity.Segment;
import example.Entity.SendWindow;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class Client {
    private Socket clientSocket;
    public OutputStream out;
    public DataInputStream in;
    public ReceiveWindow receiveWindow;
    private final Logger log = Logger.getLogger(Client.class);

    public void startConnection(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        out = clientSocket.getOutputStream();
        in = new DataInputStream(clientSocket.getInputStream());
        receiveWindow = new ReceiveWindow(Global.MAX_CACHE_SIZE);
    }

    public Segment readByteStream2Segment() throws IOException {
        byte[] buffer = new byte[1024];
        int byteRead;
        int i = 0;
        int recentMinusOneCnt = 0; // 检查是否读到了连续的4个-1
        do {
            byteRead = in.read();
            buffer[i++] = (byte) byteRead;

            if (byteRead != 255) recentMinusOneCnt = 0;
            else recentMinusOneCnt++;
        } while (recentMinusOneCnt < 4);

        byte[] newBuffer = new byte[i];
        System.arraycopy(buffer, 0, newBuffer, 0, i);

        log.info("客户端接收到字节流: " + Arrays.toString(newBuffer));
        return new Segment().deserialize(newBuffer);
    }

    public void sendByteStream(byte[] stream) throws IOException {
        out.write(stream);
    }

    public void stopConnection() throws IOException {
        if (in != null) in.close();
        if (out != null) out.close();
        if (clientSocket != null) clientSocket.close();
        log.info("客户端断开连接");
    }

    public static void main(String[] args) throws IOException {
        Logger log = Logger.getLogger(Client.class);
        Client client = new Client();
        log.info("客户端开始连接");
        client.startConnection(Global.CLIENT_IP, Global.CLIENT_PORT);
        PackListener packListener = new PackListener(client);
        packListener.start();
    }
}
