package com.example.datachecking.domain.service;

public interface TransformEngine {

    Object transform(String script, Object input);
}
