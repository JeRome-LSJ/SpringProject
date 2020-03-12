package com.jerome.mvcframework.v2.servlet;

import com.jerome.mvcframework.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author liusj
 */
public class LsjDispatcherServlet extends HttpServlet {

    Logger log = LoggerFactory.getLogger(LsjDispatcherServlet.class);
    /**
     * 用来保存application.properties配置文件中的内容
     */
    private Properties contextConfig = new Properties();
    /**
     * 保存扫描的所有类名
     */
    private List<String> classNames = new ArrayList<>();
    /**
     * IoC容器
     */
    private Map<String, Object> ioc = new HashMap<>();
    /**
     * 保存url和 method的关系
     */
    private Map<String, Method> handlerMapping = new HashMap<>();

    public LsjDispatcherServlet() {
        super();
    }

    /**
     * 初始化阶段
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        // 1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        // 2.扫描相关类
        doScanner(contextConfig.getProperty("scanPackage"));
        // 3.初始化扫描到的类，并且将他们放入Ioc容器中
        doInstance();
        // 4.完成依赖注入
        doAutowire();
        // 5.初始化HandlerMapping
        initHandlerMapping();
    }

    private void initHandlerMapping() {
        if (CollectionUtils.isEmpty(ioc)) {
            return;
        }
        for (Map.Entry entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(LsjController.class)) {
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(LsjRequestMapping.class)) {
                baseUrl = clazz.getAnnotation(LsjRequestMapping.class).value();
            }
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(LsjRequestMapping.class)) {
                    continue;
                }
                LsjRequestMapping requestMapping = method.getAnnotation(LsjRequestMapping.class);
                String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                System.out.println("Mapped:" + url + "," + method);
            }
        }
    }

    /**
     * 完成依赖注入
     */
    private void doAutowire() {
        if (CollectionUtils.isEmpty(ioc)) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //获取所有字段，包括private，protected，default类型
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(LsjAutowire.class)) {
                    continue;
                }
                LsjAutowire autowire = field.getAnnotation(LsjAutowire.class);
                String beanName = autowire.value().trim();
                if ("".equals(beanName)) {
                    System.out.println(field.getName());
                    beanName = field.getType().getName();
                }
                //如果是public以外的类型，只要加了Autowire注解，都要强制赋值
                //反射中叫暴力访问
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 实例化所有对象
     */
    private void doInstance() {
        if (CollectionUtils.isEmpty(classNames)) {
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(LsjController.class)) {
                    String beanName = clazz.getAnnotation(LsjController.class).value();
                    if ("".equals(beanName.trim())) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    ioc.put(beanName, clazz.newInstance());
                }else if (clazz.isAnnotationPresent(LsjService.class)) {
                    String beanName = clazz.getAnnotation(LsjService.class).value();
                    if ("".equals(beanName.trim())) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("The " + i.getName() + " is exists!!");
                        }
                        ioc.put(i.getName(), instance);
                    }
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 扫描配置文件中的包路径，将路径下class所有类名加入容器中
     *
     * @param scanPackage
     */
    private void doScanner(String scanPackage) {
        //转换文件路径，就是把.替换成/
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        assert url != null;
        File classPath = new File(url.getFile());
        for (File file : Objects.requireNonNull(classPath.listFiles())) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                } else {
                    String className = scanPackage + "." + file.getName().replace(".class", "");
                    classNames.add(className);
                }
            }
        }
    }

    /**
     * 加载配置文件
     *
     * @param contextConfigLocation
     */
    private void doLoadConfig(String contextConfigLocation) {
        //
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(stream);
        } catch (IOException e) {
            log.error("LsjDispatcherServlet.init load resource error.");
        } finally {
            if (null != stream) {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.error("stream close error.");
                }
            }

        }

    }

    /**
     * 静态处理url参数
     * @param req
     * @param resp
     * @throws Exception
     */
    @Deprecated
    private void doDispatch_1(HttpServletRequest req, HttpServletResponse resp)throws Exception {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 Not Found!!");
        }
        Method method = this.handlerMapping.get(url);
        Map<String, String[]> params = req.getParameterMap();
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName), new Object[]{req, resp, params.get("name")[0]});
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 Not Found!!");
        }
        Method method = this.handlerMapping.get(url);
        // 第一个参数，方法所在实例
        // 第二个参数，调用时所需要的实参
        Map<String, String[]> params = req.getParameterMap();
        // 获取方法的形参列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        // 保存请求的url参数列表
        Map<String, String[]> parameterMap = req.getParameterMap();
        // 保存赋值参数的位置
        Object[] paramValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class) {
                paramValues[i] = req;
            } else if (parameterType == HttpServletResponse.class) {
                paramValues[i] = resp;
            } else if (parameterType == String.class) {
                // 提取方法中加了注解的参数
                Annotation[][] pa = method.getParameterAnnotations();
                for (int j = 0; j < pa.length; j++) {
                    for (Annotation a : pa[j]) {
                        if (a instanceof LsjRequestParam) {
                            String paramName = ((LsjRequestParam) a).value();
                            if (!"".equals(paramName.trim())) {
                                String value = Arrays.toString(parameterMap.get(paramName))
                                        .replaceAll("\\[|\\]", "")
                                        .replaceAll("\\s", ",");
                                paramValues[i] = value;
                            }
                        }
                    }
                }
            }
        }
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName), new Object[]{req, resp, params.get("name")[0]});
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception, Detail: " + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPut(req, resp);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.service(req, resp);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }
}
