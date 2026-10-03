package com.nsysmon.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AScalarDataPointTest {

    private Locale originalLocale;

    @BeforeEach
    void pinLocale() {
        // the rendering depends on the default locale's number symbols, so pin it for determinism
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    @Test
    void gettersReturnTheConstructorArguments() {
        final AScalarDataPoint point = new AScalarDataPoint(1234L, "cpu", 42L, 2);

        assertEquals(1234L, point.getTimestamp());
        assertEquals("cpu", point.getName());
        assertEquals(42L, point.getValueRaw());
        assertEquals(2, point.getNumFracDigits());
    }

    @Test
    void everyDataPointGetsItsOwnUuid() {
        assertNotEquals(
                new AScalarDataPoint(0, "a", 0, 0).getUuid(),
                new AScalarDataPoint(0, "a", 0, 0).getUuid());
    }

    @Test
    void valueWithoutFractionDigitsEqualsTheRawValue() {
        assertEquals(42.0d, new AScalarDataPoint(0, "a", 42L, 0).getValue());
    }

    @Test
    void valueIsScaledByTheNumberOfFractionDigits() {
        assertEquals(0.042d, new AScalarDataPoint(0, "a", 42L, 3).getValue());
    }

    @Test
    void valueIsScaledForOneFractionDigit() {
        assertEquals(4.2d, new AScalarDataPoint(0, "a", 42L, 1).getValue());
    }

    @Test
    void valueIsScaledForTwoFractionDigits() {
        assertEquals(0.42d, new AScalarDataPoint(0, "a", 42L, 2).getValue());
    }

    @Test
    void integerValueIsFormattedWithThousandsSeparators() {
        assertEquals("1,234", new AScalarDataPoint(0, "a", 1234L, 0).getFormattedValue());
    }

    @Test
    void fractionDigitValueIsRenderedWithThreeDecimals() {
        assertEquals("1,234.000", new AScalarDataPoint(0, "a", 1234L, 3).getFormattedValue());
    }

    @Test
    void fractionDigitValueIsRenderedWithOneDecimal() {
        assertEquals("1,234.0", new AScalarDataPoint(0, "a", 1234L, 1).getFormattedValue());
    }

    @Test
    void negativeValuesAreFormattedWithALeadingMinus() {
        assertEquals("-150.00", new AScalarDataPoint(0, "a", -150L, 2).getFormattedValue());
    }

    @Test
    void nineFractionDigitsAreSupported() {
        assertEquals("1.000000000", new AScalarDataPoint(0, "a", 1L, 9).getFormattedValue());
    }

    @Test
    void fractionDigitsSelectTheNumberOfDecimals() {
        assertEquals("1.00", new AScalarDataPoint(0, "a", 1L, 2).getFormattedValue());
        assertEquals("1.00000", new AScalarDataPoint(0, "a", 1L, 5).getFormattedValue());
    }

    @Test
    void formattedValueRendersTheRawValueWithoutScaling() {
        // the raw value is only padded with decimals, it is not divided by 10^numFracDigits
        final AScalarDataPoint point = new AScalarDataPoint(0, "a", 42L, 2);

        assertEquals(0.42d, point.getValue());
        assertEquals("42.00", point.getFormattedValue());
    }

    @Test
    void toStringShowsNameAndScaledValue() {
        assertEquals("AScalarDataPoint{cpu: 4.2}", new AScalarDataPoint(0, "cpu", 42L, 1).toString());
    }
}