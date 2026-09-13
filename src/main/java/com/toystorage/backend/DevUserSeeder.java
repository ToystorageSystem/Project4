//package com.toystorage.backend;
//
//import com.toystorage.backend.entity.auth.Roles;
//import com.toystorage.backend.entity.auth.UserRoles;
//import com.toystorage.backend.entity.users.Users;
//import com.toystorage.backend.enums.users.UserStatus;
//import com.toystorage.backend.repository.auth.RoleRepository;
//import com.toystorage.backend.repository.auth.UserRoleRepository;
//import com.toystorage.backend.repository.users.UserRepository;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class DevUserSeeder implements CommandLineRunner {
//
//    private final UserRepository userRepository;
//    private final RoleRepository roleRepository;
//    private final UserRoleRepository userRoleRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    public DevUserSeeder(
//            UserRepository userRepository,
//            RoleRepository roleRepository,
//            UserRoleRepository userRoleRepository,
//            PasswordEncoder passwordEncoder
//    ) {
//        this.userRepository = userRepository;
//        this.roleRepository = roleRepository;
//        this.userRoleRepository = userRoleRepository;
//        this.passwordEncoder = passwordEncoder;
//    }
//
//    @Override
//    public void run(String... args) {
//
//        createOrUpdateUser(
//                "DEV001",
//                "dohoangducminh19032004@gmail.com",
//                "Đỗ Hoàng Đức Minh",
//                "000000000007",
//                7L
//        );
//
//        createOrUpdateUser(
//                "DEV002",
//                "phuctan053@gmail.com",
//                "Phúc Tân",
//                "000000000008",
//                1L
//        );
//
//        createOrUpdateUser(
//                "DEV003",
//                "quocdatvan97@gmail.com",
//                "Quốc Đạt",
//                "000000000009",
//                3L
//        );
//
//        createOrUpdateUser(
//                "DEV004",
//                "dinhlam030805@gmail.com",
//                "Đinh Lâm",
//                "000000000010",
//                2L
//        );
//
//        System.out.println();
//        System.out.println("==========================================");
//        System.out.println("ĐÃ TẠO / CẬP NHẬT 4 TÀI KHOẢN DEV");
//        System.out.println("Password tất cả tài khoản: 123456");
//        System.out.println("==========================================");
//        System.out.println();
//    }
//
//    private void createOrUpdateUser(
//            String userCode,
//            String email,
//            String name,
//            String identityCard,
//            Long roleId
//    ) {
//
//        // 1. Tìm role
//        Roles role = roleRepository.findById(roleId)
//                .orElseThrow(() ->
//                        new RuntimeException(
//                                "Không tìm thấy role ID: " + roleId
//                        )
//                );
//
//        // 2. Tìm user theo email hoặc tạo mới
//        Users user = userRepository
//                .findByEmail(email)
//                .orElseGet(Users::new);
//
//        // 3. Gán thông tin user
//        user.setUserCode(userCode);
//        user.setEmail(email);
//        user.setName(name);
//        user.setIdentityCard(identityCard);
//
//        // 4. Status
//        user.setStatus(UserStatus.ACTIVE);
//
//        // 5. Không bắt đổi mật khẩu
//        user.setMustChangePassword(false);
//
//        // 6. Mật khẩu đăng nhập = 123456
//        user.setPassword(
//                passwordEncoder.encode("123456")
//        );
//
//        // 7. Chưa gán warehouse
//        user.setWarehouse(null);
//
//        // 8. Lưu user
//        user = userRepository.save(user);
//
//        // 9. Kiểm tra user đã có role chưa
//        boolean alreadyHasRole =
//                userRoleRepository.existsByUser_IdAndRole_Id(
//                        user.getId(),
//                        roleId
//                );
//
//        // 10. Nếu chưa có role thì tạo UserRoles
//        if (!alreadyHasRole) {
//
//            UserRoles userRole = new UserRoles();
//
//            userRole.setUser(user);
//            userRole.setRole(role);
//
//            // QUAN TRỌNG:
//            // DB bắt buộc user_roles_code không được null
//            userRole.setUserRolesCode(
//                    "UR-" + userCode + "-" + roleId
//            );
//
//            userRoleRepository.save(userRole);
//
//            System.out.println(
//                    "✓ Đã gán role "
//                            + roleId
//                            + " cho "
//                            + email
//            );
//
//        } else {
//
//            System.out.println(
//                    "✓ "
//                            + email
//                            + " đã có role "
//                            + roleId
//            );
//        }
//
//        System.out.println(
//                "✓ User: "
//                        + email
//                        + " | Code: "
//                        + userCode
//                        + " | Role: "
//                        + roleId
//                        + " | Password: 123456"
//        );
//
//        System.out.println("------------------------------------------");
//    }
//}