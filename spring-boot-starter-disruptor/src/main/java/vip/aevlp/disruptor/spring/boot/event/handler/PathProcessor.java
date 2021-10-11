package vip.aevlp.disruptor.spring.boot.event.handler;


import vip.aevlp.disruptor.spring.boot.event.DisruptorEventT;

/**
 * 给Handler设置路径
 */
public interface PathProcessor<T extends DisruptorEventT> {

    DisruptorHandler<T> processPath(String path);

}
