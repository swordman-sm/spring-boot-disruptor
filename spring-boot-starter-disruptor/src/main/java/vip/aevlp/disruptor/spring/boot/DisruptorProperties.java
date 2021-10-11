package vip.aevlp.disruptor.spring.boot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import vip.aevlp.disruptor.spring.boot.config.RouterHandler;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(DisruptorProperties.PREFIX)
@Data
public class DisruptorProperties {

    public static final String PREFIX = "spring.disruptor";

    /**
     * 启用Disruptor开关
     */
    private boolean enabled = false;
    /**
     * 是否自动创建RingBuffer对象
     */
    private boolean ringBuffer = false;
    /**
     * RingBuffer缓冲区大小, 默认 1024
     */
    private int ringBufferSize = 1024;
    /**
     * 消息消费线程池大小, 默认 4
     */
    private int ringThreadNumbers = 4;
    /**
     * 是否对生产者，如果是则通过 RingBuffer.createMultiProducer创建一个多生产者的RingBuffer，
     * 否则通过RingBuffer.createSingleProducer创建一个单生产者的RingBuffer
     */
    private boolean multiProducer = false;
    /**
     * 声明Ant风格路径及对应处理的handler
     */
    private List<RouterHandler> handlerDefinitions = new ArrayList<RouterHandler>();

}