package com.aesirlogic.freyjachat.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StockNewsQuotaServiceTest {
    @Test
    void returnsAtomicDailyMonthlyAndManualRemainders() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(1L, 5L, 119L, 1L));

        var quota = new StockNewsQuotaService(redis, 6, 120, 2).reserve(true);

        assertThat(quota.getDailyRemaining()).isEqualTo(5);
        assertThat(quota.getMonthlyRemaining()).isEqualTo(119);
        assertThat(quota.getManualRemaining()).isEqualTo(1);
        verify(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    @Test
    void exhaustedQuotaReturns429() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(-1L, 0L, 44L, 0L));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> new StockNewsQuotaService(redis, 6, 120, 2).reserve(true));

        assertThat(error.getStatusCode().value()).isEqualTo(429);
    }

    @Test
    void redisFailureFailsClosedWith503() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("down"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> new StockNewsQuotaService(redis, 6, 120, 2).reserve(false));

        assertThat(error.getStatusCode().value()).isEqualTo(503);
    }
}
