package com.mistar.memo.domain.exception.auth

class RefreshTokenNotMatchedException : RuntimeException("refresh token이 일치하지 않습니다.")