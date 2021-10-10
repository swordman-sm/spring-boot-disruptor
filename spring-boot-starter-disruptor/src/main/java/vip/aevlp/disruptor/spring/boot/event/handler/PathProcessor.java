package vip.aevlp.disruptor.spring.boot.event.handler;


import vip.aevlp.disruptor.spring.boot.event.DisruptorEvent;

/**
 * 给Handler设置路径
 */
public interface PathProcessor<T extends DisruptorEvent> {

    DisruptorHandler<T> processPath(String path);

}
