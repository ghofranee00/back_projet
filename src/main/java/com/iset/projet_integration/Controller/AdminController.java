package com.iset.projet_integration.Controller;

import com.iset.projet_integration.Service.AdminReportService;
import com.iset.projet_integration.dto.SummaryReportDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin") // ⚠️ Cela donne /admin/summary-report
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminReportService adminReportService;

    // 🔹 Endpoint pour générer le rapport résumé détaillé
    @GetMapping("/summary-report")
    public SummaryReportDTO getSummaryReport() {
        System.out.println("✅ Endpoint /admin/summary-report appelé");
        return adminReportService.generateSummaryReport();
    }
}