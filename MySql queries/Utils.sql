-- gonnected Users
SELECT SUBSTRING_INDEX(host, ':', 1) AS host_short,
       GROUP_CONCAT(DISTINCT user) AS users,
       COUNT(*) AS threads
FROM information_schema.processlist
GROUP BY host_short
ORDER BY COUNT(*), host_short;
-- get charset
SELECT default_character_set_name FROM information_schema.SCHEMATA 
WHERE schema_name = "main";


delete from user where id != 9999;
insert into user(name,username,password_hash) values('yee','yeet','dwefiogr');
insert into user(name,username,password_hash) values('yee','yeeeet','dwefiogr');
insert into user(name,username,password_hash) values('yee','dorin','dwefiogr');
insert into user(name,username,password_hash,photo_path) values('yee','dodsrin','dwefiogr','gae');
insert into user(name,username,password_hash,photo_path) values('','dsar','5j7irtsx93n2jojxywz09zxecwctwrdqrzkvb8oo2w7drxzmup','');
insert into channel(creator_id,name,description,password_hash) values(4,'its free real estate','hmmmm','dsa');
select count(id) from user;
select id from user where username = 'yeeeet';
insert into channel_user() values(1,2);

select id,creator_id,name,description,(
	select count(*) from channel_user where channel_id = id and user_id = 2
) as is_part_of_channel
 from channel;

select * from channel;
select * from user;
insert into channel(creator_ir,name,description,password_hash) values();
 select count(id) from channel where id = 1 and creator_id = 4;

