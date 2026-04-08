package com.example.datachecking.infrastructure.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import groovy.lang.GroovyClassLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CompiledExpressionCache {

    private final Cache<Long, Expression> spelCache;
    private final Cache<Long, CompiledGroovyScript> groovyCache;
    private final SpelExpressionParser spelParser;
    private final GroovyClassLoader groovyClassLoader;

    public CompiledExpressionCache(
            @Value("${data-check.cache.max-size:1000}") int maxSize,
            @Value("${data-check.cache.expire-minutes:30}") int expireMinutes) {
        
        SpelParserConfiguration config = new SpelParserConfiguration(
                SpelCompilerMode.MIXED, getClass().getClassLoader());
        this.spelParser = new SpelExpressionParser(config);
        
        this.groovyClassLoader = new GroovyClassLoader(getClass().getClassLoader());

        this.spelCache = CacheBuilder.newBuilder()
                .maximumSize(maxSize)
                .expireAfterAccess(expireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();

        this.groovyCache = CacheBuilder.newBuilder()
                .maximumSize(maxSize)
                .expireAfterAccess(expireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    public Expression getSpelExpression(Long ruleId, String expression) {
        try {
            return spelCache.get(ruleId, () -> spelParser.parseRaw(expression));
        } catch (Exception e) {
            throw new RuntimeException("SpEL表达式编译失败: ruleId=" + ruleId, e);
        }
    }

    public CompiledGroovyScript getGroovyScript(Long ruleId, String script) {
        try {
            return groovyCache.get(ruleId, () -> {
                Class<?> scriptClass = groovyClassLoader.parseClass(script);
                return new CompiledGroovyScript(scriptClass);
            });
        } catch (Exception e) {
            throw new RuntimeException("Groovy脚本编译失败: ruleId=" + ruleId, e);
        }
    }

    public void invalidate(Long ruleId) {
        spelCache.invalidate(ruleId);
        groovyCache.invalidate(ruleId);
    }

    public void invalidateAll() {
        spelCache.invalidateAll();
        groovyCache.invalidateAll();
    }

    public CacheStats getStats() {
        return new CacheStats(spelCache.stats(), groovyCache.stats());
    }

    public record CacheStats(com.google.common.cache.CacheStats spelStats, 
                             com.google.common.cache.CacheStats groovyStats) {}
}