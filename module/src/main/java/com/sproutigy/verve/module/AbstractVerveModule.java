package com.sproutigy.verve.module;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractVerveModule extends AbstractModule implements VerveModule {
    private Context context;

    @Override
    protected final void configure() {
        try {
            preInject(getContext(), binder());
        } catch (Throwable e) {
            log.error("Could not preInject module {}", this, e);
        }
    }

    public abstract void preInject(Context context, Binder binder) throws Exception;

    public abstract void postInject(Context context, Injector injector) throws Exception;

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void close() throws Exception {

    }

    public Injector getInjector() {
        Injector injector = getContext().get(Injector.class);
        if (injector == null) {
            throw new IllegalStateException("Injector not available");
        }
        return injector;
    }

    public <T> T getInstance(Class<T> clazz) {
        return getInjector().getInstance(clazz);
    }

    @Override
    public <T> T getInstance(Key<T> key) {
        return getInjector().getInstance(key);
    }
}
