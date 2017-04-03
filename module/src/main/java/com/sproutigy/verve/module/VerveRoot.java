package com.sproutigy.verve.module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class VerveRoot extends AbstractModule implements Iterable<VerveModule>, Runnable, AutoCloseable {
    @Getter
    private Context rootContext = new ContextImpl();

    @Getter
    private Injector rootInjector;

    private Injector lastInjector;

    private List<VerveModule> modules = new CopyOnWriteArrayList<>();

    private List<VerveRootListener> listeners = new CopyOnWriteArrayList<>();


    public void addListener(VerveRootListener listener) {
        listeners.add(listener);
    }

    public void removeListener(VerveRootListener listener) {
        listeners.remove(listener);
    }

    @Override
    protected void configure() {
    }

    public void autoDetectModules() {
        autoDetectModules(null);
    }

    public void autoDetectModules(ClassLoader classLoader) {
        ServiceLoader<VerveModule> serviceLoader = ServiceLoader.load(VerveModule.class, classLoader);
        addModules(serviceLoader.iterator());
    }

    public void addModule(VerveModule module) {
        addModules(Collections.singleton(module).iterator());
    }

    public synchronized void addModules(Iterator<VerveModule> moduleIterator) {
        if (rootInjector == null) {
            run();
        }

        Collection<VerveModule> newModules = new LinkedList<>();

        //TODO: reorder according to module dependencies

        while (moduleIterator.hasNext()) {
            VerveModule module = moduleIterator.next();
            newModules.add(module);

            log.info("Verve module: {}", module.getName());

            ContextImpl moduleContext = new ContextImpl(getRootContext());
            module.setContext(moduleContext);

            raiseEvent(VerveRootListener.EventType.Initializing, module);
        }

        Injector injector = lastInjector.createChildInjector(newModules);
        for (VerveModule module : newModules) {
            module.getContext().set(Injector.class, injector);
            raiseEvent(VerveRootListener.EventType.Injected, module);

            try {
                module.postInject(module.getContext(), injector);
            } catch (Exception e) {
                throw new RuntimeException("Module {} thrown an exception on postInject() phase", e);
            }
            modules.add(module);

            raiseEvent(VerveRootListener.EventType.Initialized, module);
        }

        lastInjector = injector;
    }

    @Override
    public Iterator<VerveModule> iterator() {
        return Collections.unmodifiableCollection(modules).iterator();
    }

    @Override
    public synchronized void run() {
        rootInjector = Guice.createInjector(new VerveRoot());
        lastInjector = rootInjector;

        getRootContext().set(VerveRoot.class, this);
    }

    @Override
    public synchronized void close() throws Exception {
        List<VerveModule> modulesToDestroy = new ArrayList<>(modules);
        Collections.reverse(modulesToDestroy);
        for (VerveModule module : modulesToDestroy) {
            raiseEvent(VerveRootListener.EventType.Destroying, module);
            try {
                module.close();
            } catch (Throwable e) {
                log.error("Exception thrown while destroying module {}", module.getName(), e);
            }
            raiseEvent(VerveRootListener.EventType.Destroyed, module);
        }
    }

    private void raiseEvent(VerveRootListener.EventType eventType, VerveModule module) {
        for (VerveRootListener listener : listeners) {
            listener.onModuleEvent(eventType, module);
        }
    }
}
