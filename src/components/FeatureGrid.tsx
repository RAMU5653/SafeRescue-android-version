import React from 'react';
import { MapPin, Shield, Video, Users, Info } from 'lucide-react';
import { EmergencyState, LocationPoint, LocationStatus, RiskSeverity, VoiceStatus } from '../types';

interface FeatureGridProps {
  emergencyState: EmergencyState;
  locationStatus: LocationStatus;
  location: LocationPoint | null;
  riskScore: number;
  riskSeverity: RiskSeverity;
  voiceStatus: VoiceStatus;
  voiceMessage: string | null;
  lastCaptureName: string | null;
  onOpenCamera: () => void;
  onOpenContacts: () => void;
}

export const FeatureGrid: React.FC<FeatureGridProps> = ({
  emergencyState,
  locationStatus,
  location,
  riskScore,
  riskSeverity,
  voiceStatus,
  voiceMessage,
  lastCaptureName,
  onOpenCamera,
  onOpenContacts,
}) => {
  const isEmergencyActive =
    emergencyState === EmergencyState.EMERGENCY_ACTIVE ||
    emergencyState === EmergencyState.MONITORING ||
    emergencyState === EmergencyState.CANCELLATION_PENDING;

  const getLocationLabel = () => {
    switch (locationStatus) {
      case LocationStatus.ACTIVE:
        return location ? `±${Math.round(location.accuracyMeters)}m fix` : 'Tracking active';
      case LocationStatus.PERMISSION_REQUIRED:
        return 'Permission needed';
      case LocationStatus.UNAVAILABLE:
        return 'Unavailable';
      case LocationStatus.ERROR:
        return 'Service error';
      default:
        return 'Ready';
    }
  };

  return (
    <div className="space-y-3">
      <h3 className="text-lg font-bold text-white">Safety Controls</h3>

      {/* 2x2 Feature Cards */}
      <div className="grid grid-cols-2 gap-2.5">
        {/* Location */}
        <div className="rounded-[20px] p-4 bg-[#0D2140] border border-white/5 flex flex-col justify-between">
          <div className="w-8 h-8 rounded-full bg-[#12264A] flex items-center justify-center text-[#55D7FF]">
            <MapPin className="w-4 h-4" />
          </div>
          <div className="mt-3">
            <h4 className="text-sm font-bold text-white">Location</h4>
            <p className="text-xs text-[#B8C4D9] truncate mt-0.5">{getLocationLabel()}</p>
            <span className="text-[11px] font-semibold text-[#FF72C8] mt-1 inline-block">
              Phase 6
            </span>
          </div>
        </div>

        {/* Risk Engine */}
        <div className="rounded-[20px] p-4 bg-[#0D2140] border border-white/5 flex flex-col justify-between">
          <div className="w-8 h-8 rounded-full bg-[#12264A] flex items-center justify-center text-[#55D7FF]">
            <Shield className="w-4 h-4" />
          </div>
          <div className="mt-3">
            <h4 className="text-sm font-bold text-white">Risk Engine</h4>
            <p className="text-xs text-[#B8C4D9] mt-0.5">
              {riskScore === 0 ? 'Ready' : `${riskScore}/100 • ${riskSeverity}`}
            </p>
            <span className="text-[11px] font-semibold text-[#FF72C8] mt-1 inline-block">
              Phase 9
            </span>
          </div>
        </div>

        {/* Evidence / Camera */}
        <button
          type="button"
          id="btn-evidence-camera"
          onClick={isEmergencyActive ? onOpenCamera : undefined}
          className={`rounded-[20px] p-4 bg-[#0D2140] border border-white/5 text-left flex flex-col justify-between transition-all ${
            isEmergencyActive ? 'hover:bg-[#132c52] cursor-pointer' : 'opacity-70 cursor-not-allowed'
          }`}
        >
          <div className="w-8 h-8 rounded-full bg-[#12264A] flex items-center justify-center text-[#55D7FF]">
            <Video className="w-4 h-4" />
          </div>
          <div className="mt-3">
            <h4 className="text-sm font-bold text-white">Evidence</h4>
            <p className="text-xs text-[#B8C4D9] mt-0.5">
              {isEmergencyActive ? 'Camera available' : 'Start SOS first'}
            </p>
            <span className="text-[11px] font-semibold text-[#FF72C8] mt-1 inline-block">
              Phase 7
            </span>
          </div>
        </button>

        {/* Contacts */}
        <button
          type="button"
          id="btn-nav-contacts"
          onClick={onOpenContacts}
          className="rounded-[20px] p-4 bg-[#0D2140] border border-white/5 text-left flex flex-col justify-between hover:bg-[#132c52] transition-colors cursor-pointer"
        >
          <div className="w-8 h-8 rounded-full bg-[#12264A] flex items-center justify-center text-[#55D7FF]">
            <Users className="w-4 h-4" />
          </div>
          <div className="mt-3">
            <h4 className="text-sm font-bold text-white">Contacts</h4>
            <p className="text-xs text-[#B8C4D9] mt-0.5">Manage trusted list</p>
            <span className="text-[11px] font-semibold text-[#FF72C8] mt-1 inline-block">
              Phase 14
            </span>
          </div>
        </button>
      </div>

      {/* Info Cards */}
      <div className="space-y-2 pt-1">
        {/* Location Info */}
        <div className="rounded-[18px] p-4 bg-[#0D2140] border border-white/5 flex gap-3 text-white">
          <div className="w-7 h-7 rounded-full bg-[#12264A] flex items-center justify-center text-[#55D7FF] shrink-0 mt-0.5">
            <MapPin className="w-3.5 h-3.5" />
          </div>
          <div>
            <h5 className="text-xs font-bold text-white">Phase 6 • GPS Location</h5>
            <p className="text-[11px] text-[#B8C4D9] leading-relaxed mt-0.5">
              {location
                ? `Latest coordinates: ${location.latitude.toFixed(5)}, ${location.longitude.toFixed(
                    5
                  )} (±${Math.round(location.accuracyMeters)}m). GPS failure never cancels an emergency.`
                : 'Location monitoring initializes after SOS when permission is granted. GPS failure never stops an emergency.'}
            </p>
          </div>
        </div>

        {/* Camera Info if captured */}
        {lastCaptureName && (
          <div className="rounded-[18px] p-4 bg-[#0D2140] border border-[#55D7FF]/30 flex gap-3 text-white">
            <div className="w-7 h-7 rounded-full bg-[#12264A] flex items-center justify-center text-[#55D7FF] shrink-0 mt-0.5">
              <Video className="w-3.5 h-3.5" />
            </div>
            <div>
              <h5 className="text-xs font-bold text-white">Phase 7 • Captured Evidence</h5>
              <p className="text-[11px] text-[#72D8FF] leading-relaxed mt-0.5 font-mono">
                {lastCaptureName}
              </p>
            </div>
          </div>
        )}

        {/* Voice Info */}
        <div className="rounded-[18px] p-4 bg-[#0D2140] border border-white/5 flex gap-3 text-white">
          <div className="w-7 h-7 rounded-full bg-[#12264A] flex items-center justify-center text-[#55D7FF] shrink-0 mt-0.5">
            <Info className="w-3.5 h-3.5" />
          </div>
          <div>
            <h5 className="text-xs font-bold text-white">Phase 8 • Victim Voice Pipeline</h5>
            <p className="text-[11px] text-[#B8C4D9] leading-relaxed mt-0.5">
              {voiceMessage ||
                (voiceStatus === VoiceStatus.LISTENING
                  ? 'Microphone monitoring active • acoustic volume analyzed locally • raw voice stays private'
                  : 'Starts after SOS when microphone permission is granted. Acoustic metrics feed heuristic risk.')}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
