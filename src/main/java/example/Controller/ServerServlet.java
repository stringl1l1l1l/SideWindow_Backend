package example.Controller;

import com.google.gson.Gson;
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
    public void init() throws ServletException {
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Global.allowCors(req, resp);
        String url = req.getRequestURI();
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Global.allowCors(req, resp);
        super.doOptions(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Global.allowCors(req, resp);

        BufferedReader reader = req.getReader();
        PrintWriter out = resp.getWriter();

        // 解析请求体数据
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) requestBody.append(line);
        JsonPack requestBodyJson = gson.fromJson(String.valueOf(requestBody), JsonPack.class);

        String url = req.getRequestURI();
        if (url.endsWith("/start")) {
            // 创建监听线程
            server = new Server();
            ackListener = new ACKListener(server);
            scanner = new Scanner(server);
            // 在子线程中运行阻塞方法
            if (startThread == null) {
                startThread =
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    server.start(requestBodyJson.extra.port);
                                    // 运行监听线程
                                    ackListener.start();
                                    scanner.start();
                                } catch (IOException e) {
                                    if (!(e instanceof SocketException)) {
                                        out.println(
                                                GsonUtils.msg2Json(
                                                        HttpServletResponse
                                                                .SC_INTERNAL_SERVER_ERROR,
                                                        e.getMessage()));
                                    }
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
                if (startThread != null) {
                    server.stop();
                    startThread.interrupt();
                    if (sendThread != null) sendThread.interrupt();
                    if (ackListener != null) ackListener.interrupt();
                    if (scanner != null) scanner.cancel();
                    ackListener = null;
                    scanner = null;
                    startThread = null;
                    out.println(GsonUtils.msg2Json(HttpServletResponse.SC_OK, "服务端已关闭"));
                } else // 未检测到启动进程，说明没有启动
                out.println(
                            GsonUtils.msg2Json(
                                    HttpServletResponse.SC_SERVICE_UNAVAILABLE, "服务端未启动"));
            } catch (IOException e) {
                out.println(
                        GsonUtils.msg2Json(
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage()));
            }
        } else if (url.endsWith("/send")) {
            if (server != null) {
                server.sendMsg(requestBodyJson.extra.data);
                // 将旧发送线程断开
                if (sendThread != null) sendThread.interrupt();
                sendThread = new SendThread(server);
                sendThread.start();
                out.println(
                        GsonUtils.msg2Json(
                                HttpServletResponse.SC_OK, "发送成功: " + requestBodyJson.extra.data));
            }
        }
    }
}
