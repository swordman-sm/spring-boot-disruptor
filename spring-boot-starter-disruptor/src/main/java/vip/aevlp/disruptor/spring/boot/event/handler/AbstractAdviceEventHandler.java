package vip.aevlp.disruptor.spring.boot.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.aevlp.disruptor.spring.boot.event.DisruptorEvent;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChain;

public class AbstractAdviceEventHandler<T extends DisruptorEvent> extends AbstractEnabledEventHandler<T> {

	private final Logger LOG = LoggerFactory.getLogger(AbstractAdviceEventHandler.class);
	
	protected boolean preHandle(T event) throws Exception {
		return true;
	}

	private void postHandle(T event) throws Exception {
	}

	private void afterCompletion(T event, Exception exception) throws Exception {
	}

	private void executeChain(T event, HandlerChain<T> chain) throws Exception {
		chain.doHandler(event);
	}

	@Override
	public void doHandlerInternal(T event, HandlerChain<T> handlerChain) throws Exception {

		if (!isEnabled(event)) {
        	LOG.debug("Handler '{}' is not enabled for the current event.  Proceeding without invoking this handler.", getName());
        	// Proceed without invoking this handler...
            handlerChain.doHandler(event);
		} else {
			
			LOG.trace("Handler '{}' enabled.  Executing now.", getName());
			
			Exception exception = null;
			
			try {
	
				boolean continueChain = preHandle(event);
				if (LOG.isTraceEnabled()) {
					LOG.trace("Invoked preHandle method.  Continuing chain?: [" + continueChain + "]");
				}
				if (continueChain) {
					executeChain(event, handlerChain);
				}
				postHandle(event);
				if (LOG.isTraceEnabled()) {
					LOG.trace("Successfully invoked postHandle method");
				}
	
			} catch (Exception e) {
				exception = e;
			} finally {
				cleanup(event, exception);
			}
		}

	}

	private void cleanup(T event, Exception existing) throws Exception {
		Exception exception = existing;
		try {
			afterCompletion(event, exception);
			if (LOG.isTraceEnabled()) {
				LOG.trace("Successfully invoked afterCompletion method.");
			}
		} catch (Exception e) {
			if (exception == null) {
				exception = e;
			} else {
				LOG.debug("afterCompletion implementation threw an exception.  This will be ignored to "
						+ "allow the original source exception to be propagated.", e);
			}
		}
	}
	
	@Override
	protected boolean isEnabled(T event)
			throws Exception {
		return isEnabled();
	}
	
	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
