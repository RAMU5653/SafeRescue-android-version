import React, { useState, useRef, useEffect, useCallback } from 'react';
import { Camera, MapPin, Mic, ShieldAlert, Cpu, AlertOctagon, Send, MessageSquare } from 'lucide-react';
import { LocationPoint, LocationStatus, RiskSeverity, VoiceSignalMetrics, VoiceStatus } from '../types';

interface ActiveEmergencyCardProps {
  remainingSeconds: number;
  locationStatus: LocationStatus;
  location: LocationPoint | null;
  voiceStatus: VoiceStatus;
  voiceMetrics: VoiceSignalMetrics | null;
  riskScore: number;
  riskSeverity: RiskSeverity;
  onOpenCamera: () => void;
  onCancelComplete: () => void;
  onOpenSendRealMessage?: () => void;
}

export const ActiveEmergencyCard: React.FC<ActiveEmergencyCardProps> = ({
  remainingSeconds,
  locationStatus,
  location,
  voiceStatus,
  voiceMetrics,
  riskScore,
  riskSeverity,
  onOpenCamera,
  onCancelComplete,
  onOpenSendRealMessage,
}) => {
  const [holdingCancel, setHoldingCancel] = useState(false);
  const [cancelProgress, setCancelProgress] = useState(0);
  const startTimeRef = useRef<number | null>(null);
  const animFrameRef = useRef<number | null>(null);

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const handleStartCancel = (e: React.SyntheticEvent) => {
    e.preventDefault();
    setHoldingCancel(true);
    startTimeRef.current = Date.now();
  };

  const handleReleaseCancel = useCallback(() => {
    if (holdingCancel && cancelProgress < 1) {
      setHoldingCancel(false);
      setCancelProgress(0);
      startTimeRef.current = null;
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    }
  }, [holdingCancel, cancelProgress]);

  useEffect(() => {
    if (!holdingCancel) {
      setCancelProgress(0);
      startTimeRef.current = null;
      return;
    }

    const interval = 5000;
    const tick = () => {
      if (!startTimeRef.current) return;
      const elapsed = Date.now() - startTimeRef.current;
      const p = Math.min(1, elapsed / interval);
      setCancelProgress(p);

      if (p >= 1) {
        setHoldingCancel(false);
        if ('vibrate' in navigator) {
          try {
            navigator.vibrate(150);
          } catch {
            // ignore
          }
        }
        onCancelComplete();
      } else {
        animFrameRef.current = requestAnimationFrame(tick);
      }
    };

    animFrameRef.current = requestAnimationFrame(tick);

    return () => {
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    };
  }, [holdingCancel, onCancelComplete]);

  useEffect(() => {
    const onUp = () => {
      if (holdingCancel) handleReleaseCancel();
    };
    window.addEventListener('mouseup', onUp);
    window.addEventListener('touchend', onUp);
    return () => {
      window.removeEventListener('mouseup', onUp);
      window.removeEventListener('touchend', onUp);
    };
  }, [holdingCancel, handleReleaseCancel]);

  const currentCancelSeconds = Math.min(5, Math.floor(cancelProgress * 5) + 1);

  return (
    <div
      id="active-emergency-card"
      className="w-full rounded-[24px] p-5 bg-[#26131A] border-2 border-[#E51E4D]/70 shadow-[0_0_30px_rgba(229,30,77,0.3)] text-white emergency-pulse"
    >
      <div className="flex items-center justify-between">
        <span className="text-[13px] font-black uppercase tracking-wider text-[#FFA7BF] flex items-center gap-1.5">
          <AlertOctagon className="w-4 h-4 text-[#E51E4D] animate-spin" /> EMERGENCY MODE
        </span>
        <span className="text-xs px-2.5 py-0.5 rounded-full bg-[#E51E4D]/30 text-[#FFA7BF] font-semibold border border-[#E51E4D]/40">
          PROTECTION LIVE
        </span>
      </div>

      <div className="my-3">
        <div className="text-[40px] font-black text-[#FFD6E1] leading-none tracking-tight">
          {formatTime(remainingSeconds)}
        </div>
        <div className="text-[12px] text-[#FFA7BF] font-medium mt-1">
          {remainingSeconds === 0
            ? '🚨 2-Min Timer Expired: Emergency SMS Dispatched to Contacts with GPS & Evidences'
            : '2-Minute Safety Window • Automatic SMS with GPS & Evidences sent at 00:00'}
        </div>

        {onOpenSendRealMessage && (
          <button
            type="button"
            id="btn-active-send-now"
            onClick={onOpenSendRealMessage}
            className="w-full mt-2.5 py-2 px-3 rounded-xl bg-[#E51E4D] hover:bg-[#c91841] text-white text-xs font-bold flex items-center justify-center gap-2 shadow-md shadow-[#E51E4D]/40 transition-all active:scale-[0.98]"
          >
            <Send className="w-3.5 h-3.5" />
            Send Real Emergency Message Now (SMS / WhatsApp)
          </button>
        )}
      </div>

      {/* Sensor Signals Matrix */}
      <div className="rounded-[18px] bg-[#1a0c12] p-3.5 space-y-2.5 border border-[#4B1724]">
        {/* Victim Voice */}
        <div className="flex items-center justify-between text-xs">
          <div className="flex items-center gap-2 text-white font-medium">
            <Mic className="w-3.5 h-3.5 text-[#FFA7BF]" />
            <span>Victim Voice</span>
          </div>
          <div className="flex items-center gap-2">
            {voiceMetrics && (
              <span className="text-[11px] text-[#FFA7BF] font-mono">
                {voiceMetrics.rmsDb.toFixed(0)} dB
              </span>
            )}
            <span
              className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                voiceStatus === VoiceStatus.LISTENING
                  ? 'bg-emerald-950 text-emerald-300 border border-emerald-800'
                  : 'bg-zinc-800 text-zinc-300'
              }`}
            >
              {voiceStatus}
            </span>
          </div>
        </div>

        {/* Camera */}
        <div className="flex items-center justify-between text-xs">
          <div className="flex items-center gap-2 text-white font-medium">
            <Camera className="w-3.5 h-3.5 text-[#FFA7BF]" />
            <span>Camera Evidence</span>
          </div>
          <button
            type="button"
            id="btn-quick-camera"
            onClick={onOpenCamera}
            className="text-[11px] font-bold px-2.5 py-1 rounded-md bg-[#5B4BDB] hover:bg-[#4838c4] text-white transition-colors"
          >
            Open Lens
          </button>
        </div>

        {/* GPS Location */}
        <div className="flex items-center justify-between text-xs">
          <div className="flex items-center gap-2 text-white font-medium">
            <MapPin className="w-3.5 h-3.5 text-[#FFA7BF]" />
            <span>GPS Tracking</span>
          </div>
          <div className="text-right">
            <span
              className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                locationStatus === LocationStatus.ACTIVE
                  ? 'bg-emerald-950 text-emerald-300 border border-emerald-800'
                  : 'bg-amber-950 text-amber-300'
              }`}
            >
              {locationStatus === LocationStatus.ACTIVE && location
                ? `±${Math.round(location.accuracyMeters)}m`
                : locationStatus}
            </span>
          </div>
        </div>

        {/* Risk Score */}
        <div className="flex items-center justify-between text-xs">
          <div className="flex items-center gap-2 text-white font-medium">
            <ShieldAlert className="w-3.5 h-3.5 text-[#FFA7BF]" />
            <span>Risk Score</span>
          </div>
          <span className="font-bold text-[#FFA7BF]">
            {riskScore}/100 • {riskSeverity}
          </span>
        </div>

        {/* Local AI */}
        <div className="flex items-center justify-between text-xs">
          <div className="flex items-center gap-2 text-white font-medium">
            <Cpu className="w-3.5 h-3.5 text-[#FFA7BF]" />
            <span>Local AI Model</span>
          </div>
          <span className="text-[11px] text-[#B8C4D9]">Phase 18 • Local slot</span>
        </div>
      </div>

      {/* Hold to Cancel */}
      <div className="mt-4">
        <button
          type="button"
          id="btn-hold-cancel-sos"
          onMouseDown={handleStartCancel}
          onTouchStart={handleStartCancel}
          className={`w-full h-14 rounded-[16px] flex items-center justify-center gap-2 font-bold text-sm tracking-wide transition-all select-none active:scale-[0.98] ${
            holdingCancel
              ? 'bg-[#6b0f2e] text-white scale-[0.99]'
              : 'bg-[#8E1740] hover:bg-[#a61c4c] text-white shadow-[0_4px_16px_rgba(142,23,64,0.4)]'
          }`}
        >
          {holdingCancel ? `HOLD CANCEL • ${currentCancelSeconds}S / 5S` : 'HOLD 5 SEC TO CANCEL'}
        </button>

        <div className="w-full h-1.5 rounded-full bg-[#512431] mt-2 overflow-hidden">
          <div
            className="h-full bg-white transition-[width] duration-75"
            style={{ width: `${cancelProgress * 100}%` }}
          />
        </div>
      </div>
    </div>
  );
};
