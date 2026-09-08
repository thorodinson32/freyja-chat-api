package com.aesirlogic.freyjachat.service;

import com.aesirlogic.freyjachat.model.StockNewsResearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class StockNewsQuotaService {
    private static final ZoneId ZONE = ZoneId.of("America/Chicago");
    private static final DefaultRedisScript<List> RESERVE_SCRIPT = new DefaultRedisScript<>("""
            local daily = tonumber(redis.call('GET', KEYS[1]) or '0')
            local monthly = tonumber(redis.call('GET', KEYS[2]) or '0')
            local manual = tonumber(redis.call('GET', KEYS[3]) or '0')
            local isManual = tonumber(ARGV[4])
            if daily >= tonumber(ARGV[1]) or monthly >= tonumber(ARGV[2]) or
               (isManual == 1 and manual >= tonumber(ARGV[3])) then
              return {-1, tonumber(ARGV[1]) - daily, tonumber(ARGV[2]) - monthly, tonumber(ARGV[3]) - manual}
            end
            daily = redis.call('INCR', KEYS[1])
            monthly = redis.call('INCR', KEYS[2])
            if daily == 1 then redis.call('EXPIRE', KEYS[1], ARGV[5]) end
            if monthly == 1 then redis.call('EXPIRE', KEYS[2], ARGV[6]) end
            if isManual == 1 then
              manual = redis.call('INCR', KEYS[3])
              if manual == 1 then redis.call('EXPIRE', KEYS[3], ARGV[5]) end
            end
            return {1, tonumber(ARGV[1]) - daily, tonumber(ARGV[2]) - monthly, tonumber(ARGV[3]) - manual}
            """, List.class);

    private final StringRedisTemplate redis;
    private final int dailyLimit;
    private final int monthlyLimit;
    private final int manualDailyLimit;

    public StockNewsQuotaService(StringRedisTemplate redis,
                                 @Value("${openai.stock-news.daily-limit:6}") int dailyLimit,
                                 @Value("${openai.stock-news.monthly-limit:120}") int monthlyLimit,
                                 @Value("${openai.stock-news.manual-daily-limit:2}") int manualDailyLimit) {
        this.redis = redis;
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
        this.manualDailyLimit = manualDailyLimit;
    }

    public StockNewsResearchResponse.Quota reserve(boolean manual) {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        String day = now.toLocalDate().toString();
        String month = YearMonth.from(now).toString();
        long dayTtl = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(ZONE)).toSeconds() + 60;
        long monthTtl = Duration.between(now, YearMonth.from(now).plusMonths(1).atDay(1).atStartOfDay(ZONE)).toSeconds() + 60;
        try {
            List<?> result = redis.execute(RESERVE_SCRIPT,
                    List.of("openai:stock-news:day:" + day, "openai:stock-news:month:" + month,
                            "openai:stock-news:manual:" + day),
                    String.valueOf(dailyLimit), String.valueOf(monthlyLimit), String.valueOf(manualDailyLimit),
                    manual ? "1" : "0", String.valueOf(dayTtl), String.valueOf(monthTtl));
            if (result == null || result.size() < 4) throw new IllegalStateException("Invalid quota response");
            long allowed = number(result.get(0));
            var quota = StockNewsResearchResponse.Quota.builder()
                    .dailyRemaining(Math.max(0, number(result.get(1))))
                    .monthlyRemaining(Math.max(0, number(result.get(2))))
                    .manualRemaining(Math.max(0, number(result.get(3))))
                    .build();
            if (allowed != 1) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Stock-news research budget exhausted");
            }
            return quota;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (DataAccessException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Stock-news quota service unavailable", e);
        }
    }

    private long number(Object value) {
        return value instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(value));
    }
}
