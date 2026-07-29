package com.tongji.profile.api.dto;

import java.time.LocalDate;

public record ProfileResponse(
        Long id,
        String nickname,
        String avatar,
        String bio,
        String unreadIsleId,
        String gender,
        LocalDate birthday,
        String school,
        String phone,
        String email,
        String tagJson
) {}