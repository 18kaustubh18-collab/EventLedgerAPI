package com.eventledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void duplicateEventIdReturnsConflict() throws Exception {
        String payload = "{\"eventId\":\"evt-dup\",\"accountId\":\"acct-1\",\"type\":\"CREDIT\",\"amount\":10.00,\"currency\":\"USD\",\"eventTimestamp\":\"2026-05-15T08:00:00Z\",\"metadata\":{\"source\":\"test\"}}";

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("event id already exists"));
    }

    @Test
    void outOfRangePageReturnsRecordNotExistError() throws Exception {
        String event1 = "{\"eventId\":\"evt-1\",\"accountId\":\"acct-1\",\"type\":\"CREDIT\",\"amount\":10.00,\"currency\":\"USD\",\"eventTimestamp\":\"2026-05-15T08:00:00Z\",\"metadata\":{\"source\":\"test\"}}";
        String event2 = "{\"eventId\":\"evt-2\",\"accountId\":\"acct-1\",\"type\":\"CREDIT\",\"amount\":20.00,\"currency\":\"USD\",\"eventTimestamp\":\"2026-05-15T09:00:00Z\",\"metadata\":{\"source\":\"test\"}}";

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(event1))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(event2))
                .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/events")
                .param("account", "acct-1")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("this many record doesn't exist"));
    }
}
