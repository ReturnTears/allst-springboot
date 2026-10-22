package com.allst.springboot.redis.geo;

import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.KeyGuard;
import com.allst.springboot.redis.common.RedisKeys;
import com.allst.springboot.redis.common.RedisValues;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 门店坐标放在 GEO，名称和分类放在 Hash。GEO 只负责「离我多远」，详情不要塞进成员名。
 * 对应文档 docs/13-geo.md。
 */
@Service
public class GeoService {

    private final StringRedisTemplate redis;

    public GeoService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Map<String, String> save(String id, String name, String category, double lng, double lat) {
        String shopId = KeyGuard.name(id, "门店");
        if (lng < -180 || lng > 180 || lat < -90 || lat > 90) {
            throw new BizException("经纬度超出范围");
        }
        if (name == null || name.isBlank() || name.length() > 40) {
            throw new BizException("门店名称长度需在 1 到 40 之间");
        }
        Map<String, String> info = new LinkedHashMap<>();
        info.put("id", shopId);
        info.put("name", name.trim());
        info.put("category", KeyGuard.name(category, "分类"));
        info.put("lng", String.valueOf(lng));
        info.put("lat", String.valueOf(lat));
        redis.opsForHash().putAll(RedisKeys.shop(shopId), info);
        redis.opsForGeo().add(RedisKeys.GEO_SHOPS, new Point(lng, lat), shopId);
        return info;
    }

    public List<Map<String, Object>> nearby(double lng, double lat, double radiusKm, int limit) {
        if (radiusKm <= 0 || radiusKm > 5000) {
            throw new BizException("半径需在 0 到 5000 公里之间");
        }
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance().includeCoordinates().sortAscending().limit(Math.min(Math.max(limit, 1), 20));
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo().radius(
                RedisKeys.GEO_SHOPS,
                new Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS)),
                args);
        List<Map<String, Object>> list = new ArrayList<>();
        if (results == null) {
            return list;
        }
        results.forEach(item -> {
            String id = item.getContent().getName();
            Map<String, Object> row = new LinkedHashMap<>();
            row.putAll(RedisValues.asMap(redis.opsForHash().entries(RedisKeys.shop(id))));
            row.put("id", id);
            if (item.getDistance() != null) {
                row.put("distance", item.getDistance().getValue());
                row.put("metric", item.getDistance().getMetric().getAbbreviation());
            }
            list.add(row);
        });
        return list;
    }

    public Map<String, Object> distance(String from, String to) {
        Distance distance = redis.opsForGeo().distance(
                RedisKeys.GEO_SHOPS,
                KeyGuard.name(from, "起点"),
                KeyGuard.name(to, "终点"),
                Metrics.KILOMETERS);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("kilometers", distance == null ? null : distance.getValue());
        return body;
    }

    public int seed() {
        save("hz-wulin", "武林门店", "咖啡", 120.1617, 30.2729);
        save("hz-xihu", "西湖门店", "咖啡", 120.1485, 30.2420);
        save("sh-nanjing", "南京路门店", "图书", 121.4737, 31.2352);
        save("bj-sanlitun", "三里屯门店", "咖啡", 116.4551, 39.9370);
        save("sz-nanshan", "南山门店", "数码", 113.9304, 22.5329);
        save("cd-chunxi", "春熙路门店", "图书", 104.0800, 30.6570);
        return 6;
    }
}
