package com.tradingbot.controller;

import com.tradingbot.service.KiteService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String REDIRECT_DASHBOARD = "redirect:/dashboard";
    private static final String REDIRECT_LOGIN_FAILED = "redirect:/?error=login_failed";

    private final KiteService kiteService;

    public AuthController(KiteService kiteService) {
        this.kiteService = kiteService;
    }

    @GetMapping("/")
    public String index() {
        if (kiteService.isAccessTokenValid()) {
            logger.info("Access token is valid. Redirecting to dashboard.");
            return REDIRECT_DASHBOARD;
        }
        logger.info("Access token is invalid. Redirecting to login.");
        return "login";
    }

    @GetMapping("/login")
    public RedirectView login() {
        String loginUrl = kiteService.getLoginUrl("");
        logger.info("Generated login URL: {}", loginUrl);
        return new RedirectView(loginUrl);
    }

    @GetMapping("/callback")
    public String callback(@RequestParam("request_token") String requestToken,
                           @RequestParam(value = "action", required = false) String action) {
        logger.info("Callback received with action: {}", action);
        if ("login".equals(action) && kiteService.generateAccessToken(requestToken)) {
            logger.info("Access token generated successfully. Redirecting to dashboard.");
            return REDIRECT_DASHBOARD;
        }
        logger.error("Access token generation failed. Redirecting to login with error.");
        return REDIRECT_LOGIN_FAILED;
    }
}
