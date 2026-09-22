import React, { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import { LocationPoint, SafetyPlace, UnsafeZone } from '../types';
import { Shield, AlertTriangle, Navigation, MapPin, Compass, ExternalLink } from 'lucide-react';

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

export const InteractiveSafeMap: React.FC<InteractiveSafeMapProps> = ({
  location,
  safePlaces,
  unsafeZones,
  recommendedEscapeRoute,
}) => {
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const layerGroupRef = useRef<L.LayerGroup | null>(null);

  const [selectedEntity, setSelectedEntity] = useState<{
    type: 'user' | 'safe' | 'unsafe';
    title: string;
    detail: string;
    distance?: number;
    category?: string;
    lat: number;
    lng: number;
  } | null>(null);

  const [filterMode, setFilterMode] = useState<'all' | 'safe' | 'unsafe'>('all');

  const defaultLat = location?.latitude || 37.7749;
  const defaultLng = location?.longitude || -122.4194;

  useEffect(() => {
    if (!mapContainerRef.current) return;

    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: [defaultLat, defaultLng],
        zoom: 15,
        zoomControl: false,
      });

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors',
        maxZoom: 19,
      }).addTo(map);

      // Zoom control in bottom right
      L.control.zoom({ position: 'bottomright' }).addTo(map);

      mapInstanceRef.current = map;
      layerGroupRef.current = L.layerGroup().addTo(map);
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
        layerGroupRef.current = null;
      }
    };
  }, []);

  // Update markers and zones whenever location, places, or filters change
  useEffect(() => {
    const map = mapInstanceRef.current;
    const layers = layerGroupRef.current;
    if (!map || !layers) return;

    layers.clearLayers();

    const curLat = location?.latitude || defaultLat;
    const curLng = location?.longitude || defaultLng;

    // 1. Render User's Live Position Marker
    const userMarker = L.circleMarker([curLat, curLng], {
      radius: 10,
      fillColor: '#3B82F6',
      color: '#FFFFFF',
      weight: 3,
      opacity: 1,
      fillOpacity: 0.9,
    });

    const userPulse = L.circle([curLat, curLng], {
      radius: location?.accuracyMeters ? Math.max(15, location.accuracyMeters) : 25,
      color: '#3B82F6',
      fillColor: '#60A5FA',
      fillOpacity: 0.15,
      weight: 1.5,
    });

    userMarker.on('click', () => {
      setSelectedEntity({
        type: 'user',
        title: 'You Are Here (Live Fix)',
        detail: `Coordinates: ${curLat.toFixed(5)}, ${curLng.toFixed(5)} (Accuracy ±${Math.round(location?.accuracyMeters || 10)}m)`,
        lat: curLat,
        lng: curLng,
      });
    });

    layers.addLayer(userPulse);
    layers.addLayer(userMarker);

    // 2. Render Unsafe Zones (Identified by Phi-3.5 Mini)
    if (filterMode === 'all' || filterMode === 'unsafe') {
      unsafeZones.forEach((zone) => {
        const isCritical = zone.riskSeverity === 'CRITICAL';
        const color = isCritical ? '#EF4444' : '#F59E0B';

        const circle = L.circle([zone.latitude, zone.longitude], {
          radius: zone.radiusMeters,
          color,
          fillColor: color,
          fillOpacity: 0.22,
          weight: 2,
          dashArray: '4, 6',
        });

        circle.bindTooltip(`⚠️ ${zone.name} (${zone.riskSeverity})`, {
          direction: 'top',
          className: 'bg-zinc-900 text-white text-xs font-semibold px-2 py-1 rounded shadow',
        });

        circle.on('click', () => {
          setSelectedEntity({
            type: 'unsafe',
            title: zone.name,
            detail: zone.reason,
            category: zone.riskSeverity,
            lat: zone.latitude,
            lng: zone.longitude,
          });
        });

        layers.addLayer(circle);

        // Center warning icon pin
        const centerIcon = L.circleMarker([zone.latitude, zone.longitude], {
          radius: 6,
          fillColor: color,
          color: '#FFFFFF',
          weight: 2,
          fillOpacity: 1,
        });

        layers.addLayer(centerIcon);
      });
    }

    // 3. Render Safe Places
    if (filterMode === 'all' || filterMode === 'safe') {
      safePlaces.forEach((place) => {
        const isPolice = place.category === 'police';
        const isHospital = place.category === 'hospital';
        const color = isPolice ? '#2563EB' : isHospital ? '#DC2626' : '#10B981';

        const placeMarker = L.circleMarker([place.latitude, place.longitude], {
          radius: 8,
          fillColor: color,
          color: '#FFFFFF',
          weight: 2,
          fillOpacity: 0.9,
        });

        placeMarker.bindTooltip(`🛡️ ${place.name}`, {
          direction: 'top',
          className: 'bg-zinc-900 text-white text-xs font-bold px-2 py-1 rounded shadow',
        });

        placeMarker.on('click', () => {
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

        layers.addLayer(placeMarker);
      });
    }

    // 4. If Recommended Escape Route exists, draw direct walking path
    if (recommendedEscapeRoute && safePlaces.length > 0) {
      const target = safePlaces[0];
      const routeLine = L.polyline(
        [
          [curLat, curLng],
          [target.latitude, target.longitude],
        ],
        {
          color: '#10B981',
          weight: 4,
          dashArray: '6, 8',
          opacity: 0.85,
        }
      );
      layers.addLayer(routeLine);
    }
  }, [location, safePlaces, unsafeZones, filterMode, defaultLat, defaultLng, recommendedEscapeRoute]);

  const handleRecenter = () => {
    if (mapInstanceRef.current) {
      const curLat = location?.latitude || defaultLat;
      const curLng = location?.longitude || defaultLng;
      mapInstanceRef.current.flyTo([curLat, curLng], 15, { duration: 1 });
    }
  };

  return (
    <div id="interactive-safe-map-container" className="rounded-[24px] bg-[#121124] border border-white/10 p-4 space-y-3.5 shadow-lg">
      {/* Header with Title & Filter Buttons */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-[#5B4BDB]/20 text-[#A594FD] flex items-center justify-center">
            <Compass className="w-5 h-5" />
          </div>
          <div>
            <h4 className="text-sm font-black text-white uppercase tracking-wider flex items-center gap-2">
              Safe Havens & Danger Zones
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-[#5B4BDB]/30 text-[#D4CDFF] font-bold">
                Phi-3.5 Evaluated
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

      {/* Filter Tabs */}
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
          Safe Havens ({safePlaces.length})
        </button>
        <button
          type="button"
          onClick={() => setFilterMode('unsafe')}
          className={`flex-1 py-1.5 rounded-lg flex items-center justify-center gap-1 transition-colors ${
            filterMode === 'unsafe' ? 'bg-[#EF4444] text-white' : 'text-[#A6A2BC] hover:text-[#F87171]'
          }`}
        >
          <AlertTriangle className="w-3 h-3" />
          Unsafe Places ({unsafeZones.length})
        </button>
      </div>

      {/* Leaflet Map Frame */}
      <div className="relative rounded-[20px] overflow-hidden border border-white/10 h-[280px] w-full bg-zinc-950">
        <div ref={mapContainerRef} className="w-full h-full z-0" />

        {/* Legend Overlay on Bottom Left */}
        <div className="absolute bottom-2 left-2 z-10 bg-black/80 backdrop-blur-md px-2.5 py-1.5 rounded-lg border border-white/10 text-[10px] space-y-1">
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
      </div>

      {/* Recommended Escape Guidance Banner */}
      {recommendedEscapeRoute && (
        <div className="p-3 rounded-2xl bg-[#0F261E] border border-[#10B981]/30 flex items-start gap-2.5 text-xs text-[#D1FAE5]">
          <Shield className="w-4 h-4 text-[#34D399] shrink-0 mt-0.5" />
          <div className="space-y-0.5">
            <span className="font-extrabold text-[#34D399] uppercase tracking-wider block text-[10px]">
              Phi-3.5 Recommended Safe Route
            </span>
            <p className="leading-snug">{recommendedEscapeRoute.guidanceStep}</p>
          </div>
        </div>
      )}

      {/* Entity Details Popup Card if selected */}
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
            href={`https://maps.google.com/?q=${selectedEntity.lat},${selectedEntity.lng}`}
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
