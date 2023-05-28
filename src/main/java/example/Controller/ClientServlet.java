package example.Controller;

import com.google.gson.Gson;
import example.Entity.ExtraInfo;
import example.Model.JsonPack;
import example.Service.Global;
import example.Service.client.Client;
import example.Service.client.PackListener;
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
import java.util.concurrent.Semaphore;

@WebServlet("/client/*")
public class ClientServlet extends HttpServlet {
    private Client client = null;
    private final Gson gson = new Gson();
    private PackListener packListener = null;
    private final Logger log = Logger.getLogger(ClientServlet.class);
    private Thread receivePackThread = null;

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
        if (url.endsWith("/connect")) {
            client = new Client();
            client.startConnection(requestBodyJson.extra.host, requestBodyJson.extra.port);
            packListener = new PackListener(client);
            packListener.start();
            out.println(
                    GsonUtils.msg2Json(
                            HttpServletResponse.SC_OK,
                            "客户端已连接"
                                    + requestBodyJson.extra.host
                                    + ":"
                                    + requestBodyJson.extra.port));
            //        } else if (url.endsWith("/check")) {
            //            log.info("正在探测");
            //            try {
            //                Global.receiveDone.acquire();
            //            } catch (InterruptedException e) {
            //                e.printStackTrace();
            //            }
            //            String recData = client.receiveWindow.getReceivedData();
            //            out.println(
            //                    GsonUtils.msg2Json(
            //                            HttpServletResponse.SC_OK, "返回客户端已接收数据", new
            // ExtraInfo(recData)));
            //            log.info("探测完成");
        } else if (url.endsWith("/stop")) {
            try {
                if (client != null) {
                    if (packListener != null) packListener.interrupt();
                    client.stopConnection();
                    client = null;
                    if (receivePackThread != null) receivePackThread.interrupt();
                    receivePackThread = null;
                    packListener = null;
                    Global.receiveDone = new Semaphore(0);
                    out.println(GsonUtils.msg2Json(HttpServletResponse.SC_OK, "客户端已断开连接"));
                } else {
                    out.println(
                            GsonUtils.msg2Json(
                                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "客户端未连接"));
                }
            } catch (IOException e) {
                out.println(
                        GsonUtils.msg2Json(
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage()));
            }
        } else if (url.endsWith("/changeRecvWinSize")) {
            int size = requestBodyJson.extra.newRecvWinSize;
            if (client != null) {
                Global.SEND_WIND = size;
                client.receiveWindow.changeWindowSize(size);
                out.println(GsonUtils.msg2Json(HttpServletResponse.SC_OK, "接收窗口大小重设为" + size));
            } else {
                out.println(
                        GsonUtils.msg2Json(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "非法操作"));
            }
        }
    }
}
