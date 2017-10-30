\c huepferverlag;

CREATE TABLE public.users
(
  id SERIAL,
  nickname text NOT NULL,
  service text NOT NULL,
  app_id text NOT NULL,
  user_id text NOT NULL,
  created timestamp without time zone DEFAULT now(),
  UNIQUE (app_id, user_id)
)
WITH (
  OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.users
OWNER to kangaroo;
