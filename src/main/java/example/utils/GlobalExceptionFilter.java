package example.utils;

import example.Service.Global;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

public class GlobalExceptionFilter implements Filter {
    private final Logger log = Logger.getLogger(GlobalExceptionFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化操作
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // 继续传递请求到下一个过滤器或 Servlet
            allowCors((HttpServletRequest) request, (HttpServletResponse) response);
            chain.doFilter(request, response);
        } catch (Throwable throwable) {
            // 处理异常
            // 例如，将异常信息记录到日志文件或发送通知等
            PrintWriter out = response.getWriter();
            // 获取详细错误信息
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            throwable.printStackTrace(printWriter);
            String throwableAsString = stringWriter.toString();
            //            log.error(throwableAsString);
            throwable.printStackTrace();
            out.println(
                    GsonUtils.msg2Json(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR, throwableAsString));
        }
    }

    @Override
    public void destroy() {
        // 过滤器销毁时的操作
    }

    /** 配置响应，允许跨域请求 */
    public static void allowCors(HttpServletRequest request, HttpServletResponse response) {
        try {
            request.setCharacterEncoding("utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        // 允许跨域的请求方法GET, POST, HEAD 等
        response.setHeader("Access-Control-Allow-Methods", "*");
        // 重新预检验跨域的缓存时间 (s)
        response.setHeader("Access-Control-Max-Age", "4200");
        // 允许跨域的请求头
        response.setHeader("Access-Control-Allow-Headers", "*");
    }
}
