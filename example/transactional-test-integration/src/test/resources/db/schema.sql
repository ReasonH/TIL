-- create table category (
--                           id bigint generated by default as identity,
--                           title varchar(255),
--                           primary key (id)
-- );
-- create table comment (
--                          id bigint generated by default as identity,
--                          author varchar(255),
--                          content varchar(255),
--                          post_id bigint,
--                          primary key (id)
-- );
-- create table post (
--                       id bigint generated by default as identity,
--                       content varchar(255),
--                       title varchar(255),
--                       category_id bigint,
--                       sector_id bigint,
--                       user_id bigint,
--                       primary key (id)
-- );
-- create table post_for_lazy (
--                                id bigint generated by default as identity,
--                                content varchar(255),
--                                title varchar(255),
--                                sector_id bigint,
--                                user_id bigint,
--                                primary key (id)
-- );
-- create table sector (
--                         id bigint generated by default as identity,
--                         name varchar(255),
--                         primary key (id)
-- );
-- create table user (
--                       id bigint generated by default as identity,
--                       name varchar(255),
--                       primary key (id)
-- );
--
-- alter table post
--     add constraint post_uindex_content unique (content);
--
-- alter table post_for_lazy
--     add constraint post_for_lazy_uindex_content unique (content);
--
-- alter table comment
--     add constraint comment_fk_post
--         foreign key (post_id)
--             references post;
--
-- alter table post
--     add constraint post_fk_category
--         foreign key (category_id)
--             references category;
--
-- alter table post
--     add constraint post_fk_sector
--         foreign key (sector_id)
--             references sector;
--
-- alter table post
--     add constraint post_fk_user
--         foreign key (user_id)
--             references user;
--
-- alter table post_for_lazy
--     add constraint post_for_lazy_fk_sector
--         foreign key (sector_id)
--             references sector;
--
-- alter table post_for_lazy
--     add constraint post_for_lazy_fk_user
--         foreign key (user_id)
--             references user;