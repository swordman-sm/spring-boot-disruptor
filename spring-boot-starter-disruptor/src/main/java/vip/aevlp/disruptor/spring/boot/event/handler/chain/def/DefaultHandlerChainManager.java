package vip.aevlp.disruptor.spring.boot.event.handler.chain.def;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import vip.aevlp.disruptor.spring.boot.event.DisruptorEventT;
import vip.aevlp.disruptor.spring.boot.event.handler.DisruptorHandler;
import vip.aevlp.disruptor.spring.boot.event.handler.Nameable;
import vip.aevlp.disruptor.spring.boot.event.handler.NamedHandlerList;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChain;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChainManager;
import vip.aevlp.disruptor.spring.boot.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DefaultHandlerChainManager implements HandlerChainManager<DisruptorEventT> {

    private static transient final Logger log = LoggerFactory.getLogger(DefaultHandlerChainManager.class);

    private Map<String, DisruptorHandler<DisruptorEventT>> handlers;

    private Map<String, NamedHandlerList<DisruptorEventT>> handlerChains;

    private final static String DEFAULT_CHAIN_DEFINATION_DELIMITER_CHAR = ",";

    public DefaultHandlerChainManager() {
        this.handlers = new LinkedHashMap<>();
        this.handlerChains = new LinkedHashMap<>();
    }

    @Override
    public Map<String, DisruptorHandler<DisruptorEventT>> getHandlers() {
        return handlers;
    }

    public void setHandlers(Map<String, DisruptorHandler<DisruptorEventT>> handlers) {
        this.handlers = handlers;
    }

    public Map<String, NamedHandlerList<DisruptorEventT>> getHandlerChains() {
        return handlerChains;
    }

    public void setHandlerChains(Map<String, NamedHandlerList<DisruptorEventT>> handlerChains) {
        this.handlerChains = handlerChains;
    }

    public DisruptorHandler<DisruptorEventT> getHandler(String name) {
        return this.handlers.get(name);
    }

    @Override
    public void addHandler(String name, DisruptorHandler<DisruptorEventT> handler) {
        addHandler(name, handler, true);
    }

    protected void addHandler(String name, DisruptorHandler<DisruptorEventT> handler, boolean overwrite) {
        DisruptorHandler<DisruptorEventT> existing = getHandler(name);
        if (existing == null || overwrite) {
            if (handler instanceof Nameable) {
                ((Nameable) handler).setName(name);
            }
            this.handlers.put(name, handler);
        }
    }

    @Override
    public void createChain(String chainName, String chainDefinition) {
        if (StringUtils.isBlank(chainName)) {
            throw new NullPointerException("chainName cannot be null or empty.");
        }
        if (StringUtils.isBlank(chainDefinition)) {
            throw new NullPointerException("chainDefinition cannot be null or empty.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Creating chain [" + chainName + "] from String definition [" + chainDefinition + "]");
        }
        String[] handlerTokens = splitChainDefinition(chainDefinition);
        for (String token : handlerTokens) {
            addToChain(chainName, token);
        }
    }

    /**
     * Splits the comma-delimited handler chain definition line into individual handler definition tokens.
     *
     * @param chainDefinition chain definition line
     * @return array of chain definition
     */
    protected String[] splitChainDefinition(String chainDefinition) {
        String trimToNull = StringUtils.trimToNull(chainDefinition);
        if (trimToNull == null) {
            return null;
        }
        String[] split = StringUtils.splits(trimToNull, DEFAULT_CHAIN_DEFINATION_DELIMITER_CHAR);
        for (int i = 0; i < split.length; i++) {
            split[i] = StringUtils.trimToNull(split[i]);
        }
        return split;
    }

    public static void main(String[] args) {

    }

    @Override
    public void addToChain(String chainName, String handlerName) {
        if (StringUtils.isBlank(chainName)) {
            throw new IllegalArgumentException("chainName cannot be null or empty.");
        }
        DisruptorHandler<DisruptorEventT> handler = getHandler(handlerName);
        if (handler == null) {
            throw new IllegalArgumentException("There is no handler with name '" + handlerName +
                    "' to apply to chain [" + chainName + "] in the pool of available Handlers.  Ensure a " +
                    "handler with that name/path has first been registered with the addHandler method(s).");
        }
        NamedHandlerList<DisruptorEventT> chain = ensureChain(chainName);
        chain.add(handler);
    }

    private NamedHandlerList<DisruptorEventT> ensureChain(String chainName) {
        NamedHandlerList<DisruptorEventT> chain = getChain(chainName);
        if (chain == null) {
            chain = new DefaultNamedHandlerList(chainName);
            this.handlerChains.put(chainName, chain);
        }
        return chain;
    }

    @Override
    public NamedHandlerList<DisruptorEventT> getChain(String chainName) {
        return this.handlerChains.get(chainName);
    }

    @Override
    public boolean hasChains() {
        return !CollectionUtils.isEmpty(this.handlerChains);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getChainNames() {
        return this.handlerChains != null ? this.handlerChains.keySet() : Collections.EMPTY_SET;
    }

    @Override
    public HandlerChain<DisruptorEventT> proxy(HandlerChain<DisruptorEventT> original, String chainName) {
        NamedHandlerList<DisruptorEventT> configured = getChain(chainName);
        if (configured == null) {
            String msg = "There is no configured chain under the name/key [" + chainName + "].";
            throw new IllegalArgumentException(msg);
        }
        return configured.proxy(original);
    }


}
