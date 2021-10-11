package vip.aevlp.disruptor.spring.boot.event.handler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.aevlp.disruptor.spring.boot.event.DisruptorEventT;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChain;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChainResolver;
import vip.aevlp.disruptor.spring.boot.exception.EventHandleException;

import java.io.IOException;

/**
 * @author Steve
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class AbstractRouteableEventHandler<T extends DisruptorEventT> extends AbstractEnabledEventHandler<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRouteableEventHandler.class);

    /**
     * 用来判定使用那个HandlerChian
     */
    private HandlerChainResolver<T> handlerChainResolver;

    @Override
    protected void doHandlerInternal(T event, HandlerChain<T> handlerChain) throws Exception {
        Throwable t = null;
        try {
            this.executeChain(event, handlerChain);
        } catch (Throwable throwable) {
            t = throwable;
        }
        if (t != null) {
            if (t instanceof IOException) {
                throw (IOException) t;
            }
            String msg = "Handlered event failed.";
            throw new EventHandleException(msg, t);
        }
    }

    private HandlerChain<T> getExecutionChain(T event, HandlerChain<T> origChain) {
        HandlerChain<T> chain = origChain;

        HandlerChainResolver<T> resolver = getHandlerChainResolver();
        if (resolver == null) {
            LOG.debug("No HandlerChainResolver configured.  Returning original HandlerChain.");
            return origChain;
        }

        HandlerChain<T> resolved = resolver.getChain(event, origChain);
        if (resolved != null) {
            LOG.trace("Resolved a configured HandlerChain for the current event.");
            chain = resolved;
        } else {
            LOG.trace("No HandlerChain configured for the current event.  Using the default.");
        }

        return chain;
    }

    private void executeChain(T event, HandlerChain<T> origChain) throws Exception {
        HandlerChain<T> chain = getExecutionChain(event, origChain);
        chain.doHandler(event);
    }


}
