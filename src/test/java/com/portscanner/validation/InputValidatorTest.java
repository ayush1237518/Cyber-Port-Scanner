package com.portscanner.validation;

import com.portscanner.model.ScanConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests covering the validation rules in {@link InputValidator}.
 */
class InputValidatorTest {

    @Test
    void validatesIpv4Target() throws ValidationException {
        assertEquals("192.168.1.1", InputValidator.validateTarget("192.168.1.1"));
    }

    @Test
    void validatesHostnameTarget() throws ValidationException {
        assertEquals("example.com", InputValidator.validateTarget("example.com"));
    }

    @Test
    void rejectsBlankTarget() {
        assertThrows(ValidationException.class, () -> InputValidator.validateTarget("   "));
    }

    @Test
    void rejectsMalformedTarget() {
        assertThrows(ValidationException.class, () -> InputValidator.validateTarget("not_a_valid_host!!"));
    }

    @Test
    void rejectsStartPortGreaterThanEndPort() {
        assertThrows(ValidationException.class, () -> InputValidator.validatePortRange(500, 100));
    }

    @Test
    void rejectsPortOutOfRange() {
        assertThrows(ValidationException.class, () -> InputValidator.validatePort(70000, "Start port"));
        assertThrows(ValidationException.class, () -> InputValidator.validatePort(0, "Start port"));
    }

    @Test
    void rejectsThreadCountOutOfRange() {
        assertThrows(ValidationException.class, () -> InputValidator.validateThreadCount(0));
        assertThrows(ValidationException.class, () -> InputValidator.validateThreadCount(10_000));
    }

    @Test
    void rejectsNonNumericField() {
        assertThrows(ValidationException.class, () -> InputValidator.parseIntField("abc", "Timeout"));
    }

    @Test
    void buildsValidConfigFromValidInputs() throws ValidationException {
        ScanConfig config = InputValidator.buildValidatedConfig(
                "127.0.0.1", "1", "100", "500", "50", true);

        assertEquals("127.0.0.1", config.getTarget());
        assertEquals(1, config.getStartPort());
        assertEquals(100, config.getEndPort());
        assertEquals(500, config.getTimeoutMillis());
        assertEquals(50, config.getThreadCount());
        assertTrue(config.isGrabBanners());
        assertEquals(100, config.getTotalPorts());
    }
}
