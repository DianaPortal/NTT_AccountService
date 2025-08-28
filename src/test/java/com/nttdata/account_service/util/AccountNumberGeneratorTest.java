package com.nttdata.account_service.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AccountNumberGeneratorTest {

    @Test
    void numeric_generaSoloDigitosConLongitud() {
        String s = AccountNumberGenerator.numeric(16);
        assertNotNull(s);
        assertEquals(16, s.length());
        assertTrue(s.chars().allMatch(Character::isDigit));
    }
}
