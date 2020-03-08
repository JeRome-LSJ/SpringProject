package com.jerome.mvcframework.v1.servlet;

import com.jerome.mvcframework.annotation.LsjAutowire;
import com.jerome.mvcframework.annotation.LsjController;
import com.jerome.mvcframework.annotation.LsjRequestMapping;
import com.jerome.mvcframework.annotation.LsjService;
import org.springframework.stereotype.Controller;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * @author liusj
 */

public class LsjDispatcherServlet extends HttpServlet {

    private Map<String, Object> mapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public LsjDispatcherServlet() {
        super();
    }



    @Override
    public void init(ServletConfig config) throws ServletException {
        InputStream is = null;
        try {
            Properties configContext = new Properties();
            //获取web.xml中配置文件地址
            is = this.getClass().getClassLoader().getResourceAsStream(config.getInitParameter("contextConfigLocation"));
            //加载配置文件
            configContext.load(is);
            String scanPackage = configContext.getProperty("scanPackage");
            //扫描包，并将所有class文件加入mapping
            doScanner(scanPackage);
            for (String className : mapping.keySet()) {
                if (!className.contains(".")) {
                    continue;
                }
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(LsjController.class)) {
                    /** 将所有@controller标记的类加入mapping */
                    mapping.put(className, clazz.newInstance());
                    String baseUrl = "";
                    if (clazz.isAnnotationPresent(LsjRequestMapping.class)) {
                        LsjRequestMapping requestMapping = clazz.getAnnotation(LsjRequestMapping.class);
                        baseUrl = requestMapping.value();
                    }
                    Method[] methods = clazz.getMethods();
                    for (Method method : methods) {
                        if (!method.isAnnotationPresent(LsjRequestMapping.class)) {
                            continue;
                        }
                        LsjRequestMapping requestMapping = method.getAnnotation(LsjRequestMapping.class);
                        String url = (baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                        /** 将所有含有requestMapping的方法都加入mapping */
                        mapping.put(url, method);
                        System.out.println("Mapped" + url + "," + method);
                    }
                } else if (clazz.isAnnotationPresent(LsjService.class)) {
                    LsjService service = clazz.getAnnotation(LsjService.class);
                    String beanName = service.value();
                    if ("".equals(beanName)) {
                        beanName = clazz.getName();
                    }
                    Object instance = clazz.newInstance();

                    /** 将所有@Service标记的类加入mapping */
                    mapping.put(beanName, instance);
                    for (Class<?> i : clazz.getInterfaces()) {
                        // TODO: 2020/3/7 待确认
                        mapping.put(i.getName(), instance);
                    }
                } else {
                    continue;
                }
            }
            for (Object object : mapping.values()) {
                if (Objects.isNull(object)) {
                    continue;
                }
                Class clazz = object.getClass();
                if (clazz.isAnnotationPresent(LsjController.class)) {
                    Field[] fields = clazz.getFields();
                    for (Field field : fields) {
                        if (!field.isAnnotationPresent(LsjAutowire.class)) {
                            continue;
                        }
                        LsjAutowire lsjAutowire = field.getAnnotation(LsjAutowire.class);
                        String beanName = lsjAutowire.value();
                        if ("".equals(beanName)) {
                            beanName = field.getType().getName();
                            field.setAccessible(true);
                            try {
                                field.set(mapping.get(clazz.getName()), mapping.get(beanName));
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("--------Lsj MVC Framework is init-----------");
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws InvocationTargetException, IllegalAccessException, IOException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        if (!this.mapping.containsKey(url)) {
            resp.getWriter().write("404 Not Found!!");
        }
        Method method = (Method) this.mapping.get(url);
        Map<String, String[]> params = req.getParameterMap();
        method.invoke(this.mapping.get(method.getDeclaringClass().getName()), new Object[]{req, resp, params.get("name")[0]});
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        assert url != null;
        File classDir = new File(url.getFile());
        for (File file : Objects.requireNonNull(classDir.listFiles())) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String clazzName = scanPackage + "." + file.getName().replace(".class", "");
                mapping.put(clazzName, null);
            }
        }
    }

}
