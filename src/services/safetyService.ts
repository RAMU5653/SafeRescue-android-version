import { LocationPoint, SafetyPlace, WeatherCondition } from '../types';
import { supabase } from '../lib/supabase';

function haversineMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371e3;
  const phi1 = (lat1 * Math.PI) / 180;
  const phi2 = (lat2 * Math.PI) / 180;
  const deltaPhi = ((lat2 - lat1) * Math.PI) / 180;
  const deltaLambda = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
    Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return Math.round(R * c);
}

const WEATHER_CODE_MAP: Record<number, string> = {
  0: 'Clear sky',
  1: 'Mainly clear',
  2: 'Partly cloudy',
  3: 'Overcast',
  45: 'Fog',
  48: 'Depositing rime fog',
  51: 'Light drizzle',
  53: 'Moderate drizzle',
  55: 'Dense drizzle',
  61: 'Slight rain',
  63: 'Moderate rain',
  65: 'Heavy rain',
  71: 'Slight snow fall',
  73: 'Moderate snow fall',
  75: 'Heavy snow fall',
  80: 'Slight rain showers',
  81: 'Moderate rain showers',
  82: 'Violent rain showers',
  95: 'Thunderstorm',
  96: 'Thunderstorm with slight hail',
  99: 'Thunderstorm with heavy hail',
};

interface GooglePlaceResult {
  id: string;
  name: string;
  category: string;
  latitude: number;
  longitude: number;
  address?: string;
  distanceMeters?: number;
  rating?: number;
  openNow?: boolean;
}

export async function fetchNearbySafePlaces(location: LocationPoint, radiusMeters: number = 3000): Promise<SafetyPlace[]> {
  try {
    const { data, error } = await supabase.functions.invoke('google-places', {
      body: {
        latitude: location.latitude,
        longitude: location.longitude,
        radius: radiusMeters,
      },
    });

    if (error) {
      console.warn('Google Places edge function error:', error.message);
      throw error;
    }

    if (!data || !data.places || !Array.isArray(data.places)) {
      throw new Error('Invalid response from Places API');
    }

    const results: SafetyPlace[] = (data.places as GooglePlaceResult[]).map((place) => ({
      id: place.id,
      name: place.name,
      category: place.category as SafetyPlace['category'],
      latitude: place.latitude,
      longitude: place.longitude,
      distanceMeters: place.distanceMeters ?? haversineMeters(location.latitude, location.longitude, place.latitude, place.longitude),
      address: place.address,
    }));

    return results.sort((a, b) => (a.distanceMeters ?? 0) - (b.distanceMeters ?? 0)).slice(0, 15);
  } catch (err) {
    console.warn('Google Places fetch failed:', err);
    return [];
  }
}

export async function fetchCurrentWeather(location: LocationPoint): Promise<WeatherCondition> {
  try {
    const url = `https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}&current=temperature_2m,relative_humidity_2m,is_day,weather_code,wind_speed_10m&timezone=auto`;
    const res = await fetch(url);
    if (!res.ok) throw new Error(`Weather API error: ${res.status}`);
    const data = await res.json();
    const current = data.current;

    const weatherCode = current.weather_code ?? 0;
    return {
      temperatureC: Math.round(current.temperature_2m ?? 20),
      weatherCode,
      weatherDescription: WEATHER_CODE_MAP[weatherCode] || 'Clear',
      windSpeedKmh: Math.round(current.wind_speed_10m ?? 8),
      humidityPercent: Math.round(current.relative_humidity_2m ?? 65),
      isDay: Boolean(current.is_day),
    };
  } catch (err) {
    console.warn('Open-Meteo fetch failed:', err);
    return {
      temperatureC: 22,
      weatherCode: 1,
      weatherDescription: 'Mainly Clear',
      windSpeedKmh: 12,
      humidityPercent: 55,
      isDay: true,
    };
  }
}
