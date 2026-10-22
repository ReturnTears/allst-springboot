const { createApp } = Vue;

const panelMixin = {
  data() {
    return { result: null, error: "", busy: false };
  },
  methods: {
    api,
    async run(work) {
      this.busy = true;
      this.error = "";
      try {
        this.result = await work();
      } catch (error) {
        this.error = error.message || String(error);
      } finally {
        this.busy = false;
      }
    },
    pretty(value) {
      return typeof value === "string" ? value : JSON.stringify(value, null, 2);
    }
  }
};

const ResultBlock = {
  props: ["value", "error"],
  template: `
    <p class="error" v-if="error">{{ error }}</p>
    <pre class="result" v-if="value !== null && value !== undefined">{{ text }}</pre>
  `,
  computed: {
    text() {
      return typeof this.value === "string" ? this.value : JSON.stringify(this.value, null, 2);
    }
  }
};

const StructurePanel = {
  mixins: [panelMixin],
  data() {
    return { type: "string", op: "get", key: "demo:play:string", value: "hello", extra: "" };
  },
  computed: {
    steps() {
      return Array.isArray(this.result) && this.result[0] && this.result[0].command ? this.result : null;
    }
  },
  template: `
    <section class="card">
      <div class="row">
        <label>结构</label>
        <select v-model="type">
          <option>string</option><option>hash</option><option>list</option><option>set</option>
          <option>zset</option><option>geo</option><option>hyperloglog</option><option>bitmap</option>
        </select>
        <button :disabled="busy" @click="run(() => api('/api/structure/' + type + '/demo'))">运行示例</button>
      </div>
      <p class="doc">示例会重置 demo:struct:* 下面的键，不影响其它章节。</p>
    </section>
    <section class="card">
      <div class="row">
        <input v-model="op" placeholder="op，例如 get / set / incr">
        <input v-model="key" placeholder="key">
        <input v-model="value" placeholder="value">
        <button class="ghost" :disabled="busy" @click="play">执行</button>
      </div>
      <textarea v-model="extra" placeholder='可选，合并进请求体的 JSON，例如 {"field":"city","delta":1}'></textarea>
    </section>
    <p class="error" v-if="error">{{ error }}</p>
    <div class="steps" v-if="steps">
      <article class="card step" v-for="(step, index) in steps" :key="index">
        <code>{{ step.command }}</code>
        <p>{{ step.comment }}</p>
        <pre class="result">{{ pretty(step.result) }}</pre>
      </article>
    </div>
    <result-block v-else :value="result"></result-block>
  `,
  methods: {
    play() {
      return this.run(async () => {
        let extra = {};
        if (this.extra.trim()) {
          extra = JSON.parse(this.extra);
        }
        const body = Object.assign({ op: this.op, key: this.key, value: this.value }, extra);
        return api("/api/structure/" + this.type + "/play", {
          method: "POST",
          body: JSON.stringify(body)
        });
      });
    }
  }
};

const CachePanel = {
  mixins: [panelMixin],
  data() {
    return { id: 1, name: "机械键盘", price: "399.00", stock: 20, detail: "热插拔轴体", category: "外设" };
  },
  template: `
    <section class="card">
      <div class="row">
        <label>商品</label>
        <input v-model.number="id" type="number">
        <button :disabled="busy" @click="run(() => api('/api/cache/products/' + id))">读取</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/cache/stats'))">统计</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/cache/bloom/rebuild', {method:'POST'}))">重建布隆</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/cache/categories'))">字典</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/cache/categories/evict', {method:'POST'}))">清字典缓存</button>
      </div>
      <p class="doc">连续读同一商品，source 会从 MISS 变成 HIT。读一个很大的 id，布隆过滤器会直接返回 BLOOM。</p>
    </section>
    <section class="card">
      <div class="grid">
        <input v-model="name" placeholder="名称">
        <input v-model="price" placeholder="价格">
        <input v-model.number="stock" type="number" placeholder="库存">
        <input v-model="category" placeholder="分类">
      </div>
      <textarea v-model="detail"></textarea>
      <button :disabled="busy" @click="save">更新并删除缓存</button>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  methods: {
    save() {
      return this.run(() => api("/api/cache/products/" + this.id, {
        method: "PUT",
        body: JSON.stringify({
          name: this.name, price: this.price, stock: this.stock, detail: this.detail, category: this.category
        })
      }));
    }
  }
};

const SessionPanel = {
  mixins: [panelMixin],
  data() { return { username: "ada", skuId: "keyboard", qty: 1 }; },
  template: `
    <section class="card">
      <div class="row">
        <input v-model="username" placeholder="用户名">
        <button :disabled="busy" @click="login">登录</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/session/me'))">当前会话</button>
        <button class="ghost" :disabled="busy" @click="logout">退出</button>
      </div>
    </section>
    <section class="card">
      <div class="row">
        <input v-model="skuId" placeholder="sku">
        <input v-model.number="qty" type="number">
        <button :disabled="busy" @click="add">加入购物车</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/session/cart'))">查看购物车</button>
      </div>
      <p class="doc">登录态 30 分钟滑动过期。退出只删会话，购物车还在。</p>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  methods: {
    login() {
      return this.run(async () => {
        const data = await api("/api/session/login", { method: "POST", body: JSON.stringify({ username: this.username }) });
        localStorage.setItem("redis-demo-token", data.token);
        return data;
      });
    },
    logout() {
      return this.run(async () => {
        const data = await api("/api/session/logout", { method: "POST" });
        localStorage.removeItem("redis-demo-token");
        return data;
      });
    },
    add() {
      return this.run(() => api("/api/session/cart", {
        method: "POST",
        body: JSON.stringify({ skuId: this.skuId, qty: this.qty })
      }));
    }
  }
};

const RankPanel = {
  mixins: [panelMixin],
  data() { return { user: "ada", delta: 10, articleId: 1 }; },
  template: `
    <section class="card">
      <div class="row">
        <button :disabled="busy" @click="run(() => api('/api/rank/seed', {method:'POST'}))">填充榜单</button>
        <input v-model="user">
        <input v-model.number="delta" type="number">
        <button :disabled="busy" @click="score">加分</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/rank/top'))">前十名</button>
      </div>
    </section>
    <section class="card">
      <div class="row">
        <label>文章</label>
        <input v-model.number="articleId" type="number">
        <button :disabled="busy" @click="run(() => api('/api/rank/articles/' + articleId + '/view', {method:'POST'}))">浏览 +1</button>
        <button :disabled="busy" @click="like(true)">点赞</button>
        <button class="ghost" :disabled="busy" @click="like(false)">取消</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/rank/articles/' + articleId))">计数</button>
      </div>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  methods: {
    score() {
      return this.run(() => api("/api/rank/score", { method: "POST", body: JSON.stringify({ user: this.user, delta: this.delta }) }));
    },
    like(liked) {
      return this.run(() => api("/api/rank/articles/" + this.articleId + "/like", {
        method: "POST",
        body: JSON.stringify({ user: this.user, liked: liked })
      }));
    }
  }
};

const MqPanel = {
  mixins: [panelMixin],
  data() { return { to: "ada", subject: "欢迎", body: "这是一封异步邮件" }; },
  template: `
    <section class="card">
      <div class="grid">
        <input v-model="to" placeholder="收件人">
        <input v-model="subject" placeholder="主题，填 fail 会进死信">
        <textarea v-model="body"></textarea>
      </div>
      <div class="row">
        <button :disabled="busy" @click="send('list')">List 入队</button>
        <button :disabled="busy" @click="send('stream')">Stream 入队</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/mq/list'))">看 List</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/mq/stream'))">看 Stream</button>
      </div>
      <p class="doc">后台每秒消费。主题以 fail 开头的邮件会进死信。</p>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  methods: {
    send(kind) {
      return this.run(() => api("/api/mq/" + kind, {
        method: "POST",
        body: JSON.stringify({ to: this.to, subject: this.subject, body: this.body })
      }));
    }
  }
};

const AnalyticsPanel = {
  mixins: [panelMixin],
  data() { return { userId: "ada", page: "home" }; },
  template: `
    <section class="card">
      <div class="row">
        <input v-model="userId" placeholder="用户">
        <input v-model="page" placeholder="页面">
        <button :disabled="busy" @click="hit">记录访问</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/analytics/summary'))">今日汇总</button>
      </div>
      <p class="doc">同一个用户多次访问，PV 增加，UV 基本不变。</p>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  methods: {
    hit() {
      return this.run(() => api("/api/analytics/hit", { method: "POST", body: JSON.stringify({ userId: this.userId, page: this.page }) }));
    }
  }
};

const LockPanel = {
  mixins: [panelMixin],
  data() { return { productId: 1, qty: 1, key: "order", ttlSeconds: 15, token: "" }; },
  template: `
    <section class="card">
      <div class="row">
        <label>商品</label>
        <input v-model.number="productId" type="number">
        <label>数量</label>
        <input v-model.number="qty" type="number">
        <button :disabled="busy" @click="deduct">Redisson 扣库存</button>
      </div>
    </section>
    <section class="card">
      <div class="row">
        <input v-model="key" placeholder="锁名">
        <input v-model.number="ttlSeconds" type="number">
        <button :disabled="busy" @click="tryLock">SET NX 加锁</button>
        <input v-model="token" placeholder="token">
        <button class="ghost" :disabled="busy" @click="unlock">按 token 解锁</button>
      </div>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  methods: {
    deduct() {
      return this.run(() => api("/api/lock/stock/deduct", { method: "POST", body: JSON.stringify({ productId: this.productId, qty: this.qty }) }));
    },
    tryLock() {
      return this.run(async () => {
        const data = await api("/api/lock/manual/try", { method: "POST", body: JSON.stringify({ key: this.key, ttlSeconds: this.ttlSeconds }) });
        if (data.token) this.token = data.token;
        return data;
      });
    },
    unlock() {
      return this.run(() => api("/api/lock/manual/unlock", { method: "POST", body: JSON.stringify({ key: this.key, token: this.token }) }));
    }
  }
};

const PubSubPanel = {
  mixins: [panelMixin],
  data() { return { channel: "broadcast", message: "系统将在 5 分钟后维护", live: [], es: null }; },
  computed: {
    liveText() {
      return this.live.length ? this.live.join("\n") : "等待消息";
    }
  },
  template: `
    <section class="card">
      <div class="row">
        <select v-model="channel"><option>broadcast</option><option>order</option><option>im</option></select>
        <input v-model="message" style="min-width:280px">
        <button :disabled="busy" @click="publish">发布</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/pubsub/recent'))">最近消息</button>
      </div>
      <p class="doc">下面是当前页面的 SSE 实时日志。刷新页面后，离线期间的消息看「最近消息」。</p>
      <pre class="result">{{ liveText }}</pre>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  mounted() {
    this.es = new EventSource("/api/pubsub/stream");
    this.es.addEventListener("notify", (event) => {
      this.live.unshift(event.data);
      this.live = this.live.slice(0, 20);
    });
  },
  beforeUnmount() {
    if (this.es) this.es.close();
  },
  methods: {
    publish() {
      return this.run(() => api("/api/pubsub/publish", { method: "POST", body: JSON.stringify({ channel: this.channel, message: this.message }) }));
    }
  }
};

const RatePanel = {
  mixins: [panelMixin],
  template: `
    <section class="card">
      <div class="row">
        <button :disabled="busy" @click="run(() => api('/api/ratelimit/fixed?name=demo&limit=5&window=10'))">固定窗口</button>
        <button :disabled="busy" @click="run(() => api('/api/ratelimit/slide?name=demo&limit=5&window=10'))">滑动窗口</button>
        <button :disabled="busy" @click="run(() => api('/api/ratelimit/token?name=demo&capacity=5&rate=1'))">令牌桶</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/ratelimit/protected'))">注解接口</button>
      </div>
      <p class="doc">连续点同一按钮。固定窗口和滑动窗口 10 秒内 5 次，令牌桶容量 5、每秒补充 1 个。注解接口按 IP 限制。</p>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `
};

const ExpirePanel = {
  mixins: [panelMixin],
  data() { return { key: "coupon", value: "once", ttlSeconds: 15 }; },
  template: `
    <section class="card">
      <div class="row">
        <input v-model="key">
        <input v-model="value">
        <input v-model.number="ttlSeconds" type="number">
        <button :disabled="busy" @click="put">写入并过期</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/expire/' + key))">查看 TTL</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/expire/' + key + '/persist', {method:'POST'}))">取消过期</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/expire/log'))">过期日志</button>
      </div>
      <p class="doc">键会自动加上 temp: 前缀。过期日志需要 Redis 配置 notify-keyspace-events Ex，docker-compose 里已经打开。</p>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  methods: {
    put() {
      return this.run(() => api("/api/expire", { method: "POST", body: JSON.stringify({ key: this.key, value: this.value, ttlSeconds: this.ttlSeconds }) }));
    }
  }
};

const PagePanel = {
  mixins: [panelMixin],
  data() { return { slug: "home", title: "Redis 规范示例站", content: "更新这段文字后，下面的页面会重新渲染。", frame: "/api/page/home" }; },
  template: `
    <section class="card">
      <iframe :src="frame"></iframe>
      <div class="row" style="margin-top:8px">
        <button class="ghost" :disabled="busy" @click="reload">重新加载</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/page/home/view'))">查看是否命中</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/page/home/cache', {method:'DELETE'}))">只删缓存</button>
      </div>
    </section>
    <section class="card">
      <input v-model="title" style="width:100%;margin-bottom:8px">
      <textarea v-model="content"></textarea>
      <button :disabled="busy" @click="save">保存正文</button>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  methods: {
    reload() {
      this.frame = "/api/page/" + this.slug + "?t=" + Date.now();
    },
    save() {
      return this.run(async () => {
        const data = await api("/api/page/" + this.slug, { method: "PUT", body: JSON.stringify({ title: this.title, content: this.content }) });
        this.reload();
        return data;
      });
    }
  }
};

const SocialPanel = {
  mixins: [panelMixin],
  data() { return { user: "ada", target: "lin", content: "刚看完缓存那一章" }; },
  template: `
    <section class="card">
      <div class="row">
        <button :disabled="busy" @click="run(() => api('/api/social/seed', {method:'POST'}))">填充关系</button>
        <input v-model="user">
        <input v-model="target">
        <button :disabled="busy" @click="follow(true)">关注</button>
        <button class="ghost" :disabled="busy" @click="follow(false)">取消</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/social/mutual?a=' + user + '&b=' + target))">共同关注</button>
      </div>
    </section>
    <section class="card">
      <textarea v-model="content"></textarea>
      <div class="row">
        <button :disabled="busy" @click="post">发动态</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/social/feed?user=' + user))">看时间线</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/social/online', {method:'POST', body: JSON.stringify({user: user})}))">上报在线</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/social/profile?user=' + user))">资料</button>
      </div>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  methods: {
    follow(value) {
      return this.run(() => api("/api/social/follow", { method: "POST", body: JSON.stringify({ user: this.user, target: this.target, follow: value }) }));
    },
    post() {
      return this.run(() => api("/api/social/posts", { method: "POST", body: JSON.stringify({ user: this.user, content: this.content }) }));
    }
  }
};

const RecommendPanel = {
  mixins: [panelMixin],
  data() { return { userId: "qiu", itemId: 2, action: "like" }; },
  template: `
    <section class="card">
      <div class="row">
        <button :disabled="busy" @click="run(() => api('/api/recommend/seed', {method:'POST'}))">写入示例行为</button>
        <input v-model="userId">
        <input v-model.number="itemId" type="number">
        <select v-model="action"><option>view</option><option>like</option><option>cart</option><option>buy</option></select>
        <button :disabled="busy" @click="record">记录行为</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/recommend?userId=' + userId))">推荐</button>
      </div>
      <p class="doc">示例会让 qiu 看过商品 1。推荐结果来自别人和商品 1 一起出现过的商品。</p>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  methods: {
    record() {
      return this.run(() => api("/api/recommend/behavior", { method: "POST", body: JSON.stringify({ userId: this.userId, itemId: this.itemId, action: this.action }) }));
    }
  }
};

const GeoPanel = {
  mixins: [panelMixin],
  data() { return { lng: 120.1551, lat: 30.2741, radiusKm: 200 }; },
  template: `
    <section class="card">
      <div class="row">
        <button :disabled="busy" @click="run(() => api('/api/geo/seed', {method:'POST'}))">写入门店</button>
        <input v-model.number="lng" type="number" step="0.0001">
        <input v-model.number="lat" type="number" step="0.0001">
        <input v-model.number="radiusKm" type="number">
        <button :disabled="busy" @click="nearby">附近门店</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/geo/distance?from=hz-wulin&to=sh-nanjing'))">杭沪距离</button>
      </div>
      <p class="doc">默认坐标是杭州。半径单位是公里。</p>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  methods: {
    nearby() {
      return this.run(() => api("/api/geo/nearby?lng=" + this.lng + "&lat=" + this.lat + "&radiusKm=" + this.radiusKm));
    }
  }
};

const SeriesPanel = {
  mixins: [panelMixin],
  data() { return { metric: "cpu", value: 0.2 }; },
  template: `
    <section class="card">
      <div class="row">
        <input v-model="metric">
        <input v-model.number="value" type="number" step="0.01">
        <button :disabled="busy" @click="add">写入采样</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/timeseries/' + metric))">原始点</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/timeseries/' + metric + '?bucketSeconds=60'))">按分钟平均</button>
      </div>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  methods: {
    add() {
      return this.run(() => api("/api/timeseries/" + this.metric, { method: "POST", body: JSON.stringify({ value: this.value, timestamp: 0 }) }));
    }
  }
};

const SchedulePanel = {
  mixins: [panelMixin],
  data() { return { type: "LOG", payload: "发送周报", delaySeconds: 5 }; },
  template: `
    <section class="card">
      <div class="row">
        <select v-model="type"><option>LOG</option><option>DELETE_KEY</option></select>
        <input v-model="payload" placeholder="LOG 的文本，或 temp: 开头的键">
        <input v-model.number="delaySeconds" type="number">
        <button :disabled="busy" @click="submit">提交</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/schedule/tasks'))">待执行</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/schedule/results'))">已完成</button>
      </div>
      <p class="doc">调度器大约每秒领取一次到期任务。DELETE_KEY 只能删除 temp: 前缀。</p>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  methods: {
    submit() {
      return this.run(() => api("/api/schedule/tasks", { method: "POST", body: JSON.stringify({ type: this.type, payload: this.payload, delaySeconds: this.delaySeconds }) }));
    }
  }
};

const SharePanel = {
  mixins: [panelMixin],
  data() { return { field: "feature.rank", value: "on", service: "order", instance: "order-1" }; },
  template: `
    <section class="card">
      <div class="row">
        <input v-model="field">
        <input v-model="value">
        <button :disabled="busy" @click="put">写配置</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/share/config'))">读配置</button>
      </div>
    </section>
    <section class="card">
      <div class="row">
        <input v-model="service">
        <input v-model="instance">
        <button :disabled="busy" @click="beat">心跳</button>
        <button class="ghost" :disabled="busy" @click="run(() => api('/api/share/services'))">在线实例</button>
      </div>
      <p class="doc">心跳 30 秒过期。停掉续期后，实例会从列表消失。</p>
    </section>
    <result-block :value="result" :error="error"></result-block>
  `,
  methods: {
    put() {
      return this.run(() => api("/api/share/config", { method: "POST", body: JSON.stringify({ field: this.field, value: this.value }) }));
    },
    beat() {
      return this.run(() => api("/api/share/heartbeat", { method: "POST", body: JSON.stringify({ service: this.service, instance: this.instance }) }));
    }
  }
};

const DurablePanel = {
  mixins: [panelMixin],
  template: `
    <section class="card">
      <button :disabled="busy" @click="load">读取持久化状态</button>
      <p class="doc">这里只读 INFO 和 CONFIG GET，不会改 Redis 的持久化配置。</p>
      <result-block :value="result" :error="error"></result-block>
    </section>
  `,
  mounted() { this.load(); },
  methods: {
    load() { return this.run(() => api("/api/durable")); }
  }
};

createApp({
  components: {
    structure: StructurePanel,
    cache: CachePanel,
    session: SessionPanel,
    rank: RankPanel,
    mq: MqPanel,
    analytics: AnalyticsPanel,
    lock: LockPanel,
    pubsub: PubSubPanel,
    ratelimit: RatePanel,
    expire: ExpirePanel,
    page: PagePanel,
    social: SocialPanel,
    recommend: RecommendPanel,
    geo: GeoPanel,
    timeseries: SeriesPanel,
    schedule: SchedulePanel,
    share: SharePanel,
    durable: DurablePanel
  },
  data() {
    return { catalog: [], current: "structure", health: {} };
  },
  computed: {
    active() {
      return this.catalog.find((item) => item.id === this.current) || { title: "Redis 规范", summary: "", doc: "README.md" };
    }
  },
  mounted() {
    this.load();
    this.timer = setInterval(this.loadHealth, 10000);
  },
  beforeUnmount() {
    clearInterval(this.timer);
  },
  methods: {
    async load() {
      try {
        this.catalog = await api("/api/catalog");
        await this.loadHealth();
      } catch (error) {
        this.catalog = [{ id: "structure", title: "无法加载目录", summary: error.message, doc: "README.md" }];
      }
    },
    async loadHealth() {
      try {
        this.health = await api("/api/health");
      } catch (error) {
        this.health = { redis: "DOWN", database: "DOWN" };
      }
    }
  }
}).component("result-block", ResultBlock).mount("#app");
