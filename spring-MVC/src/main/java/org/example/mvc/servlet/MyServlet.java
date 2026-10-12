package org.example.mvc.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 原生 HttpServlet：与 DispatcherServlet 并存的另一个 Servlet，映射路径 /my-servlet。
 * 说明：Spring MVC 只是 Servlet 的一种，MVC 处理不了的场景可以退回原生 Servlet。
 */
public class MyServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().write("这是原生 Servlet（非 DispatcherServlet），地址：" + req.getRequestURI()
                + "，参数 name=" + req.getParameter("name"));
    }
}
