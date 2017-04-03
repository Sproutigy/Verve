package com.sproutigy.verve.demo;

import com.google.auto.service.AutoService;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.sproutigy.verve.module.AbstractVerveModule;
import com.sproutigy.verve.module.Context;
import com.sproutigy.verve.module.VerveModule;
import com.sproutigy.verve.resources.ClassPathResource;
import com.sproutigy.verve.webserver.HttpRouter;
import com.sproutigy.verve.webserver.handlers.JSONRPCHttpHandler;
import com.sproutigy.verve.webserver.handlers.ServeResourceHttpHandler;

@AutoService(VerveModule.class)
public class DemoModule extends AbstractVerveModule {

    @Override
    public void preInject(Context context, Binder binder) throws Exception {
    }

    @Override
    public void postInject(Context context, Injector injector) throws Exception {
        HttpRouter router = context.get(HttpRouter.class);
        router.add(injector.getInstance(Hello.class));
        router.add(injector.getInstance(Randomizer.class));
        router.add("/rpc/test", new JSONRPCHttpHandler(new TestRPC()));
        router.add(new ServeResourceHttpHandler(new ClassPathResource("web/", this.getClass())));
    }
}
