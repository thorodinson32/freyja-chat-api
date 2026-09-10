package com.aesirlogic.freyjachat.service;

import com.aesirlogic.freyjachat.model.CardStrategyResearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.List;

@Service
public class CardStrategyQuotaService {
    private static final ZoneId ZONE = ZoneId.of("America/Chicago");
    private static final DefaultRedisScript<List> SCRIPT = new DefaultRedisScript<>("""
        local d=tonumber(redis.call('GET',KEYS[1]) or '0'); local m=tonumber(redis.call('GET',KEYS[2]) or '0')
        if d>=tonumber(ARGV[1]) or m>=tonumber(ARGV[2]) then return {-1,tonumber(ARGV[1])-d,tonumber(ARGV[2])-m} end
        d=redis.call('INCR',KEYS[1]); m=redis.call('INCR',KEYS[2])
        if d==1 then redis.call('EXPIRE',KEYS[1],ARGV[3]) end; if m==1 then redis.call('EXPIRE',KEYS[2],ARGV[4]) end
        return {1,tonumber(ARGV[1])-d,tonumber(ARGV[2])-m}
        """, List.class);
    private final StringRedisTemplate redis; private final int daily; private final int monthly;
    public CardStrategyQuotaService(StringRedisTemplate redis,
        @Value("${openai.card-strategy.daily-limit:1}") int daily,
        @Value("${openai.card-strategy.monthly-limit:8}") int monthly) { this.redis=redis; this.daily=daily; this.monthly=monthly; }
    public CardStrategyResearchResponse.Quota reserve() {
        ZonedDateTime now=ZonedDateTime.now(ZONE); String day=now.toLocalDate().toString(); String month=YearMonth.from(now).toString();
        long dayTtl=Duration.between(now,now.toLocalDate().plusDays(1).atStartOfDay(ZONE)).toSeconds()+60;
        long monthTtl=Duration.between(now,YearMonth.from(now).plusMonths(1).atDay(1).atStartOfDay(ZONE)).toSeconds()+60;
        try {
            List<?> result=redis.execute(SCRIPT,List.of("openai:card-strategy:day:"+day,"openai:card-strategy:month:"+month),String.valueOf(daily),String.valueOf(monthly),String.valueOf(dayTtl),String.valueOf(monthTtl));
            if(result==null||result.size()<3) throw new IllegalStateException("Invalid quota response");
            CardStrategyResearchResponse.Quota quota=CardStrategyResearchResponse.Quota.builder().dailyRemaining(Math.max(0,number(result.get(1)))).monthlyRemaining(Math.max(0,number(result.get(2)))).build();
            if(number(result.get(0))!=1) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Card-strategy research budget exhausted");
            return quota;
        } catch(ResponseStatusException e){throw e;} catch(DataAccessException|IllegalStateException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Card-strategy quota service unavailable",e);}
    }
    private long number(Object value){return value instanceof Number n?n.longValue():Long.parseLong(String.valueOf(value));}
}
