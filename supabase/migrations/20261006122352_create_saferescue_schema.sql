/*
# SafeRescue: Core Database Schema

## Overview
Creates the tables needed to make SafeRescue a fully functional app with real data persistence.
Replaces all localStorage/in-memory fake data with Supabase database tables.

## New Tables

### 1. profiles
- Stores extended user profile data beyond what auth.users holds.
- `id` (uuid, PK, references auth.users) — one-to-one with Supabase Auth users.
- `name` (text) — user's full display name.
- `username` (text, unique) — username for login display.
- `phone` (text) — phone number.
- `role` (text, default 'user') — role label (user/admin).
- `created_at` (timestamptz).

### 2. trusted_contacts
- Stores up to 3 emergency contacts per user.
- `id` (uuid, PK).
- `user_id` (uuid, FK → auth.users, default auth.uid()).
- `name` (text) — contact name.
- `phone` (text) — contact phone number.
- `relationship` (text) — relationship description.
- `verified` (boolean, default false).
- `created_at` (timestamptz).

### 3. timeline_events
- Stores emergency timeline events (SOS triggered, cancelled, evidence captured, etc.).
- `id` (uuid, PK).
- `user_id` (uuid, FK → auth.users, default auth.uid()).
- `event_type` (text) — enum-like: EMERGENCY_TRIGGERED, EMERGENCY_CANCELLED, etc.
- `title` (text) — short title.
- `detail` (text) — longer description.
- `risk_score` (int) — optional risk score at time of event.
- `timestamp_millis` (bigint) — millisecond timestamp from client.
- `created_at` (timestamptz).

### 4. emergencies
- Records emergency incidents (SOS activations and their outcomes).
- `id` (uuid, PK).
- `user_id` (uuid, FK → auth.users, default auth.uid()).
- `state` (text) — emergency state: EMERGENCY_ACTIVE, CANCELLED, COMPLETED, etc.
- `risk_score` (int) — final risk score.
- `risk_severity` (text) — LOW/MODERATE/HIGH/CRITICAL.
- `sms_report` (text) — the SMS message that was dispatched.
- `latitude` (float8) — GPS latitude at time of incident.
- `longitude` (float8) — GPS longitude.
- `evidence_count` (int) — number of evidence photos captured.
- `started_at` (timestamptz) — when SOS was triggered.
- `ended_at` (timestamptz) — when emergency ended.
- `created_at` (timestamptz).

## Security
- RLS enabled on all tables.
- All tables are owner-scoped: each authenticated user can only CRUD their own rows.
- `user_id` columns default to `auth.uid()` so inserts that omit it still satisfy RLS.
- Four separate policies (SELECT/INSERT/UPDATE/DELETE) per table.
*/

-- Profiles table
CREATE TABLE IF NOT EXISTS profiles (
  id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  name text NOT NULL DEFAULT '',
  username text UNIQUE,
  phone text DEFAULT '',
  role text NOT NULL DEFAULT 'user',
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "select_own_profile" ON profiles;
CREATE POLICY "select_own_profile" ON profiles FOR SELECT
TO authenticated USING (auth.uid() = id);

DROP POLICY IF EXISTS "insert_own_profile" ON profiles;
CREATE POLICY "insert_own_profile" ON profiles FOR INSERT
TO authenticated WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "update_own_profile" ON profiles;
CREATE POLICY "update_own_profile" ON profiles FOR UPDATE
TO authenticated USING (auth.uid() = id) WITH CHECK (auth.uid() = id);

-- Trusted contacts table
CREATE TABLE IF NOT EXISTS trusted_contacts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
  name text NOT NULL,
  phone text NOT NULL,
  relationship text DEFAULT '',
  verified boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE trusted_contacts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "select_own_contacts" ON trusted_contacts;
CREATE POLICY "select_own_contacts" ON trusted_contacts FOR SELECT
TO authenticated USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "insert_own_contacts" ON trusted_contacts;
CREATE POLICY "insert_own_contacts" ON trusted_contacts FOR INSERT
TO authenticated WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "update_own_contacts" ON trusted_contacts;
CREATE POLICY "update_own_contacts" ON trusted_contacts FOR UPDATE
TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "delete_own_contacts" ON trusted_contacts;
CREATE POLICY "delete_own_contacts" ON trusted_contacts FOR DELETE
TO authenticated USING (auth.uid() = user_id);

-- Timeline events table
CREATE TABLE IF NOT EXISTS timeline_events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
  event_type text NOT NULL,
  title text NOT NULL,
  detail text NOT NULL DEFAULT '',
  risk_score int,
  timestamp_millis bigint NOT NULL DEFAULT extract(epoch from now()) * 1000,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE timeline_events ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "select_own_timeline" ON timeline_events;
CREATE POLICY "select_own_timeline" ON timeline_events FOR SELECT
TO authenticated USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "insert_own_timeline" ON timeline_events;
CREATE POLICY "insert_own_timeline" ON timeline_events FOR INSERT
TO authenticated WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "update_own_timeline" ON timeline_events;
CREATE POLICY "update_own_timeline" ON timeline_events FOR UPDATE
TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "delete_own_timeline" ON timeline_events;
CREATE POLICY "delete_own_timeline" ON timeline_events FOR DELETE
TO authenticated USING (auth.uid() = user_id);

-- Emergencies table
CREATE TABLE IF NOT EXISTS emergencies (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
  state text NOT NULL DEFAULT 'EMERGENCY_ACTIVE',
  risk_score int DEFAULT 0,
  risk_severity text DEFAULT 'LOW',
  sms_report text,
  latitude float8,
  longitude float8,
  evidence_count int DEFAULT 0,
  started_at timestamptz NOT NULL DEFAULT now(),
  ended_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE emergencies ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "select_own_emergencies" ON emergencies;
CREATE POLICY "select_own_emergencies" ON emergencies FOR SELECT
TO authenticated USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "insert_own_emergencies" ON emergencies;
CREATE POLICY "insert_own_emergencies" ON emergencies FOR INSERT
TO authenticated WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "update_own_emergencies" ON emergencies;
CREATE POLICY "update_own_emergencies" ON emergencies FOR UPDATE
TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "delete_own_emergencies" ON emergencies;
CREATE POLICY "delete_own_emergencies" ON emergencies FOR DELETE
TO authenticated USING (auth.uid() = user_id);

-- Indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_trusted_contacts_user_id ON trusted_contacts(user_id);
CREATE INDEX IF NOT EXISTS idx_timeline_events_user_id ON timeline_events(user_id);
CREATE INDEX IF NOT EXISTS idx_timeline_events_created_at ON timeline_events(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_emergencies_user_id ON emergencies(user_id);
CREATE INDEX IF NOT EXISTS idx_emergencies_started_at ON emergencies(started_at DESC);
