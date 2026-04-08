package com.example.datachecking.infrastructure.cache;

import com.example.datachecking.domain.model.CheckRule;
import com.example.datachecking.domain.model.MatchType;
import com.example.datachecking.domain.repository.CheckRuleRepository;
import com.example.datachecking.domain.service.RuleMatchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleIndexService implements RuleMatchService {

    private final CheckRuleRepository ruleRepository;

    private final Map<String, List<CheckRule>> exactMatch = new ConcurrentHashMap<>();
    private final Map<String, List<CheckRule>> wildcardMatch = new ConcurrentHashMap<>();
    private volatile List<RegexRuleEntry> regexMatch = new ArrayList<>();

    @PostConstruct
    public void init() {
        rebuildIndex();
        log.info("规则索引服务初始化完成: exact={}, wildcard={}, regex={}",
                exactMatch.size(), wildcardMatch.size(), regexMatch.size());
    }

    @Override
    public List<CheckRule> matchRules(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return Collections.emptyList();
        }

        List<CheckRule> matched = new ArrayList<>();

        List<CheckRule> exactRules = exactMatch.get(methodName);
        if (exactRules != null) {
            matched.addAll(exactRules);
        }

        for (Map.Entry<String, List<CheckRule>> entry : wildcardMatch.entrySet()) {
            if (matchWildcard(entry.getKey(), methodName)) {
                matched.addAll(entry.getValue());
            }
        }

        for (RegexRuleEntry entry : regexMatch) {
            if (entry.pattern().matcher(methodName).matches()) {
                matched.addAll(entry.rules());
            }
        }

        return matched.stream()
                .collect(Collectors.toMap(CheckRule::getId, r -> r, (a, b) -> a))
                .values()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .toList();
    }

    @Override
    public synchronized void rebuildIndex() {
        log.info("开始重建规则索引...");

        exactMatch.clear();
        wildcardMatch.clear();
        regexMatch = new ArrayList<>();

        List<CheckRule> allRules = ruleRepository.findAllEnabled();
        log.info("从数据库加载 {} 条启用规则", allRules.size());

        for (CheckRule rule : allRules) {
            if (rule.getMethodPattern() == null || rule.getMethodPattern().isBlank()) {
                log.warn("规则 methodPattern 为空，跳过: ruleId={}", rule.getId());
                continue;
            }

            switch (rule.getMatchType()) {
                case EXACT -> {
                    exactMatch.computeIfAbsent(rule.getMethodPattern(), k -> new ArrayList<>())
                            .add(rule);
                }
                case WILDCARD -> {
                    wildcardMatch.computeIfAbsent(rule.getMethodPattern(), k -> new ArrayList<>())
                            .add(rule);
                }
                case REGEX -> {
                    try {
                        Pattern pattern = Pattern.compile(rule.getMethodPattern());
                        regexMatch.add(new RegexRuleEntry(pattern, List.of(rule)));
                    } catch (Exception e) {
                        log.error("规则正则表达式编译失败: ruleId={}, pattern={}", rule.getId(), rule.getMethodPattern(), e);
                    }
                }
            }
        }

        log.info("规则索引重建完成: exact={}, wildcard={}, regex={}",
                exactMatch.size(), wildcardMatch.size(), regexMatch.size());
    }

    private boolean matchWildcard(String pattern, String methodName) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");
        return methodName.matches(regex);
    }

    private record RegexRuleEntry(Pattern pattern, List<CheckRule> rules) {
    }
}
