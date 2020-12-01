package org.scruz.rest;

import org.scruz.NettyClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private NettyClient nettyClient;

    @GetMapping("/nettyTest")
    public String nettyMessage(@RequestParam(value = "msg") String msg) {
        try {
            nettyClient.sendMessage(msg);
            return "Sending message ...";
        } catch (Exception e) {
            System.err.println("Error sending msg to netty server: " + e.getMessage());
            return "Error sending message! " + e.getMessage();
        }
    }

}
