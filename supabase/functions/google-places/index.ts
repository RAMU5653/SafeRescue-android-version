import { createClient } from "npm:@supabase/supabase-js@2.45.4";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Client-Info, Apikey",
};

interface NearbySearchRequest {
  latitude: number;
  longitude: number;
  radius?: number;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 200, headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

    if (!supabaseUrl || !supabaseServiceKey) {
      return new Response(
        JSON.stringify({ error: "Server configuration error" }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // Fetch the Google Maps API key from Supabase secrets
    const { data: secretData, error: secretError } = await supabase
      .from("app_secrets")
      .select("value")
      .eq("name", "GOOGLE_MAPS_API_KEY")
      .maybeSingle();

    let apiKey: string | null = secretData?.value ?? null;

    // Fall back to environment variable if not in secrets table
    if (!apiKey) {
      apiKey = Deno.env.get("GOOGLE_MAPS_API_KEY") ?? null;
    }

    if (!apiKey) {
      return new Response(
        JSON.stringify({ error: "Google Maps API key not configured. Add GOOGLE_MAPS_API_KEY as a secret." }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    let body: NearbySearchRequest;
    try {
      body = await req.json();
    } catch {
      return new Response(
        JSON.stringify({ error: "Invalid JSON body" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const { latitude, longitude, radius = 3000 } = body;

    if (typeof latitude !== "number" || typeof longitude !== "number") {
      return new Response(
        JSON.stringify({ error: "latitude and longitude are required numbers" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Google Places API Nearby Search - fetch multiple place types
    const placeTypes = [
      { type: "police", category: "police" },
      { type: "hospital", category: "hospital" },
      { type: "fire_station", category: "fire_station" },
      { type: "pharmacy", category: "public_safe" },
      { type: "community_center", category: "public_safe" },
    ];

    const allResults: Array<{
      id: string;
      name: string;
      category: string;
      latitude: number;
      longitude: number;
      address?: string;
      distanceMeters?: number;
      rating?: number;
      openNow?: boolean;
    }> = [];

    // Google Places Nearby Search max radius is 50000m, but we use 3000m default
    const searchRadius = Math.min(Math.max(radius, 500), 50000);

    for (const { type, category } of placeTypes) {
      try {
        const url = `https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=${latitude},${longitude}&radius=${searchRadius}&type=${type}&key=${apiKey}`;
        const res = await fetch(url);
        if (!res.ok) continue;
        const data = await res.json();

        if (data.results && Array.isArray(data.results)) {
          for (const place of data.results) {
            const lat = place.geometry?.location?.lat;
            const lng = place.geometry?.location?.lng;
            if (typeof lat !== "number" || typeof lng !== "number") continue;

            // Calculate distance via haversine
            const R = 6371e3;
            const phi1 = (latitude * Math.PI) / 180;
            const phi2 = (lat * Math.PI) / 180;
            const dPhi = ((lat - latitude) * Math.PI) / 180;
            const dLambda = ((lng - longitude) * Math.PI) / 180;
            const a = Math.sin(dPhi / 2) ** 2 + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) ** 2;
            const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            const distanceMeters = Math.round(R * c);

            allResults.push({
              id: place.place_id || `place-${allResults.length}`,
              name: place.name || `${type} place`,
              category,
              latitude: lat,
              longitude: lng,
              address: place.vicinity || place.formatted_address || undefined,
              distanceMeters,
              rating: place.rating,
              openNow: place.opening_hours?.open_now,
            });
          }
        }
      } catch (err) {
        console.warn(`Failed to fetch ${type}:`, err);
      }
    }

    // Sort by distance and deduplicate by id
    const seen = new Set<string>();
    const uniqueResults = allResults
      .filter((r) => {
        if (seen.has(r.id)) return false;
        seen.add(r.id);
        return true;
      })
      .sort((a, b) => (a.distanceMeters ?? 0) - (b.distanceMeters ?? 0))
      .slice(0, 15);

    return new Response(
      JSON.stringify({ places: uniqueResults }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ error: err.message || "Internal server error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
