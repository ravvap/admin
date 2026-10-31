package gov.fdic.tip.securityaudit.service;

import gov.fdic.tip.securityaudit.dto.SecurityAlertDto;
import gov.fdic.tip.securityaudit.entity.SecurityAlert;
import gov.fdic.tip.securityaudit.repository.SecurityAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityAlertServiceTest {

    @Mock
    private SecurityAlertRepository alertRepository;

    @InjectMocks
    private SecurityAlertService alertService;

    private SecurityAlert sampleAlert;

    @BeforeEach
    void setUp() {
        sampleAlert = SecurityAlert.builder()
                .id(1L)
                .severity(SecurityAlert.Severity.CRITICAL)
                .conditionName("Privilege escalation")
                .triggerType("CHECKED INSTANTLY")
                .accountSource("BOYD, Marcus (MBOYD)")
                .firstDetected(LocalDateTime.now())
                .lastSeen(LocalDateTime.now())
                .status(SecurityAlert.AlertStatus.NEW)
                .build();
    }

    @Test
    void testGetAlertDetail_Success() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(sampleAlert));

        SecurityAlertDto result = alertService.getAlertDetail(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Privilege escalation", result.getConditionName());
        verify(alertRepository, times(1)).findById(1L);
    }

    @Test
    void testGetAlertDetail_NotFound() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            alertService.getAlertDetail(99L);
        });

        assertTrue(exception.getMessage().contains("Security Alert not found"));
    }

    @Test
    void testTriageAlert_Success() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(sampleAlert));
        when(alertRepository.save(any(SecurityAlert.class))).thenReturn(sampleAlert);

        SecurityAlertDto result = alertService.triageAlert(
                1L, 
                SecurityAlert.AlertStatus.ACKNOWLEDGED, 
                null, 
                "Investigated event", 
                "JCARTER"
        );

        assertNotNull(result);
        assertEquals(SecurityAlert.AlertStatus.ACKNOWLEDGED, sampleAlert.getStatus());
        assertEquals("Investigated event", sampleAlert.getResolutionNote());
        assertEquals("JCARTER", sampleAlert.getHandledBy());
        verify(alertRepository, times(1)).save(any(SecurityAlert.class));
    }
}