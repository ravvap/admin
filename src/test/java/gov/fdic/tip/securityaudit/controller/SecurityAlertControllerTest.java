package gov.fdic.tip.securityaudit.controller;

import gov.fdic.tip.securityaudit.dto.SecurityAlertDto;
import gov.fdic.tip.securityaudit.entity.SecurityAlert;
import gov.fdic.tip.securityaudit.service.SecurityAlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SecurityAlertController.class)
class SecurityAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityAlertService alertService;

    @Test
    void testGetAlertsEndpoint() throws Exception {
        SecurityAlert alert = SecurityAlert.builder()
                .id(1L)
                .severity(SecurityAlert.Severity.CRITICAL)
                .conditionName("Privilege escalation")
                .triggerType("CHECKED INSTANTLY")
                .accountSource("BOYD, Marcus")
                .firstDetected(LocalDateTime.now())
                .lastSeen(LocalDateTime.now())
                .status(SecurityAlert.AlertStatus.NEW)
                .build();

        Page<SecurityAlertDto> alertPage = new PageImpl<>(List.of(SecurityAlertDto.from(alert)));
        when(alertService.getAlerts(any(), any(), any(), any(), any(), any(), any())).thenReturn(alertPage);

        mockMvc.perform(get("/api/v1/security-audit/alerts")
                .param("status", "NEW")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].conditionName").value("Privilege escalation"));
    }

    @Test
    void testGetAlertDetailEndpoint() throws Exception {
        SecurityAlertDto dto = SecurityAlertDto.builder()
                .id(5L)
                .conditionName("Password spray")
                .severity(SecurityAlert.Severity.CRITICAL)
                .status(SecurityAlert.AlertStatus.NEW)
                .firstDetected(LocalDateTime.now())
                .lastSeen(LocalDateTime.now())
                .build();

        when(alertService.getAlertDetail(5L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/security-audit/alerts/5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.conditionName").value("Password spray"));
    }
}