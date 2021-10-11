package vip.aevlp.disruptor.spring.boot.event.handler;

import com.lmax.disruptor.EventHandler;
import org.springframework.core.Ordered;
import vip.aevlp.disruptor.spring.boot.event.DisruptorEventT;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChain;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChainResolver;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.ProxiedHandlerChain;

/**
 * Disruptor 事件分发实现
 */
public class DisruptorEventDispatcher extends AbstractRouteableEventHandler<DisruptorEventT> implements EventHandler<DisruptorEventT>, Ordered {

    private int order = 0;

    public DisruptorEventDispatcher(HandlerChainResolver<DisruptorEventT> filterChainResolver, int order) {
        super(filterChainResolver);
        this.order = order;
    }

    /**
     * 责任链入口
     */
    @Override
    public void onEvent(DisruptorEventT event, long sequence, boolean endOfBatch) throws Exception {

        //构造原始链对象
        HandlerChain<DisruptorEventT> originalChain = new ProxiedHandlerChain();
        //执行事件处理链
        this.doHandler(event, originalChain);

    }

    @Override
    public int getOrder() {
        return order;
    }

}

