package org.example.mvc.view;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.View;

import java.util.Map;

/**
 * 自定义 View：没有模板引擎时，把 Model 数据放进 request 属性，再 forward 到 static 下的静态页。
 * 用来演示 View / ViewResolver 的作用（视图渲染这一步是可替换的）。
 */
public class StaticView implements View {

    private final String resourcePath;

    public StaticView(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public String getContentType() {
        return "text/html;charset=UTF-8";
    }

    @Override
    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (model != null) {
            model.forEach(request::setAttribute);
        }
        RequestDispatcher rd = request.getRequestDispatcher(resourcePath);
        rd.forward(request, response);
    }
}
