-- select * from orders
-- inner join clients c on c.id = orders.client_id
-- inner join order_lines ol on orders.id = ol.order_id
-- inner join articles a on a.id = ol.article_id;
--
-- insert into orders (client_id)
-- values (1)

-- für orm Aufgabe;

select o1_0.id,o1_0.client_id,o1_0.created_at from orders o1_0;
select nextval('order_lines_id_seq');

alter sequence order_lines_id_seq restart with 5;