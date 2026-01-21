package net.coding.template.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/payment")
public class PaymentController {

    /**
     * 生成支付预订单
     */
    @PostMapping("/prepay")
    public void prepay() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    }

    /**
     * 微信支付回调
     */
    @PostMapping("/notify")
    public void notify1() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    }

    /**
     * 资金冻结
     */
    @PostMapping("/freeze")
    public void freeze() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    }

    /**
     * 资金解冻
     */
    @PostMapping("/unfreeze")
    public void unfreeze() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    }

    /**
     * 扣除违约金
     */
    @PostMapping("/deduct")
    public void deduct() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    }
}
