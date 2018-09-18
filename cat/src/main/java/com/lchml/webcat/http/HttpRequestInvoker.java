package com.lchml.webcat.http;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lchml.webcat.annotation.*;
import com.lchml.webcat.config.WebcatHttpConf;
import com.lchml.webcat.ex.WebcatInitException;
import com.lchml.webcat.util.*;
import io.netty.handler.codec.http.*;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
 * invoke the real http request handler
 *
 * Created by lc on 11/11/17.
 */
@Component
public class HttpRequestInvoker {
    private final static Logger logger = LoggerFactory.getLogger(HttpRequestInvoker.class);

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private WebcatHttpConf webcatHttpConf;

    /**
     * Controller名称 和 对应的 controller对象
     * testHttpController - TestHttpController对象
     */
    private Map<String, Object> controllerMap; // controllerName - bean :

    /**
     * 请求url -  对应的Controller名称
     * "/test/redirect" -> "testHttpController"
     * "/test/body" -> "testHttpController"
     * "/test/hello" -> "testHttpController"
     */
    private Map<String, String> pathMapping; // path - controllerName

    /**
     * 请求url - 对应的执行方法Method
     *
     * "/test/redirect" -> "public void com.lchml.test.controller.TestHttpController.testRedirect(io.netty.handler.codec.http.FullHttpResponse)"
     * "/test/body" -> "public java.lang.String com.lchml.test.controller.TestHttpController.testBody(java.lang.String)"
     * "/test/hello" -> "public java.lang.Object com.lchml.test.controller.TestHttpController.testHello()"
     */
    private Map<String, Method> methodMap; // path - method:
    /**
     * 请求url - 对应的方法注解
     * "/test/redirect" -> "@com.lchml.webcat.annotation.HttpRequestMapping(produces=[], path=/redirect, headers=[], method=[GET], consumes=[])"
     * "/test/body" -> "@com.lchml.webcat.annotation.HttpRequestMapping(produces=[], path=/body, headers=[], method=[POST], consumes=[])"
     * "/test/hello" -> "@com.lchml.webcat.annotation.HttpRequestMapping(produces=[], path=/hello, headers=[], method=[], consumes=[])"
     */
    private Map<String, HttpRequestMapping> requesAnnotMap; // path - requestAnno
    /**
     * 请求url - 方法的参数列表（索引 - 方法的参数的名称）
     * "/test/redirect" -> " size = 1" ("0" -> "response")
     *
     * "/test/body" -> " size = 1" ("0" -> "body")
     * "/test/hello" -> " size = 0"
     */
    private Map<String, Map<Integer, String>> methodParamIndexMap;
    /**
     * 请求url - ReqParam
     */
    private Map<String, Map<Integer, ReqParam>> methodParamAnnoMap;
    /**
     *  请求url - 注解中方法参数上的ReqBody注解
     *  "/test/redirect" -> " size = 0"
     *  "/test/body" -> " size = 1" ( "0" -> "@com.lchml.webcat.annotation.ReqBody(required=true)")
     *  "/test/hello" -> " size = 0"
     */
    private Map<String, Map<Integer, ReqBody>> methodBodyAnnoMap;



    @PostConstruct
    public void init() {
        methodMap = Maps.newHashMap();
        pathMapping = Maps.newHashMap();
        methodParamIndexMap = Maps.newHashMap();
        methodParamAnnoMap = Maps.newHashMap();
        controllerMap = Maps.newHashMap();
        requesAnnotMap = Maps.newHashMap();
        methodBodyAnnoMap = Maps.newHashMap();
        Map<String, Object> controllers = beanFactory.getBeansWithAnnotation(HttpController.class);
        for (Map.Entry<String, Object> entry : controllers.entrySet()) {
            controllerMap.put(entry.getKey(), entry.getValue());
            initPathMapping(entry.getValue(), entry.getKey());
        }
    }

    private void initPathMapping(Object controller, String controllerName) {
        Class<?> clazz = controller.getClass();
        HttpController controllerAnno = controller.getClass().getAnnotation(HttpController.class);
        while (clazz != Object.class) {
            Method[] methods = clazz.getDeclaredMethods();
            for (final Method method : methods) {
                // 查找 公共方法 和 被HttpRequestMapping注解的 方法
                if (method.getModifiers() == Modifier.PUBLIC && method.isAnnotationPresent(HttpRequestMapping.class)) {
                    HttpRequestMapping methodAnno = method.getAnnotation(HttpRequestMapping.class);
                    if (Strings.isNullOrEmpty(methodAnno.path())) { // url 非空
                        throw new WebcatInitException("HttpRequestMapping path can't be empty");
                    }
                    // 将HttpController注解上的path和HttpRequestMapping注解上的path 合并在一起组成新的path
                    String fullPath = UrlUtil.resolveUrlPath(controllerAnno.path()) + UrlUtil.resolveUrlPath(methodAnno.path());
                    if (methodMap.containsKey(fullPath)) { //
                        throw new WebcatInitException("HttpRequestMapping path must be unique");
                    }
                    requesAnnotMap.put(fullPath, methodAnno); // 请求url - 对应的方法注解
                    methodMap.put(fullPath, method); // 请求url - 对应的执行方法Method
                    try {
                        Map<Integer, String> paramIndex = Maps.newHashMap();
                        Map<Integer, ReqParam> paramAnno = Maps.newHashMap();
                        Map<Integer, ReqBody> bodyAnno = Maps.newHashMap();
                        MethodUtil.getParameterMap(method, paramIndex, paramAnno, bodyAnno);
                        if (bodyAnno.size() > 1) {
                            throw new WebcatInitException("HttpRequestMapping method can't with two body param");
                        }
                        methodParamIndexMap.put(fullPath, paramIndex);
                        methodParamAnnoMap.put(fullPath, paramAnno);
                        methodBodyAnnoMap.put(fullPath, bodyAnno);
                    } catch (NotFoundException e) {
                        logger.error(e.getMessage(), e);
                    }
                    pathMapping.put(fullPath, controllerName);
                }
            }
            clazz = clazz.getSuperclass(); // 循环查询它的父类
        }
    }

    public boolean validPath(String path) {
        return methodMap.containsKey(path);
    }

    public void invoke(String path, HttpRequestData data, FullHttpRequest request, FullHttpResponse response) {
        // 根据path获取可以处理这个请求Method和Object controller
        Method method = methodMap.get(path);
        WebcatLog.setMethod(method.getName());
        String controllerName = pathMapping.get(path);
        Object controller = controllerMap.get(controllerName);
        try {
            HttpRequestMapping mapping = requesAnnotMap.get(path);
            assert mapping != null;
            // 查询请求是否符合要求
            if (!checkRequest(mapping, request, response)) {
                return;
            }
            List<Object> convertedArgs = Lists.newArrayList();
            // 解析出请求中的参数，并将参数封装到convertedArgs中
            if (!assembleParam(convertedArgs, data, method.getParameterTypes(), methodParamIndexMap.get(path),
                methodParamAnnoMap.get(path), methodBodyAnnoMap.get(path), request, response)) {
                return;
            }

            // 执行真实的方法
            Object result = method.invoke(controller, convertedArgs.toArray(new Object[convertedArgs.size()]));

            // 配置返回的content type值
            if (mapping.produces().length > 0) {
                ResponseUtil.contentType(response, Joiner.on(",").join(mapping.produces()));
            } else {
                // default content-type
                ResponseUtil.contentType(response, webcatHttpConf.getDefaultProduce());
            }

            if (response.status() == null || response.status() == HttpResponseStatus.CONTINUE) {
                // 如果controller没有设置过response status,则默认设置为200
                ResponseUtil.status(response, HttpResponseStatus.OK);
            }
            // 设置response content
            if (result != null && !void.class.isAssignableFrom(result.getClass()) && !Void.class.isAssignableFrom(result.getClass())) {
                if (webcatHttpConf.isLogResponse()) {
                    WebcatLog.setResponse(result);
                }
                if (String.class.isAssignableFrom(result.getClass())) {
                    ResponseUtil.content(response, (String) result);
                } else {
                    ResponseUtil.content(response, JsonUtil.toJson(result));
                }

            }
        } catch (Exception e) {
            ResponseUtil.status(response, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            logger.error(e.getMessage(), e);
        }
    }

    private static boolean checkRequest(HttpRequestMapping mapping, FullHttpRequest request, FullHttpResponse response) {
        // Http方法是否支持
        if (!RequestUtil.supportMethod(mapping.method(), request.method().name())) {
            ResponseUtil.notSupportMehotd(response, request.method().name());
            return false;
        }
        // 对应的content type是否支持
        String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (!RequestUtil.supportContentType(mapping.consumes(), contentType)) {
            ResponseUtil.notSupportContentType(response, contentType);
            return false;
        }
        // 必须的header头是否存在
        String missHeader = RequestUtil.checkHeaders(mapping.headers(), request.headers());
        if (missHeader != null) {
            ResponseUtil.missHeader(response, missHeader);
            return false;
        }
        return true;
    }

    private boolean assembleParam(List<Object> convertedArgs, HttpRequestData data, Class<?>[] parameterTypes, Map<Integer, String> paramsIndex,
        Map<Integer, ReqParam> paramsAnno, Map<Integer, ReqBody> bodyAnno, FullHttpRequest request, FullHttpResponse response) {
        assert paramsIndex != null && paramsAnno != null;
        Map<String, String> args = data.getParams();
        for (Integer i = 0; i < parameterTypes.length; i++) {
            // 处理Long
            if (Long.class.isAssignableFrom(parameterTypes[i]) || long.class.isAssignableFrom(parameterTypes[i])) {
                String arg = args.get(paramsIndex.get(i)); // 先从获取变量名称，然后再根据名称从请求参数中获取值
                if (arg == null) {
                    // 如果没有，则查询ReqParam注解：如果有配置ReqParam且参数非必须，则设置默认值; 否则报错
                    ReqParam anno = paramsAnno.get(i);
                    if (anno != null && !anno.required()) {
                        convertedArgs.add(Long.parseLong(anno.defaultValue()));
                    } else {
                        ResponseUtil.missParam(response, paramsIndex.get(i));
                        return false;
                    }
                } else {
                    Double num = NumUtil.toDouble(arg);
                    convertedArgs.add(num.longValue());
                }
            } else if (Integer.class.isAssignableFrom(parameterTypes[i]) || int.class.isAssignableFrom(parameterTypes[i])) {
                String arg = args.get(paramsIndex.get(i));
                if (arg == null) {
                    ReqParam anno = paramsAnno.get(i);
                    if (anno != null && !anno.required()) {
                        convertedArgs.add(Integer.parseInt(anno.defaultValue()));
                    } else {
                        ResponseUtil.missParam(response, paramsIndex.get(i));
                        return false;
                    }
                } else {
                    Double num = NumUtil.toDouble(arg);
                    convertedArgs.add(num.intValue());
                }
            } else if (Double.class.isAssignableFrom(parameterTypes[i]) || double.class.isAssignableFrom(parameterTypes[i])) {
                String arg = args.get(paramsIndex.get(i));
                if (arg == null) {
                    ReqParam anno = paramsAnno.get(i);
                    if (anno != null && !anno.required()) {
                        convertedArgs.add(Double.parseDouble(anno.defaultValue()));
                    } else {
                        ResponseUtil.missParam(response, paramsIndex.get(i));
                        return false;
                    }
                } else {
                    Double num = NumUtil.toDouble(arg);
                    convertedArgs.add(num);
                }
            } else if (Float.class.isAssignableFrom(parameterTypes[i]) || float.class.isAssignableFrom(parameterTypes[i])) {
                String arg = args.get(paramsIndex.get(i));
                if (arg == null) {
                    ReqParam anno = paramsAnno.get(i);
                    if (anno != null && !anno.required()) {
                        convertedArgs.add(Float.parseFloat(anno.defaultValue()));
                    } else {
                        ResponseUtil.missParam(response, paramsIndex.get(i));
                        return false;
                    }
                } else {
                    Double num = NumUtil.toDouble(arg);
                    convertedArgs.add(num.floatValue());
                }
            } else if (Boolean.class.isAssignableFrom(parameterTypes[i]) || boolean.class.isAssignableFrom(parameterTypes[i])) {
                String arg = args.get(paramsIndex.get(i));
                if (arg == null) {
                    ReqParam anno = paramsAnno.get(i);
                    if (anno != null && !anno.required()) {
                        convertedArgs.add(Boolean.parseBoolean(anno.defaultValue()));
                    } else {
                        ResponseUtil.missParam(response, paramsIndex.get(i));
                        return false;
                    }
                } else {
                    convertedArgs.add(Boolean.valueOf(arg));
                }
            } else if (String.class.isAssignableFrom(parameterTypes[i])) {
                String arg = args.get(paramsIndex.get(i));
                if (arg == null) {
                    ReqParam anno = paramsAnno.get(i);
                    if (anno != null && !anno.required()) {
                        convertedArgs.add(anno.defaultValue());
                    } else {
                        ReqBody bAnno = bodyAnno.get(i);
                        if (bAnno != null) {
                            if (bAnno.required() && Strings.isNullOrEmpty(data.getBody())) {
                                ResponseUtil.missParam(response, "RequestBody");
                                return false;
                            }
                            convertedArgs.add(data.getBody());
                        } else {
                            ResponseUtil.missParam(response, paramsIndex.get(i));
                            return false;
                        }
                    }
                } else {
                    convertedArgs.add(arg);
                }
            } else if (HttpRequest.class.isAssignableFrom(parameterTypes[i])) {
                convertedArgs.add(request);
            } else if (HttpResponse.class.isAssignableFrom(parameterTypes[i])) {
                convertedArgs.add(response);
            }
        }
        return true;
    }

}
