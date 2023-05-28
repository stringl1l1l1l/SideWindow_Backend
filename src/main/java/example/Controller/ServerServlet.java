package example.Controller;

import com.google.gson.Gson;
import example.Entity.ExtraInfo;
import example.Service.Global;
import example.Model.JsonPack;
import example.Service.server.ACKListener;
import example.Service.server.Scanner;
import example.Service.server.SendThread;
import example.Service.server.Server;
import example.utils.GsonUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.Timer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@WebServlet("/server/*")
public class ServerServlet extends HttpServlet {
    private Server server;
    private final Gson gson = new Gson();
    private final Logger log = Logger.getLogger(ServerServlet.class);
    private Thread startThread = null;
    private ACKListener ackListener = null;
    private Scanner scanner = null;
    private SendThread sendThread = null;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        BufferedReader reader = req.getReader();
        PrintWriter out = resp.getWriter();

        // 解析请求体数据
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) requestBody.append(line);
        JsonPack requestBodyJson = gson.fromJson(String.valueOf(requestBody), JsonPack.class);

        String url = req.getRequestURI();
        if (url.endsWith("/start")) {
            server = new Server();
            ackListener = new ACKListener(server);
            scanner = new Scanner(server);

            // 创建发送线程
            if (sendThread == null) {
                sendThread = new SendThread(server);
                Global.executor.execute(sendThread);
            }

            // 在子线程中运行阻塞方法
            if (startThread == null) {
                startThread =
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    server.start(requestBodyJson.extra.port);
                                    Global.executor.execute(ackListener);
                                    Global.executor.execute(scanner);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                startThread.start();

                out.println(
                        GsonUtils.msg2Json(
                                HttpServletResponse.SC_OK,
                                "服务端启动，端口: " + requestBodyJson.extra.port));
            } else {
                out.println(
                        GsonUtils.msg2Json(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "请勿重复启动"));
            }

        } else if (url.endsWith("/stop")) {
            try {
                if (server != null) {
                    server.stop();
                    Global.executor.shutdownNow();
                    Global.hasSendPack = new Semaphore(0);
                    Global.readyToSend = new Semaphore(0);
                    Global.executor = Executors.newCachedThreadPool();
                    out.println(GsonUtils.msg2Json(HttpServletResponse.SC_OK, "服务端已关闭"));
                } else // 未检测到启动进程，说明没有启动
                out.println(
                            GsonUtils.msg2Json(
                                    HttpServletResponse.SC_SERVICE_UNAVAILABLE, "服务端未启动"));
                server = null;
                startThread = null;
                ackListener = null;
                scanner = null;
                sendThread = null;
            } catch (IOException e) {
                out.println(
                        GsonUtils.msg2Json(
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage()));
            }
        } else if (url.endsWith("/send")) {
            if (server != null) {
                server.sendMsg(requestBodyJson.extra.data);
                Global.readyToSend.release();
                out.println(GsonUtils.msg2Json(HttpServletResponse.SC_OK, "发送成功!"));
            }
        } else if (url.endsWith("/changeSendWinSize")) {
            int size = requestBodyJson.extra.newSendWinSize;
            if (server != null) {
                Global.SEND_WIND = size;
                server.sendWindow.changeWindowSize(size);
                out.println(GsonUtils.msg2Json(HttpServletResponse.SC_OK, "发送窗口大小重设为" + size));
            } else {
                out.println(
                        GsonUtils.msg2Json(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "非法操作"));
            }
        }
    }
}
