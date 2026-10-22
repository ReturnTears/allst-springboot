# 13 地理位置

代码：`geo/GeoService`、`geo/GeoController`。

第 00 章看的是 GEO 命令。这一章把命令收成门店查询。

- 坐标在 GEO 键 `geo:shops`，成员是门店 id
- 名称、分类、经纬度原文在 Hash `geo:shop:{id}`

GEO 成员只放 id。把店名编码进成员会导致改名时还要删掉旧坐标。距离和坐标由 `GEORADIUS` 带回，详情再读 Hash。

半径用公里。结果按距离从近到远，最多 20 条。经度范围 -180 到 180，纬度 -90 到 90。

`GEODIST` 是球面距离，城市之间够用。室内定位、要走路网的场景不要用它。

## 接口

- `POST /api/geo/seed` 写入杭州、上海、北京、深圳、成都的示例门店
- `POST /api/geo/shops`，体：`{"id":"hz-wulin","name":"武林门店","category":"咖啡","lng":120.16,"lat":30.27}`
- `GET /api/geo/nearby?lng=120.1551&lat=30.2741&radiusKm=200`
- `GET /api/geo/distance?from=hz-wulin&to=sh-nanjing`

## 验证

写入门店后，以杭州为圆心、半径 200 公里，应看到西湖和武林两家，看不到上海。把半径放到 300 公里以上，南京路门店会出现。杭沪距离大约 160 公里。
