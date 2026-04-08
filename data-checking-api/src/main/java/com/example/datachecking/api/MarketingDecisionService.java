package com.example.datachecking.api;

import com.example.datachecking.api.dto.MarketingDecisionRequest;
import com.example.datachecking.api.dto.MarketingDecisionResponse;

public interface MarketingDecisionService {

    MarketingDecisionResponse decide(MarketingDecisionRequest request);
}