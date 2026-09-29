-- Gestione Turni: schema Supabase.
-- Da eseguire UNA volta: Supabase → SQL Editor → New query → incolla tutto → Run.
--
-- Modello: una "famiglia" condivide un calendario di turni. Chi crea la famiglia riceve un
-- codice invito; l'altra persona lo inserisce nell'app per unirsi. Le regole RLS fanno sì che
-- ognuno veda e modifichi solo i turni della propria famiglia.

-- ─── Tabelle ────────────────────────────────────────────────────────────────

create table public.famiglie (
  id            uuid primary key default gen_random_uuid(),
  codice_invito text not null unique default upper(substr(md5(gen_random_uuid()::text), 1, 8)),
  creata_il     timestamptz not null default now()
);

create table public.membri (
  famiglia_id uuid not null references public.famiglie (id) on delete cascade,
  user_id     uuid not null references auth.users (id) on delete cascade,
  primary key (famiglia_id, user_id)
);
-- Ogni utente appartiene a una sola famiglia.
create unique index membri_un_utente_una_famiglia on public.membri (user_id);

create table public.turni (
  id            uuid primary key,                -- generato dall'app, così si può creare offline
  famiglia_id   uuid not null references public.famiglie (id) on delete cascade,
  data          date not null,
  tipo          text not null,                   -- MATTINA, POMERIGGIO, NOTTE, STRAORDINARIO, RIPOSO, FERIE
  inizio        int  not null,                   -- minuti dalla mezzanotte
  fine          int  not null,
  note          text not null default '',
  modificato_il bigint not null,                 -- ms dal telefono: in caso di conflitto vince il più recente
  eliminato     boolean not null default false,  -- eliminazione "soft", per propagarla agli altri telefoni
  modificato_da uuid,
  updated_at    timestamptz not null default now() -- orologio del server: cursore della sincronizzazione
);
create index turni_per_sincronizzazione on public.turni (famiglia_id, updated_at);

-- ─── Trigger: timestamp del server e "vince la modifica più recente" ────────

create function public.turni_prima_di_scrivere() returns trigger
language plpgsql as $$
begin
  -- Un telefono rimasto offline non deve sovrascrivere una modifica più recente dell'altro.
  if tg_op = 'UPDATE' and new.modificato_il < old.modificato_il then
    return null;
  end if;
  new.updated_at := clock_timestamp();
  new.modificato_da := auth.uid();
  return new;
end $$;

create trigger turni_prima_di_scrivere
  before insert or update on public.turni
  for each row execute function public.turni_prima_di_scrivere();

-- ─── Sicurezza (RLS) ────────────────────────────────────────────────────────

alter table public.famiglie enable row level security;
alter table public.membri   enable row level security;
alter table public.turni    enable row level security;

-- security definer: legge "membri" senza passare dalle sue stesse policy (evita la ricorsione).
create function public.mia_famiglia() returns uuid
language sql stable security definer set search_path = public as $$
  select famiglia_id from public.membri where user_id = auth.uid()
$$;

create policy "vedo la mia famiglia" on public.famiglie
  for select to authenticated using (id = public.mia_famiglia());

create policy "vedo i membri della mia famiglia" on public.membri
  for select to authenticated using (famiglia_id = public.mia_famiglia());

create policy "gestisco i turni della mia famiglia" on public.turni
  for all to authenticated
  using (famiglia_id = public.mia_famiglia())
  with check (famiglia_id = public.mia_famiglia());

-- ─── Funzioni chiamate dall'app ─────────────────────────────────────────────

create function public.crea_famiglia() returns public.famiglie
language plpgsql security definer set search_path = public as $$
declare
  f public.famiglie;
begin
  if auth.uid() is null then
    raise exception 'Non autenticato';
  end if;
  if exists (select 1 from public.membri where user_id = auth.uid()) then
    raise exception 'Fai già parte di una famiglia';
  end if;
  insert into public.famiglie default values returning * into f;
  insert into public.membri (famiglia_id, user_id) values (f.id, auth.uid());
  return f;
end $$;

create function public.unisciti_famiglia(codice text) returns public.famiglie
language plpgsql security definer set search_path = public as $$
declare
  f public.famiglie;
begin
  if auth.uid() is null then
    raise exception 'Non autenticato';
  end if;
  if exists (select 1 from public.membri where user_id = auth.uid()) then
    raise exception 'Fai già parte di una famiglia';
  end if;
  select * into f from public.famiglie where codice_invito = upper(trim(codice));
  if not found then
    raise exception 'Codice non valido';
  end if;
  insert into public.membri (famiglia_id, user_id) values (f.id, auth.uid());
  return f;
end $$;

revoke execute on function public.crea_famiglia() from public, anon;
revoke execute on function public.unisciti_famiglia(text) from public, anon;
revoke execute on function public.mia_famiglia() from public, anon;
grant execute on function public.crea_famiglia() to authenticated;
grant execute on function public.unisciti_famiglia(text) to authenticated;
grant execute on function public.mia_famiglia() to authenticated;

-- ─── Realtime: avvisa l'altro telefono quando cambia un turno ──────────────

alter publication supabase_realtime add table public.turni;
