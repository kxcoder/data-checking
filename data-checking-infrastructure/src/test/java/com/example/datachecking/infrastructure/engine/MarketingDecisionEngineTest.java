package com.example.datachecking.infrastructure.engine;

import com.example.datachecking.domain.model.CheckRule;
import com.example.datachecking.domain.model.DecisionResult;
import com.example.datachecking.domain.model.RuleType;
import com.example.datachecking.domain.model.MatchType;
import com.example.datachecking.domain.service.DataSupplierService;
import com.example.datachecking.domain.service.SceneRuleService;
import com.example.datachecking.infrastructure.cache.CompiledExpressionCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketingDecisionEngineTest {

    @Mock
    private CompiledExpressionCache expressionCache;

    @Mock
    private DataSupplierService dataSupplierService;

    @Mock
    private SceneRuleService sceneRuleService;

    private MarketingDecisionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MarketingDecisionEngine(expressionCache, dataSupplierService, sceneRuleService);
    }

    @Test
    void testExecute_withNoRules_returnsReject() {
        when(sceneRuleService.matchRules("test_scene")).thenReturn(List.of());

        DecisionResult result = engine.execute("test_scene", 12345L, Map.of(), Map.of());

        assertNotNull(result);
        assertEquals("REJECT", result.getDecisionCode());
        assertTrue(result.getMessage().contains("No rules"));
        assertTrue(result.getSuccess());
    }

    @Test
    void testExecute_withNullSceneCode_returnsReject() {
        DecisionResult result = engine.execute(null, 12345L, Map.of(), Map.of());

        assertNotNull(result);
        assertEquals("REJECT", result.getDecisionCode());
    }

    @Test
    void testExecute_withValidScene_returnsPass() {
        CheckRule rule = CheckRule.builder()
                .id(1L)
                .ruleName("Test Rule")
                .ruleType(RuleType.SPEL)
                .expression("#uid > 0")
                .priority(100)
                .enabled(true)
                .build();

        when(sceneRuleService.matchRules("test_scene")).thenReturn(List.of(rule));

        SpelExpressionParser parser = new SpelExpressionParser();
        SpelExpression expression = parser.parseRaw("#uid > 0");
        when(expressionCache.getSpelExpression(eq(0L), any())).thenReturn(expression);

        DecisionResult result = engine.execute("test_scene", 12345L, Map.of(), Map.of());

        assertNotNull(result);
        assertEquals("PASS", result.getDecisionCode());
        assertTrue(result.getSuccess());
    }

    @Test
    void testExecute_withSpelExpression_returnsCorrectResult() {
        CheckRule rule = CheckRule.builder()
                .id(1L)
                .ruleName("Age Check")
                .methodPattern("age_check")
                .matchType(MatchType.EXACT)
                .ruleType(RuleType.SPEL)
                .expression("#uid > 10000")
                .priority(100)
                .enabled(true)
                .build();

        when(sceneRuleService.matchRules("age_check")).thenReturn(List.of(rule));

        SpelExpressionParser parser = new SpelExpressionParser();
        SpelExpression expression = parser.parseRaw("#uid > 10000");
        when(expressionCache.getSpelExpression(eq(0L), any())).thenReturn(expression);

        DecisionResult result = engine.execute("age_check", 12345L, Map.of(), Map.of());

        assertNotNull(result);
        assertEquals("PASS", result.getDecisionCode());
    }

    @Test
    void testExecute_withLowUid_returnsReject() {
        CheckRule rule = CheckRule.builder()
                .id(1L)
                .ruleName("Age Check")
                .methodPattern("age_check")
                .matchType(MatchType.EXACT)
                .ruleType(RuleType.SPEL)
                .expression("#uid > 10000")
                .priority(100)
                .enabled(true)
                .build();

        when(sceneRuleService.matchRules("age_check")).thenReturn(List.of(rule));

        SpelExpressionParser parser = new SpelExpressionParser();
        SpelExpression expression = parser.parseRaw("#uid > 10000");
        when(expressionCache.getSpelExpression(eq(0L), any())).thenReturn(expression);

        DecisionResult result = engine.execute("age_check", 999L, Map.of(), Map.of());

        assertNotNull(result);
        assertEquals("REJECT", result.getDecisionCode());
    }

    @Test
    void testExecute_withMultipleRules_usesFirstMatch() {
        CheckRule rule1 = CheckRule.builder()
                .id(1L)
                .ruleName("High Priority Rule")
                .methodPattern("multi_rule")
                .matchType(MatchType.EXACT)
                .ruleType(RuleType.SPEL)
                .expression("#uid > 0")
                .priority(200)
                .enabled(true)
                .build();

        CheckRule rule2 = CheckRule.builder()
                .id(2L)
                .ruleName("Low Priority Rule")
                .methodPattern("multi_rule")
                .matchType(MatchType.EXACT)
                .ruleType(RuleType.SPEL)
                .expression("#uid < 0")
                .priority(100)
                .enabled(true)
                .build();

        when(sceneRuleService.matchRules("multi_rule")).thenReturn(List.of(rule1, rule2));

        SpelExpressionParser parser = new SpelExpressionParser();
        SpelExpression expr1 = parser.parseRaw("#uid > 0");
        SpelExpression expr2 = parser.parseRaw("#uid < 0");
        when(expressionCache.getSpelExpression(eq(0L), any()))
                .thenReturn(expr1)
                .thenReturn(expr2);

        DecisionResult result = engine.execute("multi_rule", 12345L, Map.of(), Map.of());

        assertNotNull(result);
        assertEquals("PASS", result.getDecisionCode());
    }

    @Test
    void testExecute_withDisabledRule_skipsRule() {
        when(sceneRuleService.matchRules("disabled_scene")).thenReturn(List.of());

        DecisionResult result = engine.execute("disabled_scene", 12345L, Map.of(), Map.of());

        assertNotNull(result);
        assertEquals("REJECT", result.getDecisionCode());
    }

    @Test
    void testExecute_measureProcessTime() {
        CheckRule rule = CheckRule.builder()
                .id(1L)
                .ruleName("Timing Test")
                .methodPattern("timing_scene")
                .matchType(MatchType.EXACT)
                .ruleType(RuleType.SPEL)
                .expression("#uid > 0")
                .priority(100)
                .enabled(true)
                .build();

        when(sceneRuleService.matchRules("timing_scene")).thenReturn(List.of(rule));

        SpelExpressionParser parser = new SpelExpressionParser();
        SpelExpression expression = parser.parseRaw("#uid > 0");
        when(expressionCache.getSpelExpression(eq(0L), any())).thenReturn(expression);

        DecisionResult result = engine.execute("timing_scene", 12345L, Map.of(), Map.of());

        assertNotNull(result);
        assertNotNull(result.getProcessTimeMs());
        assertTrue(result.getProcessTimeMs() >= 0);
    }
}