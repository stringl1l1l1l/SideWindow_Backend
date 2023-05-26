package example.utils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class GlobalExceptionFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化操作
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // 继续传递请求到下一个过滤器或 Servlet
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
            out.println(
                    GsonUtils.msg2Json(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR, throwableAsString));
        }
    }

    @Override
    public void destroy() {
        // 过滤器销毁时的操作
    }
}
