package com.re.service;

import com.re.exceptions.BadRequestException;
import com.re.exceptions.ResourceNotFoundException;
import com.re.exceptions.ConflictException;
import com.re.model.dto.auth.RegisterRequest;
import com.re.model.dto.auth.UserResponse;
import com.re.model.entity.Company;
import com.re.model.entity.Role;
import com.re.model.entity.User;
import com.re.model.entity.enums.RegisterType;
import com.re.repository.CompanyRepository;
import com.re.repository.RoleRepository;
import com.re.repository.UserRepository;
import com.re.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest candidateRequest;
    private RegisterRequest employerRequest;
    private Role candidateRole;
    private User savedUser;

    @BeforeEach
    void setUp() {
        candidateRequest = RegisterRequest.builder()
                .username("test_candidate")
                .email("candidate@gmail.com")
                .password("password123")
                .fullName("Nguyen Van A")
                .registerType(RegisterType.CANDIDATE)
                .build();

        employerRequest = RegisterRequest.builder()
                .username("test_employer")
                .email("employer@gmail.com")
                .password("password123")
                .fullName("Nha Tuyen Dung")
                .registerType(RegisterType.EMPLOYER)
                .companyName("") // Đóng giả trường hợp tên công ty trống
                .build();

        candidateRole = new Role(1L, "ROLE_CANDIDATE");

        savedUser = User.builder()
                .id(1L)
                .username("test_candidate")
                .email("candidate@gmail.com")
                .role(candidateRole)
                .build();
    }

    // Test 1: Đăng ký Candidate thành công
    @Test
    void register_CandidateSuccess_ShouldReturnUserResponse() {
        when(userRepository.existsByEmail(candidateRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByRoleName("ROLE_CANDIDATE")).thenReturn(Optional.of(candidateRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = authService.register(candidateRequest);

        assertNotNull(response);
        assertEquals(savedUser.getEmail(), response.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    // Test 2: Đăng ký thất bại do Email đã tồn tại (Ném lỗi ConflictException)
    @Test
    void register_EmailExists_ShouldThrowConflictException() {
        when(userRepository.existsByEmail(candidateRequest.getEmail())).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(candidateRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    // Test 3: Đăng ký vai trò Employer thất bại do bỏ trống tên công ty (BadRequestException)
    @Test
    void register_EmployerMissingCompanyName_ShouldThrowBadRequestException() {
        when(userRepository.existsByEmail(employerRequest.getEmail())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.register(employerRequest));
    }

    // Test 4: Đăng ký thất bại vì hệ thống chưa cấu hình Role (ResourceNotFoundException)
    @Test
    void register_RoleNotFound_ShouldThrowResourceNotFoundException() {
        when(userRepository.existsByEmail(candidateRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByRoleName("ROLE_CANDIDATE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.register(candidateRequest));
    }

    // Test 5: Đăng ký Employer thành công và phải lưu kèm thông tin Company
    @Test
    void register_EmployerSuccess_ShouldSaveCompany() {
        employerRequest.setCompanyName("FPT Software");
        Role employerRole = new Role(2L, "ROLE_EMPLOYER");
        User savedEmployer = User.builder().id(2L).username("employer").email("employer@gmail.com").role(employerRole).build();

        when(userRepository.existsByEmail(employerRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByRoleName("ROLE_EMPLOYER")).thenReturn(Optional.of(employerRole));
        when(userRepository.save(any(User.class))).thenReturn(savedEmployer);

        UserResponse response = authService.register(employerRequest);

        assertNotNull(response);
        verify(companyRepository, times(1)).save(any(Company.class));
    }
}