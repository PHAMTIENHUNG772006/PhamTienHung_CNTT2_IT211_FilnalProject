package com.re.controller;

import com.re.model.dto.auth.UserResponse;
import com.re.model.entity.Job;
import com.re.model.entity.enums.JobStatus;
import com.re.service.JobService;
import com.re.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private UserService userService;

    private UserResponse mockUserResponse;
    private Job mockJob;

    @BeforeEach
    void setUp() {
        mockUserResponse = UserResponse.builder()
                .id(1L)
                .username("candidate_test")
                .email("test@gmail.com")
                .role("ROLE_CANDIDATE")
                .build();

        mockJob = Job.builder()
                .id(10L)
                .title("Java Developer")
                .status(JobStatus.APPROVED)
                .build();
    }

    // Test 6: Cập nhật trạng thái Job thành công bởi ADMIN
    @Test
    @WithMockUser(roles = "ADMIN")
    void updateJobStatus_Success_ShouldReturnOk() throws Exception {
        when(jobService.updateStatus(10L, JobStatus.APPROVED)).thenReturn(mockJob);

        mockMvc.perform(put("/api/v1/admin/jobs/10/status")
                        .param("status", "APPROVED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Java Developer"));
    }

    // Test 7: ADMIN lấy danh sách người dùng thành công (Phân trang)
    @Test
    @WithMockUser(roles = "ADMIN")
    void getUser_Success_ShouldReturnPagedUsers() throws Exception {
        Page<UserResponse> page = new PageImpl<>(List.of(mockUserResponse));
        when(userService.getAllUser(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].username").value("candidate_test"));
    }

    // Test 8: ADMIN khóa tài khoản thành công
    @Test
    @WithMockUser(roles = "ADMIN")
    void updateJobLock_Success_ShouldReturnOk() throws Exception {
        when(userService.lockUserAccount(1L)).thenReturn(mockUserResponse);

        mockMvc.perform(put("/api/v1/admin/users/1/lockUser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Khóa tài khoản người dùng"));
    }

    // Test 9: Chặn người dùng thông thường (ROLE_CANDIDATE) truy cập API Admin (403 Forbidden)
    @Test
    @WithMockUser(roles = "CANDIDATE")
    void adminApi_AccessedByCandidate_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // Test 10: Người dùng chưa đăng nhập gọi API Admin (401 Unauthorized)
    @Test
    void adminApi_AnonymousUser_ShouldReturnUnauthorizedOrForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}