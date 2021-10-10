package vip.aevlp.disruptor.spring.boot.event.handler.chain;


import vip.aevlp.disruptor.spring.boot.event.DisruptorEvent;

public interface HandlerChainResolver<T extends DisruptorEvent> {

	HandlerChain<T> getChain(T event, HandlerChain<T> originalChain);
	
}
