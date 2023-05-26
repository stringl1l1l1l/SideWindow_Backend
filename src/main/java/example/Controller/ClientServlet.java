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

@WebServlet("/client/*")
public class ClientServlet extends HttpServlet {
    private Client client = null;
    private final Gson gson = new Gson();
    private PackListener packListener = null;
    private final Logger log = Logger.getLogger(ClientServlet.class);

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Global.allowCors(req, resp);
        super.doOptions(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        super.doGet(req, resp);
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
        if (url.endsWith("/connect")) {
            client = new Client();
            packListener = new PackListener(client);
            client.startConnection(requestBodyJson.extra.host, requestBodyJson.extra.port);
            packListener.start();
            out.println(
                    GsonUtils.msg2Json(
                            HttpServletResponse.SC_OK,
                            "客户端已连接"
                                    + requestBodyJson.extra.host
                                    + ":"
                                    + requestBodyJson.extra.port));
        } else if (url.endsWith("/stop")) {
            try {
                if (client != null) {
                    if (packListener != null) packListener.interrupt();
                    client.stopConnection();
                    out.println(GsonUtils.msg2Json(HttpServletResponse.SC_OK, "已断开连接"));
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
        }
    }
}
