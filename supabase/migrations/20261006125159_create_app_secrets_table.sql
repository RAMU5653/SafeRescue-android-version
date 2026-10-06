/*
# Create app_secrets table for storing third-party API keys

## Overview
Creates a table to store sensitive API keys (like Google Maps API key) that edge functions need to access.
This avoids requiring manual environment variable configuration.

## New Tables
### app_secrets
- `id` (uuid, PK)
- `name` (text, unique) — name of the secret, e.g. "GOOGLE_MAPS_API_KEY"
- `value` (text) — the secret value
- `created_at` (timestamptz)
- `updated_at` (timestamptz)

## Security
- RLS enabled.
- No SELECT/INSERT/UPDATE/DELETE policies for anon or authenticated roles — the table is locked down.
- Only the service role key (used by edge functions) can read/write, since it bypasses RLS.
*/

CREATE TABLE IF NOT EXISTS app_secrets (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text UNIQUE NOT NULL,
  value text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE app_secrets ENABLE ROW LEVEL SECURITY;
