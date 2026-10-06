import React, { useEffect, useRef, useState } from 'react';
import { LocationPoint, SafetyPlace, UnsafeZone } from '../types';
import { Shield, AlertTriangle, Navigation, Compass, ExternalLink } from 'lucide-react';
import { loadGoogleMaps } from '../lib/googleMaps';

interface InteractiveSafeMapProps {
  location: LocationPoint | null;
  safePlaces: SafetyPlace[];
  unsafeZones: UnsafeZone[];
  recommendedEscapeRoute?: {
    destinationName: string;
    distanceMeters: number;
    estimatedWalkMinutes: number;
    guidanceStep: string;
  } | null;
}

interface SelectedEntity {
  type: 'user' | 'safe' | 'unsafe';
  title: string;
  detail: string;
  distance?: number;
  category?: string;
  lat: number;
  lng: number;
}

const CATEGORY_COLORS: Record<string, string> = {
  police: '#2563EB',
  hospital: '#DC2626',
  fire_station: '#F59E0B',
  public_safe: '#10B981',
};

export const InteractiveSafeMap: React.FC<InteractiveSafeMapProps> = ({
  location,
  safePlaces,
  unsafeZones,
  recommendedEscapeRoute,
}) => {
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<google.maps.Map | null>(null);
  const markersRef = useRef<google.maps.Marker[]>([]);
  const circlesRef = useRef<google.maps.Circle[]>([]);
  const userMarkerRef = useRef<google.maps.Marker | null>(null);
  const userCircleRef = useRef<google.maps.Circle | null>(null);
  const routeLineRef = useRef<google.maps.Polyline | null>(null);
  const apiKeyRef = useRef<string>('');

  const [selectedEntity, setSelectedEntity] = useState<SelectedEntity | null>(null);
  const [filterMode, setFilterMode] = useState<'all' | 'safe' | 'unsafe'>('all');
  const [mapReady, setMapReady] = useState(false);
  const [mapError, setMapError] = useState<string | null>(null);

  const defaultLat = location?.latitude || 37.7749;
  const defaultLng = location?.longitude || -122.4194;

  // Initialize Google Map
  useEffect(() => {
    if (!mapContainerRef.current) return;

    const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || '';
    apiKeyRef.current = apiKey;

    if (!apiKey) {
      setMapError('Google Maps API key is not configured. Set VITE_GOOGLE_MAPS_API_KEY in your environment.');
      return;
    }

    let cancelled = false;

    loadGoogleMaps(apiKey)
      .then(() => {
        if (cancelled || !mapContainerRef.current) return;
        if (!window.google?.maps) {
          setMapError('Google Maps failed to initialize.');
          return;
        }

        const map = new window.google.maps.Map(mapContainerRef.current, {
          center: { lat: defaultLat, lng: defaultLng },
          zoom: 15,
          zoomControl: true,
          zoomControlOptions: {
            position: window.google.maps.ControlPosition.RIGHT_BOTTOM,
          },
          streetViewControl: false,
          mapTypeControl: false,
          fullscreenControl: false,
          styles: [
            { elementType: 'geometry', stylers: [{ color: '#0d1117' }] },
            { elementType: 'labels.text.stroke', stylers: [{ color: '#0d1117' }] },
            { elementType: 'labels.text.fill', stylers: [{ color: '#8E9BB6' }] },
            { featureType: 'road', elementType: 'geometry', stylers: [{ color: '#1a2235' }] },
            { featureType: 'road', elementType: 'labels.text.fill', stylers: [{ color: '#6B7B94' }] },
            { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#0a1428' }] },
            { featureType: 'landscape', elementType: 'geometry', stylers: [{ color: '#101828' }] },
            { featureType: 'poi', elementType: 'geometry', stylers: [{ color: '#141d30' }] },
            { featureType: 'transit', elementType: 'geometry', stylers: [{ color: '#141d30' }] },
            { featureType: 'administrative', elementType: 'geometry', stylers: [{ color: '#1a2235' }] },
          ],
        });

        mapRef.current = map;
        setMapReady(true);
      })
      .catch((err) => {
        setMapError(err.message || 'Failed to load Google Maps.');
      });

    return () => {
      cancelled = true;
      markersRef.current.forEach((m) => m.setMap(null));
      markersRef.current = [];
      circlesRef.current.forEach((c) => c.setMap(null));
      circlesRef.current = [];
      if (userMarkerRef.current) {
        userMarkerRef.current.setMap(null);
        userMarkerRef.current = null;
      }
      if (userCircleRef.current) {
        userCircleRef.current.setMap(null);
        userCircleRef.current = null;
      }
      if (routeLineRef.current) {
        routeLineRef.current.setMap(null);
        routeLineRef.current = null;
      }
      mapRef.current = null;
      setMapReady(false);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Update markers when location/places/zones/filter change
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !mapReady || !window.google?.maps) return;

    const curLat = location?.latitude || defaultLat;
    const curLng = location?.longitude || defaultLng;
    const gmaps = window.google.maps;

    // Clear previous markers and circles
    markersRef.current.forEach((m) => m.setMap(null));
    markersRef.current = [];
    circlesRef.current.forEach((c) => c.setMap(null));
    circlesRef.current = [];
    if (routeLineRef.current) {
      routeLineRef.current.setMap(null);
      routeLineRef.current = null;
    }

    // 1. User position marker
    if (userMarkerRef.current) userMarkerRef.current.setMap(null);
    if (userCircleRef.current) userCircleRef.current.setMap(null);

    userMarkerRef.current = new gmaps.Marker({
      position: { lat: curLat, lng: curLng },
      map,
      title: 'Your Location',
      icon: {
        path: gmaps.SymbolPath.CIRCLE,
        scale: 8,
        fillColor: '#3B82F6',
        fillOpacity: 1,
        strokeColor: '#FFFFFF',
        strokeWeight: 3,
      },
    });

    userCircleRef.current = new gmaps.Circle({
      center: { lat: curLat, lng: curLng },
      radius: location?.accuracyMeters ? Math.max(15, location.accuracyMeters) : 25,
      map,
      fillColor: '#60A5FA',
      fillOpacity: 0.12,
      strokeColor: '#3B82F6',
      strokeOpacity: 0.5,
      strokeWeight: 1.5,
    });

    userMarkerRef.current.addListener('click', () => {
      setSelectedEntity({
        type: 'user',
        title: 'You Are Here (Live Fix)',
        detail: `Coordinates: ${curLat.toFixed(5)}, ${curLng.toFixed(5)} (Accuracy ±${Math.round(location?.accuracyMeters || 10)}m)`,
        lat: curLat,
        lng: curLng,
      });
    });

    // 2. Unsafe zones
    if (filterMode === 'all' || filterMode === 'unsafe') {
      unsafeZones.forEach((zone) => {
        const isCritical = zone.riskSeverity === 'CRITICAL';
        const color = isCritical ? '#EF4444' : '#F59E0B';

        const circle = new gmaps.Circle({
          center: { lat: zone.latitude, lng: zone.longitude },
          radius: zone.radiusMeters,
          map,
          fillColor: color,
          fillOpacity: 0.18,
          strokeColor: color,
          strokeOpacity: 0.8,
          strokeWeight: 2,
        });

        circle.addListener('click', () => {
          setSelectedEntity({
            type: 'unsafe',
            title: zone.name,
            detail: zone.reason,
            category: zone.riskSeverity,
            lat: zone.latitude,
            lng: zone.longitude,
          });
        });

        circlesRef.current.push(circle);

        const centerMarker = new gmaps.Marker({
          position: { lat: zone.latitude, lng: zone.longitude },
          map,
          title: zone.name,
          icon: {
            path: gmaps.SymbolPath.CIRCLE,
            scale: 5,
            fillColor: color,
            fillOpacity: 1,
            strokeColor: '#FFFFFF',
            strokeWeight: 2,
          },
        });

        centerMarker.addListener('click', () => {
          setSelectedEntity({
            type: 'unsafe',
            title: zone.name,
            detail: zone.reason,
            category: zone.riskSeverity,
            lat: zone.latitude,
            lng: zone.longitude,
          });
        });

        markersRef.current.push(centerMarker);
      });
    }

    // 3. Safe places
    if (filterMode === 'all' || filterMode === 'safe') {
      safePlaces.forEach((place) => {
        const color = CATEGORY_COLORS[place.category] || '#10B981';

        const marker = new gmaps.Marker({
          position: { lat: place.latitude, lng: place.longitude },
          map,
          title: place.name,
          icon: {
            path: gmaps.SymbolPath.CIRCLE,
            scale: 7,
            fillColor: color,
            fillOpacity: 0.9,
            strokeColor: '#FFFFFF',
            strokeWeight: 2,
          },
        });

        marker.addListener('click', () => {
          setSelectedEntity({
            type: 'safe',
            title: place.name,
            detail: place.address || `${place.category.replace('_', ' ').toUpperCase()} Safe Haven`,
            distance: place.distanceMeters,
            category: place.category,
            lat: place.latitude,
            lng: place.longitude,
          });
        });

        markersRef.current.push(marker);
      });
    }

    // 4. Escape route line
    if (recommendedEscapeRoute && safePlaces.length > 0) {
      const target = safePlaces[0];
      routeLineRef.current = new gmaps.Polyline({
        path: [
          { lat: curLat, lng: curLng },
          { lat: target.latitude, lng: target.longitude },
        ],
        map,
        geodesic: true,
        strokeColor: '#10B981',
        strokeOpacity: 0.85,
        strokeWeight: 4,
        icons: [{
          icon: { path: gmaps.SymbolPath.CIRCLE, scale: 3 },
          offset: '0',
          repeat: '12px',
        }],
      });
    }
  }, [location, safePlaces, unsafeZones, filterMode, mapReady, defaultLat, defaultLng, recommendedEscapeRoute]);

  const handleRecenter = () => {
    if (mapRef.current) {
      const curLat = location?.latitude || defaultLat;
      const curLng = location?.longitude || defaultLng;
      mapRef.current.panTo({ lat: curLat, lng: curLng });
      mapRef.current.setZoom(15);
    }
  };

  return (
    <div id="interactive-safe-map-container" className="rounded-[24px] bg-[#121124] border border-white/10 p-4 space-y-3.5 shadow-lg">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-[#5B4BDB]/20 text-[#A594FD] flex items-center justify-center">
            <Compass className="w-5 h-5" />
          </div>
          <div>
            <h4 className="text-sm font-black text-white uppercase tracking-wider flex items-center gap-2">
              Safe Havens & Danger Zones
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-[#5B4BDB]/30 text-[#D4CDFF] font-bold">
                Google Maps
              </span>
            </h4>
            <p className="text-[11px] text-[#A6A2BC]">Real-time spatial risk & safe navigation escape corridors</p>
          </div>
        </div>

        <button
          type="button"
          onClick={handleRecenter}
          className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-[#C8BFFF] transition-colors"
          title="Recenter GPS"
        >
          <Navigation className="w-4 h-4" />
        </button>
      </div>

      <div className="flex items-center gap-1.5 p-1 rounded-xl bg-black/30 border border-white/5 text-[11px] font-bold">
        <button
          type="button"
          onClick={() => setFilterMode('all')}
          className={`flex-1 py-1.5 rounded-lg transition-colors ${
            filterMode === 'all' ? 'bg-[#5B4BDB] text-white' : 'text-[#A6A2BC] hover:text-white'
          }`}
        >
          All ({safePlaces.length + unsafeZones.length})
        </button>
        <button
          type="button"
          onClick={() => setFilterMode('safe')}
          className={`flex-1 py-1.5 rounded-lg flex items-center justify-center gap-1 transition-colors ${
            filterMode === 'safe' ? 'bg-[#10B981] text-white' : 'text-[#A6A2BC] hover:text-[#34D399]'
          }`}
        >
          <Shield className="w-3 h-3" />
          Safe ({safePlaces.length})
        </button>
        <button
          type="button"
          onClick={() => setFilterMode('unsafe')}
          className={`flex-1 py-1.5 rounded-lg flex items-center justify-center gap-1 transition-colors ${
            filterMode === 'unsafe' ? 'bg-[#EF4444] text-white' : 'text-[#A6A2BC] hover:text-[#F87171]'
          }`}
        >
          <AlertTriangle className="w-3 h-3" />
          Unsafe ({unsafeZones.length})
        </button>
      </div>

      <div className="relative rounded-[20px] overflow-hidden border border-white/10 h-[280px] w-full bg-zinc-950">
        <div ref={mapContainerRef} className="w-full h-full" />

        {mapError && (
          <div className="absolute inset-0 flex flex-col items-center justify-center p-4 text-center bg-[#0d1117]">
            <AlertTriangle className="w-8 h-8 text-amber-500 mb-2" />
            <p className="text-xs text-[#A6A2BC] leading-relaxed">{mapError}</p>
          </div>
        )}

        {!mapError && !mapReady && (
          <div className="absolute inset-0 flex items-center justify-center bg-[#0d1117]">
            <div className="w-6 h-6 border-2 border-[#5B4BDB] border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {mapReady && (
          <div className="absolute bottom-2 left-2 z-10 bg-black/80 backdrop-blur-md px-2.5 py-1.5 rounded-lg border border-white/10 text-[10px] space-y-1 pointer-events-none">
            <div className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-[#3B82F6] inline-block border border-white" />
              <span className="text-white font-medium">Your Live Position</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-[#10B981] inline-block" />
              <span className="text-[#6EE7B7]">Safe Havens (Police/Hospital)</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-[#EF4444] inline-block border border-dashed border-white" />
              <span className="text-[#FCA5A5]">Unsafe / Risk Zones</span>
            </div>
          </div>
        )}
      </div>

      {recommendedEscapeRoute && (
        <div className="p-3 rounded-2xl bg-[#0F261E] border border-[#10B981]/30 flex items-start gap-2.5 text-xs text-[#D1FAE5]">
          <Shield className="w-4 h-4 text-[#34D399] shrink-0 mt-0.5" />
          <div className="space-y-0.5">
            <span className="font-extrabold text-[#34D399] uppercase tracking-wider block text-[10px]">
              Recommended Safe Route
            </span>
            <p className="leading-snug">{recommendedEscapeRoute.guidanceStep}</p>
          </div>
        </div>
      )}

      {selectedEntity && (
        <div className="p-3 rounded-2xl bg-[#1B1A33] border border-white/10 flex items-center justify-between text-xs animate-in fade-in">
          <div className="space-y-0.5">
            <div className="flex items-center gap-2">
              <span
                className={`w-2 h-2 rounded-full ${
                  selectedEntity.type === 'safe'
                    ? 'bg-[#10B981]'
                    : selectedEntity.type === 'unsafe'
                    ? 'bg-[#EF4444]'
                    : 'bg-[#3B82F6]'
                }`}
              />
              <span className="font-bold text-white text-xs">{selectedEntity.title}</span>
              {selectedEntity.category && (
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-white/10 text-[#C8BFFF] uppercase font-bold">
                  {selectedEntity.category}
                </span>
              )}
            </div>
            <p className="text-[11px] text-[#B8B4D0] leading-snug">{selectedEntity.detail}</p>
          </div>

          <a
            href={`https://www.google.com/maps/dir/?api=1&destination=${selectedEntity.lat},${selectedEntity.lng}`}
            target="_blank"
            rel="noopener noreferrer"
            className="p-2 rounded-xl bg-white/10 hover:bg-white/15 text-white flex items-center gap-1 text-[11px] font-bold shrink-0 ml-2"
          >
            <ExternalLink className="w-3.5 h-3.5" />
            Navigate
          </a>
        </div>
      )}
    </div>
  );
};
