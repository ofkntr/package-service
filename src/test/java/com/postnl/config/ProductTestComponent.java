package com.postnl.config;

import com.postnl.dao.ProductDao;
import com.postnl.handler.ProductHandlerTestBase;

import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {ProductModule.class})
public interface ProductTestComponent {
    ProductDao provideProductDao();
    void inject(ProductHandlerTestBase integrationTest);
}
