package com.lchml.webcat.util;

import com.lchml.webcat.annotation.ReqBody;
import com.lchml.webcat.annotation.ReqParam;
import javassist.*;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 *  Class Method方法类
 *      使用javaassist获取方法的真实名称
 */
public class MethodUtil {
    public static Map<Integer, String> getParameterMap(Method method, Map<Integer, String> paramIndexMap,
        Map<Integer, ReqParam> paramAnnoMap, Map<Integer, ReqBody> bodyAnno) throws NotFoundException {
        // 查询method对应的class字节码并加载
        ClassPool pool = ClassPool.getDefault();
        Class<?> clazz = method.getDeclaringClass();
        CtClass clz = pool.getCtClass(clazz.getName());

        // 将方法参数对应的字节码全部加载
        CtClass[] params = new CtClass[method.getParameterTypes().length];
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            params[i] = pool.getCtClass(method.getParameterTypes()[i].getName());
        }

        // 使用javaassist的反射方法获取方法的参数名
        CtMethod cm = clz.getDeclaredMethod(method.getName(), params);
        MethodInfo methodInfo = cm.getMethodInfo();
        CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
        LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag); // 获取方法的参数列表，对于非static方法，第0个是指向this对象
        int pos = 0;

        // 结束循环，当以下一个条件发生： a. method是静态方法; b. 找到attr里的属性名称为this
        while (!Modifier.isStatic(cm.getModifiers()) && !"this".equals(attr.variableName(pos++))) { // 这里的第0个就是this
        }
        System.out.println("======= " + pos);
        Annotation[][] annos = method.getParameterAnnotations();
        for (int i = 0; i < cm.getParameterTypes().length; i++) { // 循环方法的参数
            paramIndexMap.put(i, attr.variableName(i + pos)); // 设置方法的名称
            Annotation[] annosParam = annos[i]; // 注解
            for (Annotation anno : annosParam) { // 循环处理注解
                if (ReqParam.class.isInstance(anno)) {
                    ReqParam req = (ReqParam) anno;
                    paramAnnoMap.put(i, req);
                    if (!StringUtils.isEmpty(req.name())) {
                        paramIndexMap.put(i, req.name());
                    }
                } else if (ReqBody.class.isInstance(anno)) {
                    if (bodyAnno != null) {
                        bodyAnno.put(i, (ReqBody) anno);
                    }
                }
            }
        }
        return paramIndexMap;
    }
}
