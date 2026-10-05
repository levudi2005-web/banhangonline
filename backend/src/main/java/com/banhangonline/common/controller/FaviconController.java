package com.banhangonline.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FaviconController {
    @GetMapping("/favicon.ico")
    public String redirectToSvgFavicon() {
        return "redirect:/favicon.svg";
    }
}
