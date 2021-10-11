package vip.aevlp.disruptor.spring.boot.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.aevlp.disruptor.spring.boot.event.DisruptorEventT;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChain;

public abstract class AbstractEnabledEventHandler<T extends DisruptorEventT> extends AbstractNameableEventHandler<T> {

	private final Logger LOG = LoggerFactory.getLogger(AbstractEnabledEventHandler.class);
	boolean enabled = true;

	protected abstract void doHandlerInternal(T event, HandlerChain<T> handlerChain) throws Exception;

	@Override
	public void doHandler(T event, HandlerChain<T> handlerChain) throws Exception {

		if (!isEnabled(event)) {
			LOG.debug("Handler '{}' is not enabled for the current event.  Proceeding without invoking this handler.",
					getName());
			// Proceed without invoking this handler...
			handlerChain.doHandler(event);
		} else {
			LOG.trace("Handler '{}' enabled.  Executing now.", getName());
			doHandlerInternal(event, handlerChain);
		}

	}
	
	protected boolean isEnabled(T event) throws Exception {
		return isEnabled();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	

}
