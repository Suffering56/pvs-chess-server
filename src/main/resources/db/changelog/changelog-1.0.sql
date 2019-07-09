--liquibase formatted sql

--changeset Magic:1
create table if not exists game
(
	id bigint not null
		constraint game_pkey
			primary key,
	mode varchar(255),
	position integer default 0 not null
);
alter table game owner to postgres;
create sequence game_id_seq;
alter sequence game_id_seq owner to postgres;

create table if not exists game_features
(
	id bigint not null
		constraint game_features_pkey
			primary key,
	is_under_check boolean default false not null,
	last_visit_date timestamp,
	long_castling_available boolean default true not null,
	pawn_long_move_column_index integer,
	session_id varchar(255),
	short_castling_available boolean default true not null,
	side varchar(255) not null,
	game_id bigint not null
		constraint fk63xltct60scimpm06k8bhbe4a
			references game
);
alter table game_features owner to postgres;
create sequence game_features_id_seq;
alter sequence game_features_id_seq owner to postgres;

create table if not exists history
(
	id bigint not null
		constraint history_pkey
			primary key,
	column_index_from integer not null,
	column_index_to integer not null,
	description varchar(255),
	game_id bigint not null,
	piece_from_pawn integer,
	position integer not null,
	row_index_from integer not null,
	row_index_to integer not null
);
alter table history owner to postgres;
create sequence history_id_seq;
alter sequence history_id_seq owner to postgres;