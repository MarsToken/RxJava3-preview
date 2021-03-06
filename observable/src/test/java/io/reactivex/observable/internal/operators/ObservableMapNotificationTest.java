/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.observable.internal.operators;



import java.util.concurrent.Callable;

import org.junit.Test;

import io.reactivex.common.Disposables;
import io.reactivex.common.exceptions.*;
import io.reactivex.common.functions.Function;
import io.reactivex.common.internal.functions.Functions;
import io.reactivex.observable.*;
import io.reactivex.observable.internal.operators.ObservableMapNotification.MapNotificationObserver;
import io.reactivex.observable.observers.TestObserver;

public class ObservableMapNotificationTest {
    @Test
    public void testJust() {
        TestObserver<Object> ts = new TestObserver<Object>();
        Observable.just(1)
        .flatMap(
                new Function<Integer, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(Integer item) {
                        return Observable.just((Object)(item + 1));
                    }
                },
                new Function<Throwable, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(Throwable e) {
                        return Observable.error(e);
                    }
                },
                new Callable<Observable<Object>>() {
                    @Override
                    public Observable<Object> call() {
                        return Observable.never();
                    }
                }
        ).subscribe(ts);

        ts.assertNoErrors();
        ts.assertNotComplete();
        ts.assertValue(2);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(new Observable<Integer>() {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                MapNotificationObserver mn = new MapNotificationObserver(
                        observer,
                        Functions.justFunction(Observable.just(1)),
                        Functions.justFunction(Observable.just(2)),
                        Functions.justCallable(Observable.just(3))
                );
                mn.onSubscribe(Disposables.empty());
            }
        });
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Observable<Object> o) throws Exception {
                return o.flatMap(
                        Functions.justFunction(Observable.just(1)),
                        Functions.justFunction(Observable.just(2)),
                        Functions.justCallable(Observable.just(3))
                );
            }
        });
    }

    @Test
    public void onErrorCrash() {
        TestObserver<Integer> ts = Observable.<Integer>error(new TestException("Outer"))
        .flatMap(Functions.justFunction(Observable.just(1)),
                new Function<Throwable, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(Throwable t) throws Exception {
                        throw new TestException("Inner");
                    }
                },
                Functions.justCallable(Observable.just(3)))
        .test()
        .assertFailure(CompositeException.class);

        TestHelper.assertError(ts, 0, TestException.class, "Outer");
        TestHelper.assertError(ts, 1, TestException.class, "Inner");
    }
}
