package vip.aevlp.disruptor.spring.boot.event.handler.chain;


import vip.aevlp.disruptor.spring.boot.event.DisruptorEvent;

public interface HandlerChain<T extends DisruptorEvent>{

	void doHandler(T event) throws Exception;
	
}
