package com.jerome.demo.mvc.action;

import com.jerome.demo.service.IDemoService;
import com.jerome.mvcframework.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author liusj
 */

@LsjController
@LsjRequestMapping("/demo")
public class DemoAction {
    @LsjAutowire
    IDemoService demoService;

    @LsjRequestMapping(value = "/query")
    public void query(HttpServletRequest req, HttpServletResponse resp,
                      @LsjRequestParam("name") String name) {
        String result = demoService.get(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @LsjRequestMapping(value = "/add")
    public void add(HttpServletRequest req, HttpServletResponse resp,
                    @LsjRequestParam("a") Integer a, @LsjRequestParam("b") Integer b) {
        try {
            resp.getWriter().write(a + "+" + b + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @LsjRequestMapping(value = "/remove")
    public void remove(HttpServletRequest req, HttpServletResponse resp,
                       @LsjRequestParam("id") Integer id) {

    }
}
