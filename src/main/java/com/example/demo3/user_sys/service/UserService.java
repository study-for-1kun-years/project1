package com.example.demo3.user_sys.service;

import com.example.demo3.user_sys.dto.ChangePasswordRequest;
import com.example.demo3.user_sys.dto.UserStatsDTO;
import com.example.demo3.user_sys.entity.User;
import com.example.demo3.user_sys.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private static final String[] NICKNAME_PREFIX = {"小", "大", "老", "阿", "萌"};
    private static final String[] NICKNAME_SUFFIX = {"猫", "狗", "熊", "兔", "鱼"};
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";
    private static final String ROLE_EDITOR = "EDITOR";
    private static final String STATUS_ON = "on";
    private static final String STATUS_OFF = "off";

    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Transactional
    public User saveUser(User user) {
        if (user.getNickname() == null || user.getNickname().isEmpty()) {
            user.setNickname(generateRandomNickname());
        }
        if (!isValidRole(user.getRole())) {
            user.setRole(ROLE_USER);
        }
        if (!isValidStatus(user.getStatus())) {
            user.setStatus(STATUS_ON);
        }
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void updateLoginInfo(String username, HttpServletRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        String ip = request.getRemoteAddr();
        LocalDateTime loginTime = LocalDateTime.now();
        userRepository.updateLoginInfo(username, ip, loginTime);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (!request.getOldPassword().equals(user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "旧密码错误");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码不能与旧密码相同");
        }

        user.setPassword(request.getNewPassword());
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        userRepository.deleteById(id);
    }

    @Transactional
    public void setUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (!isValidRole(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色只能是 ADMIN、USER 或 EDITOR");
        }
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional
    public void setUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (!isValidStatus(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "状态只能是 on 或 off");
        }
        user.setStatus(status);
        userRepository.save(user);
    }

    private String generateRandomNickname() {
        Random random = new Random();
        String prefix = NICKNAME_PREFIX[random.nextInt(NICKNAME_PREFIX.length)];
        String suffix = NICKNAME_SUFFIX[random.nextInt(NICKNAME_SUFFIX.length)];
        int number = random.nextInt(1000);
        return prefix + suffix + number;
    }

    private boolean isValidRole(String role) {
        return role != null && (role.equals(ROLE_ADMIN) || role.equals(ROLE_USER) || role.equals(ROLE_EDITOR));
    }

    private boolean isValidStatus(String status) {
        return status != null && (status.equals(STATUS_ON) || status.equals(STATUS_OFF));
    }

    public List<User> getUsersByIds(List<Long> userIds) {
        return userRepository.findAllById(userIds);
    }

    public UserStatsDTO getUserStats() {
        List<User> allUsers = userRepository.findAll();
        int totalUsers = allUsers.size();
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long monthlyIncrease = allUsers.stream()
                .filter(u -> u.getCreate_time().isAfter(startOfMonth))
                .count();

        List<LocalDate> last30Days = IntStream.range(0, 30)
                .mapToObj(i -> LocalDate.now().minusDays(i))
                .collect(Collectors.toList());
        Map<LocalDate, Long> trend = new TreeMap<>(Collections.reverseOrder());
        last30Days.forEach(date -> trend.put(date, 0L));

        allUsers.forEach(user -> {
            LocalDate date = user.getCreate_time().toLocalDate();
            if (trend.containsKey(date)) {
                trend.put(date, trend.get(date) + 1);
            }
        });

        List<String> dates = new ArrayList<>(trend.keySet().stream().map(LocalDate::toString).toList());
        List<Long> counts = new ArrayList<>(trend.values());
        return new UserStatsDTO(totalUsers, (int) monthlyIncrease, dates, counts);
    }
}
