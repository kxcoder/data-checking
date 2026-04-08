package com.example.datachecking.infrastructure.cache;

import groovy.lang.Binding;
import groovy.lang.GroovyObject;

/**
 * 预编译 Groovy 脚本包装器
 * <p>
 * 职责: 封装 GroovyClassLoader.parseClass 编译后的 Class 对象，
 * 执行时通过反射创建实例并注入 Binding 变量。
 * <p>
 * 性能优化: Class 对象只编译一次，后续执行直接 newInstance，
 * 避免每次 GroovyShell.evaluate() 的解析开销。
 */
public class CompiledGroovyScript {

    /** 预编译后的 Class 对象 */
    private final Class<?> scriptClass;

    public CompiledGroovyScript(Class<?> scriptClass) {
        this.scriptClass = scriptClass;
    }

    /**
     * 执行预编译脚本
     *
     * @param binding Groovy 变量绑定
     * @return 脚本执行结果
     */
    public Object execute(Binding binding) {
        try {
            GroovyObject instance = (GroovyObject) scriptClass.getDeclaredConstructor().newInstance();
            binding.getVariables().forEach((key, value) -> instance.setProperty((String) key, value));
            return instance.invokeMethod("run", null);
        } catch (Exception e) {
            throw new RuntimeException("Groovy脚本执行失败", e);
        }
    }
}
