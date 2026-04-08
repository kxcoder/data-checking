package com.example.datachecking.infrastructure.engine;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.springframework.stereotype.Component;

import javax.script.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GroovyTransformEngine {

    private final GroovyScriptEngineFactory engineFactory = new GroovyScriptEngineFactory();
    private final Map<String, CompiledScript> scriptCache = new ConcurrentHashMap<>();
    private final ThreadLocal<ScriptEngine> engineThreadLocal = ThreadLocal.withInitial(() -> engineFactory.getScriptEngine());

    public Object transform(String script, Object rootObject) {
        if (script == null || script.isEmpty()) {
            return rootObject;
        }
        
        try {
            CompiledScript compiled = scriptCache.get(script);
            if (compiled == null) {
                ScriptEngine engine = engineThreadLocal.get();
                compiled = ((Compilable) engine).compile(script);
                scriptCache.put(script, compiled);
            }
            
            Bindings bindings = compiled.getEngine().createBindings();
            bindings.put("root", rootObject);
            
            if (rootObject instanceof Map) {
                Map<?, ?> rootMap = (Map<?, ?>) rootObject;
                for (Map.Entry<?, ?> entry : rootMap.entrySet()) {
                    bindings.put(entry.getKey().toString(), entry.getValue());
                }
            }
            
            return compiled.eval(bindings);
            
        } catch (Exception e) {
            log.error("Groovy transform failed: script={}", script, e);
            return null;
        }
    }
}
