package vip.aevlp.disruptor.spring.boot.event.handler;


import vip.aevlp.disruptor.spring.boot.event.DisruptorEvent;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChain;

public interface DisruptorHandler<T extends DisruptorEvent> {

    void doHandler(T event, HandlerChain<T> handlerChain) throws Exception;

}
