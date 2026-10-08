/*
 * Copyright 2026 LLC SOLANOTECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solano.stage.web;

import com.solano.stage.json.JsonMapperFactory;
import com.solano.stage.messaging.IngestMessage;
import com.solano.stage.pipeline.StagePipeline;
import com.solano.stage.pipeline.StageProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IngestControllerTest {

    @Mock
    StagePipeline pipeline;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new IngestController(pipeline, JsonMapperFactory.create()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validBody_returns200_andInvokesPipeline() throws Exception {
        mockMvc.perform(post("/api/v1/client-data")
                        .contentType(APPLICATION_JSON)
                        .content("{\"data\":{\"firstName\":\"John\"},\"source\":\"standard\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        ArgumentCaptor<IngestMessage> captor = ArgumentCaptor.forClass(IngestMessage.class);
        verify(pipeline).process(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo("standard");
        assertThat(captor.getValue().getData().get("firstName").asText()).isEqualTo("John");
    }

    @Test
    void processingException_returns422_withDetail() throws Exception {
        doThrow(new StageProcessingException("validation failed"))
                .when(pipeline).process(any());

        mockMvc.perform(post("/api/v1/client-data")
                        .contentType(APPLICATION_JSON)
                        .content("{\"data\":{},\"source\":\"standard\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("validation failed"));
    }

    @Test
    void malformedJson_returns400_andDoesNotTouchPipeline() throws Exception {
        mockMvc.perform(post("/api/v1/client-data")
                        .contentType(APPLICATION_JSON)
                        .content("this is not json"))
                .andExpect(status().isBadRequest());

        verify(pipeline, never()).process(any());
    }
}
