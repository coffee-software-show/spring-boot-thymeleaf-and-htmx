create table if not exists todo
(
    id    serial primary key,
    title text not null
);