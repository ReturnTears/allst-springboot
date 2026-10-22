package com.allst.springboot.redis.common;

/**
 * 键名集中在这里，避免各业务各自拼接后对不上。
 * 规范：业务:实体:标识，示例数据一律放在 demo: 或各业务自己的前缀下，互不覆盖。
 */
public final class RedisKeys {

    public static final String NULL_VALUE = "__NULL__";

    public static final String STRUCT_STRING = "demo:struct:string";
    public static final String STRUCT_HASH = "demo:struct:hash";
    public static final String STRUCT_LIST = "demo:struct:list";
    public static final String STRUCT_SET_A = "demo:struct:set:a";
    public static final String STRUCT_SET_B = "demo:struct:set:b";
    public static final String STRUCT_ZSET = "demo:struct:zset";
    public static final String STRUCT_GEO = "demo:struct:geo";
    public static final String STRUCT_HLL = "demo:struct:hll";
    public static final String STRUCT_BITMAP = "demo:struct:bitmap";

    public static final String CACHE_BLOOM = "cache:product:bloom";
    public static final String CACHE_STAT_HIT = "cache:stat:hit";
    public static final String CACHE_STAT_MISS = "cache:stat:miss";
    public static final String CACHE_STAT_NULL = "cache:stat:null";

    public static final String RANK_BOARD = "rank:board";
    public static final String MQ_LIST = "mq:list:email";
    public static final String MQ_LIST_PROCESSING = "mq:list:email:processing";
    public static final String MQ_LIST_RESULT = "mq:list:email:result";
    public static final String MQ_LIST_DEAD = "mq:list:email:dead";
    public static final String MQ_STREAM = "mq:stream:email";
    public static final String MQ_STREAM_GROUP = "email-group";
    public static final String MQ_STREAM_RESULT = "mq:stream:email:result";
    public static final String MQ_STREAM_DEAD = "mq:stream:email:dead";

    public static final String PUBSUB_RECENT = "pubsub:recent";
    public static final String EXPIRE_LOG = "expire:log";
    public static final String SCHEDULE_DELAY = "schedule:delay";
    public static final String SCHEDULE_RESULT = "schedule:result";
    public static final String SHARE_CONFIG = "share:config";
    public static final String SHARE_CONFIG_VER = "share:config:ver";
    public static final String GEO_SHOPS = "geo:shops";

    private RedisKeys() {
    }

    public static String product(long id) {
        return "cache:product:" + id;
    }

    public static String productLock(long id) {
        return "cache:product:" + id + ":lock";
    }

    public static String session(String token) {
        return "session:" + token;
    }

    public static String cart(String userId) {
        return "cart:" + userId;
    }

    public static String articleView(long id) {
        return "counter:view:" + id;
    }

    public static String articleLikeUsers(long id) {
        return "counter:like:users:" + id;
    }

    public static String articleLikeCount(long id) {
        return "counter:like:count:" + id;
    }

    public static String analyticsPv(String day) {
        return "analytics:pv:" + day;
    }

    public static String analyticsUv(String day) {
        return "analytics:uv:" + day;
    }

    public static String analyticsHour(String day) {
        return "analytics:hour:" + day;
    }

    public static String analyticsHot(String day) {
        return "analytics:hot:" + day;
    }

    public static String pageHtml(String slug) {
        return "page:html:" + slug;
    }

    public static String follow(String user) {
        return "social:follow:" + user;
    }

    public static String fans(String user) {
        return "social:fans:" + user;
    }

    public static String feed(String user) {
        return "social:feed:" + user;
    }

    public static String online(String user) {
        return "social:online:" + user;
    }

    public static String interest(String userId) {
        return "recommend:interest:" + userId;
    }

    public static String recentItems(String userId) {
        return "recommend:recent:" + userId;
    }

    public static String similar(long itemId) {
        return "recommend:sim:" + itemId;
    }

    public static String seen(String userId) {
        return "recommend:seen:" + userId;
    }

    public static String shop(String id) {
        return "geo:shop:" + id;
    }

    public static String tsIndex(String metric) {
        return "ts:index:" + metric;
    }

    public static String tsValue(String metric) {
        return "ts:value:" + metric;
    }

    public static String serviceInstance(String service, String instance) {
        return "share:svc:" + service + ":" + instance;
    }
}
