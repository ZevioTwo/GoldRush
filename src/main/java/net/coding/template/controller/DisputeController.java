package net.coding.template.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/dispute")
public class DisputeController {

    /**
     * 申请仲裁
     */
    @PostMapping("/apply")
    public void apply() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    }

    /**
     * 提交证据
     */
    @GetMapping("/submit")
    public void submit() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    }

    /**
     * 人工判责（后台）
     */
    @GetMapping("/judge")
    public void judge() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    }
}
