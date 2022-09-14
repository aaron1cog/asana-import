package com.cappella.asana;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testing the {@link AsanaClient} class and it's Spring configuration.
 */
@SpringBootTest
public class AsanaClientSpringTests {
    @Autowired
    private AsanaClient client;
    
    @Test
    void dependencyInjectionConfiguredCorrectly() {
        assertNotNull(client);
    }
}
