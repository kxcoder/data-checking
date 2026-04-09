package com.example.datachecking.infrastructure.supplier;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.datachecking.domain.model.SupplierType;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RpcSupplierExecutor {

    private final ApplicationConfig applicationConfig;
    private final RegistryConfig registryConfig;
    private final Map<String, ReferenceConfig<GenericService>> referenceCache = new ConcurrentHashMap<>();

    public RpcSupplierExecutor(String applicationName, String registryAddress) {
        this.applicationConfig = new ApplicationConfig(applicationName);
        this.registryConfig = new RegistryConfig(registryAddress);
    }

    public Object execute(String configJson, Map<String, Object> params) {
        JSONObject config = JSON.parseObject(configJson);
        
        String interfaceName = config.getString("interface");
        String methodName = config.getString("method");
        String group = config.getString("group");
        String version = config.getString("version");
        JSONObject paramMapping = config.getJSONObject("paramMapping");
        
        try {
            ReferenceConfig<GenericService> ref = referenceCache.get(interfaceName + ":" + methodName);
            if (ref == null) {
                ref = new ReferenceConfig<>();
                ref.setApplication(applicationConfig);
                ref.setRegistry(registryConfig);
                ref.setInterface(interfaceName);
                ref.setGeneric(true);
                ref.setTimeout(3000);
                
                if (group != null) {
                    ref.setGroup(group);
                }
                if (version != null) {
                    ref.setVersion(version);
                }
                
                referenceCache.put(interfaceName + ":" + methodName, ref);
            }
            
            GenericService genericService = ref.get();
            Object[] args = buildRpcArgs(params, paramMapping);
            
            String[] paramTypes = args.length > 0 
                ? new String[]{args[0].getClass().getName()} 
                : new String[]{};
            
            return genericService.$invoke(methodName, paramTypes, args);
            
        } catch (Exception e) {
            log.error("RPC call failed: interface={}, method={}", interfaceName, methodName, e);
            return null;
        }
    }

    private Object[] buildRpcArgs(Map<String, Object> params, JSONObject paramMapping) {
        if (paramMapping == null || params == null) {
            return params != null ? new Object[]{params} : new Object[0];
        }
        
        Object[] args = new Object[paramMapping.size()];
        int index = 0;
        for (String key : paramMapping.keySet()) {
            String paramName = paramMapping.getString(key);
            args[index++] = params.get(paramName);
        }
        return args;
    }
    
    public SupplierType getSupplierType() {
        return SupplierType.RPC;
    }
}
