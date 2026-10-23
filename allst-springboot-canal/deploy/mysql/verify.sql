USE demo;

INSERT INTO t_product (id, name, price, stock) VALUES (1001, 'Canal入门', 39.00, 20);
UPDATE t_product SET name = 'Canal入门（修订）', stock = 18, price = 42.50 WHERE id = 1001;

-- 确认三个下游都有 id=1001 之后，再执行删除，观察 key / 文档是否消失。
-- DELETE FROM t_product WHERE id = 1001;
