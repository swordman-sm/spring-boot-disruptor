package vip.aevlp.disruptor.spring.boot.event.handler.chain;


import vip.aevlp.disruptor.spring.boot.event.DisruptorEvent;

/**
 * @author Steve
 */
public interface HandlerChain<T extends DisruptorEvent> {

    /**
     * @param event
     * @throws Exception
     */
    void doHandler(T event) throws Exception;

}
