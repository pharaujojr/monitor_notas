package br.com.pharaujo.monitornfe.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalNotasDetectadas").isNumber())
            .andExpect(jsonPath("$.ultimasNotas").isArray());
    }

    @Test
    void shouldListNotes() throws Exception {
        mockMvc.perform(get("/api/notes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }
}
