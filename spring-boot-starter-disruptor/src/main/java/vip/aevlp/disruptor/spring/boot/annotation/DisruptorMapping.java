package vip.aevlp.disruptor.spring.boot.annotation;

import java.lang.annotation.*;

/**
 * @author Steve
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented	
@Inherited		
public @interface DisruptorMapping {
	
	/**
	 * Ant风格规则表达式
	 * 格式为：/event/tags/keys，如：/Event-DC-Output/TagA-Output/**
	 * @return 规则表达式
	 */
	String router() default "*";
	
}
