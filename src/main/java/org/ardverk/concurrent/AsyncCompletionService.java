/*
 * Copyright 2010-2011 Roger Kapsi
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.ardverk.concurrent;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 */
public class AsyncCompletionService {

    private AsyncCompletionService() {}
    
    /**
     * 
     */
    public static <V, T extends AsyncFuture<V>> AsyncFuture<T[]> create(final T... futures) {
        return create(Arrays.asList(futures), new ValueFactory<T[]>() {
            @Override
            public T[] create() {
                return futures;
            }
        });
    }
    
    /**
     * 
     */
    public static <V, T extends Iterable<AsyncFuture<V>>> AsyncFuture<T> create(final T futures) {
        return create(futures, new ValueFactory<T>() {
            @Override
            public T create() {
                return futures;
            }
        });
    }
    
    private static <V, T> AsyncFuture<T> create(Iterable<? extends AsyncFuture<V>> futures, final ValueFactory<T> factory) {
        final Object lock = new Object();
        synchronized (lock) {
            final AsyncFuture<T> dst = new AsyncValueFuture<T>();
            
            final AtomicInteger counter = new AtomicInteger();
            AsyncFutureListener<V> listener = new AsyncFutureListener<V>() {
                @Override
                public void operationComplete(AsyncFuture<V> future) {
                    synchronized (lock) {
                        if (counter.decrementAndGet() == 0) {
                            try {
                                dst.setValue(factory.create());
                            } catch (Throwable err) {
                                dst.setException(err);
                            }
                        }
                    }
                }
            };
            
            for (AsyncFuture<V> future : futures) {
                future.addAsyncFutureListener(listener);
                counter.incrementAndGet();
            }
            
            if (counter.get() == 0) {
                try {
                    dst.setValue(factory.create());
                } catch (Throwable err) {
                    dst.setException(err);
                }
            }
            
            return dst;
        }
    }
    
    private static interface ValueFactory<T> {
        public T create();
    }
}
