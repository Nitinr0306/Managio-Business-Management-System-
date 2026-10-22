package com.managio.modules.reports.controller;

import com.managio.modules.reports.service.PortfolioService;
import com.managio.platform.security.Principals.UserPrincipal;
import com.managio.modules.reports.service.PortfolioService.Portfolio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/portfolio")
@RequiredArgsConstructor
class PortfolioController {

    private final PortfolioService portfolio;

    @GetMapping
    Portfolio get(@AuthenticationPrincipal UserPrincipal user) {
        return portfolio.portfolio(user.id());
    }
}
