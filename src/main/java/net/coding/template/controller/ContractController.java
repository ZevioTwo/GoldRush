package net.coding.template.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/contract")
public class ContractController {

    /**
     * 创建契约
     */
    @PostMapping("/create")
    public void create() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    }

    /**
     * 契约列表
     */
    @GetMapping("/list")
    public void list() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    }

    /**
     * 契约详情
     */
    @GetMapping("/detail/{id}")
    public void detail() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    }

    /**
     * 确认完成
     */
    @PostMapping("/confirm")
    public void confirm() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    }
}
