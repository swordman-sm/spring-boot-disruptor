package vip.aevlp.disruptor.spring.boot.event.handler.chain.def;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import vip.aevlp.disruptor.spring.boot.event.DisruptorEventT;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChain;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChainManager;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChainResolver;

public class PathMatchingHandlerChainResolver implements HandlerChainResolver<DisruptorEventT> {

    private static final Logger log = LoggerFactory.getLogger(PathMatchingHandlerChainResolver.class);
    /**
     * handlerChain管理器
     */
    private HandlerChainManager<DisruptorEventT> handlerChainManager;

    /**
     * 路径匹配器
     */
    private PathMatcher pathMatcher;

    public PathMatchingHandlerChainResolver() {
        this.pathMatcher = new AntPathMatcher();
        this.handlerChainManager = new DefaultHandlerChainManager();
    }

    private HandlerChainManager<DisruptorEventT> getHandlerChainManager() {
        return handlerChainManager;
    }

    public void setHandlerChainManager(HandlerChainManager<DisruptorEventT> handlerChainManager) {
        this.handlerChainManager = handlerChainManager;
    }

    private PathMatcher getPathMatcher() {
        return pathMatcher;
    }

    public void setPathMatcher(PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
    }


    @Override
    public HandlerChain<DisruptorEventT> getChain(DisruptorEventT event, HandlerChain<DisruptorEventT> originalChain) {
        HandlerChainManager<DisruptorEventT> handlerChainManager = getHandlerChainManager();
        if (!handlerChainManager.hasChains()) {
            return null;
        }
        String eventURI = getPathWithinEvent(event);
        for (String pathPattern : handlerChainManager.getChainNames()) {
            if (pathMatches(pathPattern, eventURI)) {
                if (log.isTraceEnabled()) {
                    log.trace("Matched path pattern [" + pathPattern + "] for eventURI [" + eventURI + "].  " +
                            "Utilizing corresponding handler chain...");
                }
                return handlerChainManager.proxy(originalChain, pathPattern);
            }
        }
        return null;
    }

    private boolean pathMatches(String pattern, String path) {
        PathMatcher pathMatcher = getPathMatcher();
        return pathMatcher.match(pattern, path);
    }

    private String getPathWithinEvent(DisruptorEventT event) {
        return event.getRouteExpression();
    }

}
