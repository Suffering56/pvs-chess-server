update game_features set session_id =
  (select session_id from game_features
    where game_id in (select max(id) from game) and session_id is not null)
    where game_id = 375 and session_id is not null;

update game set position = 46 where id = 375;
delete from history where game_id = 375;


INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120821, 0, 375, 4, 46, 0);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120822, 1, 375, 2, 46, 0);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120823, 3, 375, 6, 46, 0);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120824, 5, 375, 4, 46, 0);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120825, 0, 375, 1, 46, 1);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120826, 1, 375, 1, 46, 1);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120827, 2, 375, 1, 46, 1);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120828, 6, 375, 1, 46, 1);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120829, 7, 375, 1, 46, 1);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120830, 0, 375, 10, 46, 2);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120831, 6, 375, 7, 46, 3);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120832, 7, 375, 9, 46, 3);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120833, 2, 375, 7, 46, 4);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120834, 1, 375, 3, 46, 5);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120835, 3, 375, 7, 46, 5);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120836, 7, 375, 7, 46, 5);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120837, 1, 375, 12, 46, 6);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120838, 5, 375, 7, 46, 6);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120839, 4, 375, 11, 46, 7);
INSERT INTO public.history (id, column_index, game_id, piece_id, position, row_index) VALUES (120840, 6, 375, 10, 46, 7);
