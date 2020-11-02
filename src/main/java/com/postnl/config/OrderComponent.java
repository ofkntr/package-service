package com.postnl.config;

import com.postnl.handler.CreateOrderHandler;
import com.postnl.handler.DeleteOrderHandler;
import com.postnl.handler.GetOrderHandler;
import com.postnl.handler.GetOrdersHandler;
import com.postnl.handler.UpdateOrderHandler;

import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {OrderModule.class})
public interface OrderComponent {

    void inject(CreateOrderHandler requestHandler);

    void inject(DeleteOrderHandler requestHandler);

    void inject(GetOrderHandler requestHandler);

    void inject(GetOrdersHandler requestHandler);

    void inject(UpdateOrderHandler requestHandler);
}
