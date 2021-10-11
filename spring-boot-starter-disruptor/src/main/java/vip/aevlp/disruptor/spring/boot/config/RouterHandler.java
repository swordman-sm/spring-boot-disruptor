package vip.aevlp.disruptor.spring.boot.config;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Steve
 */
@Data
public class RouterHandler {

    /**
     * 当前处理器所在位置
     */
    private int order = 0;

    /**
     * 处理器链定义
     */
    private String routers = null;

    /**
     * key:Ant风格路径 value:对应处理的handler
     */
    private Map<String, String> definitionMap = new LinkedHashMap<>();

}
