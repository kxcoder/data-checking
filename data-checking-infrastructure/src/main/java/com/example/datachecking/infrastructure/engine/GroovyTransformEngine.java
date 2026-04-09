package com.example.datachecking.infrastructure.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GroovyTransformEngine {

    public Object transform(String script, Object rootObject) {
        if (script == null || script.isEmpty()) {
            return rootObject;
        }
        
        log.warn("Groovy transform not fully implemented, returning raw value");
        return rootObject;
    }
}
