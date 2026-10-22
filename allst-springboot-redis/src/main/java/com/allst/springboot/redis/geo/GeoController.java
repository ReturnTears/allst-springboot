package com.allst.springboot.redis.geo;

import com.allst.springboot.redis.common.ApiResult;
import com.allst.springboot.redis.common.Req;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/geo")
public class GeoController {

    private final GeoService geoService;

    public GeoController(GeoService geoService) {
        this.geoService = geoService;
    }

    @PostMapping("/shops")
    public ApiResult<?> save(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(geoService.save(
                Req.str(body, "id"),
                Req.str(body, "name"),
                Req.str(body, "category"),
                Req.doubleValue(body, "lng"),
                Req.doubleValue(body, "lat")));
    }

    @GetMapping("/nearby")
    public ApiResult<?> nearby(@RequestParam double lng,
                               @RequestParam double lat,
                               @RequestParam(defaultValue = "50") double radiusKm,
                               @RequestParam(defaultValue = "10") int limit) {
        return ApiResult.ok(geoService.nearby(lng, lat, radiusKm, limit));
    }

    @GetMapping("/distance")
    public ApiResult<?> distance(@RequestParam String from, @RequestParam String to) {
        return ApiResult.ok(geoService.distance(from, to));
    }

    @PostMapping("/seed")
    public ApiResult<?> seed() {
        return ApiResult.ok(geoService.seed());
    }
}
