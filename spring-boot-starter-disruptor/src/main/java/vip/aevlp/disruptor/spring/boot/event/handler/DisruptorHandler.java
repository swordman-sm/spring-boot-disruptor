package vip.aevlp.disruptor.spring.boot.event.handler;


import vip.aevlp.disruptor.spring.boot.event.DisruptorEventT;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChain;

public interface DisruptorHandler<T extends DisruptorEventT> {

    void doHandler(T event, HandlerChain<T> handlerChain) throws Exception;

}
