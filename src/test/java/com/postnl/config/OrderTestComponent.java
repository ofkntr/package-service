package com.postnl.config;

import com.postnl.dao.OrderDao;
import com.postnl.handler.OrderHandlerTestBase;

import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {OrderModule.class})
public interface OrderTestComponent {
    OrderDao provideOrderDao();
    void inject(OrderHandlerTestBase integrationTest);
}
