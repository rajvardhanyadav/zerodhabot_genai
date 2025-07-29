package com.tradingbot.controller;

import com.tradingbot.service.KiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class AuthController {

    @Autowired
    private KiteService kiteService;

    @GetMapping("/")
    public String index() {
        if (kiteService.isAccessTokenValid()) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/login")
    public RedirectView login() {
        String loginUrl = kiteService.getLoginUrl("");
        return new RedirectView(loginUrl);
    }

    @GetMapping("/callback")
    public String callback(@RequestParam("request_token") String requestToken,
                           @RequestParam(value = "action", required = false) String action) {
        if ("login".equals(action) && kiteService.generateAccessToken(requestToken)) {
            return "redirect:/dashboard";
        }
        return "redirect:/?error=login_failed";
    }
}
