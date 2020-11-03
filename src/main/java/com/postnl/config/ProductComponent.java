package com.postnl.config;

import com.postnl.handler.CreateProductHandler;
import com.postnl.handler.GetProductHandler;
import com.postnl.handler.GetPackagesHandler;

import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {ProductModule.class})
public interface ProductComponent {

    void inject(CreateProductHandler requestHandler);

    void inject(GetProductHandler requestHandler);

    void inject(GetPackagesHandler requestHandler);

}
