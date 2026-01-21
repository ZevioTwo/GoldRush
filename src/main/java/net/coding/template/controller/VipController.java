package net.coding.template.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/vip")
public class VipController {

    /**
     * 开通会员
     */
    @PostMapping("/subscribe")
    public void subscribe() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    }

    /**
     * 会员权益
     */
    @GetMapping("/benefits")
    public void benefits() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    }

}
