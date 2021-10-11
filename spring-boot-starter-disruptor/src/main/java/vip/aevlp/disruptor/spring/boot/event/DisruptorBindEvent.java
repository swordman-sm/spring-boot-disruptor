/*
 * Copyright (c) 2017, vindell (https://github.com/vindell).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package vip.aevlp.disruptor.spring.boot.event;

/**
 * @author Steve
 */
@SuppressWarnings("serial")
public class DisruptorBindEvent extends DisruptorEventT {

    /**
     * 当前事件绑定的数据对象
     */
    private Object bind;

    public DisruptorBindEvent() {
        super(null);
    }

    public DisruptorBindEvent(Object source) {
        super(source);
    }

    public DisruptorBindEvent(Object source, Object bind) {
        super(source);
        this.bind = bind;
    }

    public Object getBind() {
        return bind;
    }

    public void bind(Object bind) {
        this.bind = bind;
    }

}
