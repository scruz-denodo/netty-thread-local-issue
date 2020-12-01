package org.scruz.rest;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.scruz.NettyClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NettyConfig {

    @Value("${netty.server.host}")
    private String nettyServerHost;
    @Value("${netty.server.port}")
    private int nettyServerPort;

    @Autowired
    private NettyClient nettyClient;

    @Bean
    public NettyClient nettyClient() {
        return new NettyClient(nettyServerHost, nettyServerPort);
    }

    @Bean
    ServletListenerRegistrationBean<ServletContextListener> servletContextListener() {
        final ServletListenerRegistrationBean<ServletContextListener> srvListenerRegBean = new ServletListenerRegistrationBean<>();
        srvListenerRegBean.setListener(new ContextListener());
        return srvListenerRegBean;
    }

    class ContextListener implements ServletContextListener {
        @Override
        public void contextDestroyed(ServletContextEvent event) {
            try {
                System.out.println("Disconnecting netty client...");
                nettyClient.disconnect();
            } catch (InterruptedException e) {
                System.err.println("Error disconnecting netty client: " + e.getMessage());
            }
        }

        @Override
        public void contextInitialized(ServletContextEvent event) {
            // Nothing
        }
    }

}
