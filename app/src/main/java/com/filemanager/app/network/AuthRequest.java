package com.filemanager.app.network;

public class AuthRequest {
    // 서버의 변수명과 완벽하게 일치해야 합니다. (username -> email 로 변경)
    private String email;
    private String password;

    // 생성자
    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getter
    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}