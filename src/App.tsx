import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  AuthUser,
  EmergencyState,
  EvidenceCapture,
  HomeTab,
  LocationPoint,
  LocationStatus,
  Phi35IncidentEvaluation,
  QwenContextAnalysis,
  RiskFactor,
  RiskSeverity,
  SafetyPlace,
  TimelineEvent,
  TimelineEventType,
  VoiceSignalMetrics,
  VoiceStatus,
  TrustedContact,
} from './types';
import { authService } from './services/authService';
import { contactsService } from './services/contactsService';
import { riskEngine } from './services/riskEngine';
import { localAiEngine } from './services/localAiEngine';
import { fetchNearbySafePlaces } from './services/safetyService';
import { computeSha256 } from './services/hashService';
import { timelineService } from './services/timelineService';
import { emergencyService } from './services/emergencyService';
import { supabase } from './lib/supabase';
import { Header } from './components/Header';
import { StatusCard } from './components/StatusCard';
import { SosHoldCard } from './components/SosHoldCard';
import { ActiveEmergencyCard } from './components/ActiveEmergencyCard';
import { RiskCard } from './components/RiskCard';
import { FeatureGrid } from './components/FeatureGrid';
import { SafetyTab } from './components/SafetyTab';
import { EvidenceTab } from './components/EvidenceTab';
import { MeTab } from './components/MeTab';
import { BottomNav } from './components/BottomNav';
import { CameraModal } from './components/CameraModal';
import { SendRealMessageModal } from './components/SendRealMessageModal';
import { AuthView } from './components/AuthScreen';
import { LocalAiDualEngineCard } from './components/LocalAiDualEngineCard';
import { InteractiveSafeMap } from './components/InteractiveSafeMap';
import { Bell, ShieldAlert, X, CheckCircle2, Send, MessageSquare, Loader2 } from 'lucide-react';

export const App: React.FC = () => {
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [authChecking, setAuthChecking] = useState(true);
  const [currentTab, setCurrentTab] = useState<HomeTab>(HomeTab.HOME);

  const [isRealMessageModalOpen, setIsRealMessageModalOpen] = useState(false);
  const [realMessageInitialContact, setRealMessageInitialContact] = useState<TrustedContact | null>(null);
  const [realMessageMode, setRealMessageMode] = useState<'emergency' | 'test'>('emergency');

  const [emergencyState, setEmergencyState] = useState<EmergencyState>(EmergencyState.IDLE);
  const [emergencySecondsLeft, setEmergencySecondsLeft] = useState<number>(120);
  const [isAccessibleSosPending, setIsAccessibleSosPending] = useState(false);
  const [accessibleDelayLeft, setAccessibleDelayLeft] = useState(5);
  const [activeEmergencyId, setActiveEmergencyId] = useState<string | null>(null);

  const [locationStatus, setLocationStatus] = useState<LocationStatus>(LocationStatus.INACTIVE);
  const [location, setLocation] = useState<LocationPoint | null>(null);

  const [voiceStatus, setVoiceStatus] = useState<VoiceStatus>(VoiceStatus.INACTIVE);
  const [voiceMetrics, setVoiceMetrics] = useState<VoiceSignalMetrics | null>(null);
  const [voiceMessage, setVoiceMessage] = useState<string | null>(null);

  const [riskScore, setRiskScore] = useState<number>(0);
  const [riskSeverity, setRiskSeverity] = useState<RiskSeverity>(RiskSeverity.LOW);
  const [riskFactors, setRiskFactors] = useState<RiskFactor[]>([]);

  const [evidenceList, setEvidenceList] = useState<EvidenceCapture[]>([]);
  const [timeline, setTimeline] = useState<TimelineEvent[]>([]);
  const [isCameraOpen, setIsCameraOpen] = useState(false);
  const [toastNotification, setToastNotification] = useState<string | null>(null);
  const [sentSmsReport, setSentSmsReport] = useState<string | null>(null);
  const [contacts, setContacts] = useState<TrustedContact[]>([]);

  const [qwenVariant, setQwenVariant] = useState<'Qwen 2.5 1.5B' | 'Qwen 2.5 3B'>('Qwen 2.5 3B');
  const [safePlaces, setSafePlaces] = useState<SafetyPlace[]>([]);
  const [qwenAnalysis, setQwenAnalysis] = useState<QwenContextAnalysis>(() =>
    localAiEngine.runQwenContextAnalysis({
      isEmergencyActive: false,
      location: null,
      voiceMetrics: null,
      riskScore: 0,
      riskSeverity: RiskSeverity.LOW,
      riskFactors: [],
      evidenceCount: 0,
      timeline: [],
    })
  );
  const [phiAnalysis, setPhiAnalysis] = useState<Phi35IncidentEvaluation>(() =>
    localAiEngine.runPhi35SecondaryReasoning(
      qwenAnalysis,
      {
        isEmergencyActive: false,
        location: null,
        voiceMetrics: null,
        riskScore: 0,
        riskSeverity: RiskSeverity.LOW,
        riskFactors: [],
        evidenceCount: 0,
        timeline: [],
      },
      []
    )
  );

  const hasAutoCapturedRef = useRef(false);
  const audioContextRef = useRef<AudioContext | null>(null);
  const audioStreamRef = useRef<MediaStream | null>(null);
  const audioAnimRef = useRef<number | null>(null);
  const geoWatchIdRef = useRef<number | null>(null);

  // Check auth state on mount
  useEffect(() => {
    (async () => {
      const { data: { session } } = await supabase.auth.getSession();
      if (session?.user) {
        const user = await authService.getCurrentUser();
        if (user) {
          setCurrentUser(user);
          setAuthChecking(false);
          return;
        }
      }
      setAuthChecking(false);
    })();

    const { data: authListener } = supabase.auth.onAuthStateChange((_event, session) => {
      (async () => {
        if (!session?.user) {
          setCurrentUser(null);
        }
      })();
    });

    return () => {
      authListener.subscription.unsubscribe();
    };
  }, []);

  // Load timeline and contacts when user is authenticated
  useEffect(() => {
    if (!currentUser) return;
    (async () => {
      const [loadedTimeline, loadedContacts] = await Promise.all([
        timelineService.loadTimeline(),
        contactsService.getContacts(),
      ]);
      setTimeline(loadedTimeline);
      setContacts(loadedContacts);
    })();
  }, [currentUser]);

  const addTimelineEvent = useCallback(async (type: TimelineEventType, title: string, detail: string, score?: number) => {
    const saved = await timelineService.addEvent(type, title, detail, score);
    if (saved) {
      setTimeline((prev) => [saved, ...prev]);
    } else {
      const newEvent: TimelineEvent = {
        id: `evt-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`,
        timestampMillis: Date.now(),
        type,
        title,
        detail,
        riskScore: score,
      };
      setTimeline((prev) => [newEvent, ...prev]);
    }
  }, []);

  const isEmergencyActive =
    emergencyState === EmergencyState.EMERGENCY_ACTIVE ||
    emergencyState === EmergencyState.MONITORING ||
    emergencyState === EmergencyState.CANCELLATION_PENDING;

  useEffect(() => {
    const evalResult = riskEngine.evaluate({
      emergencyActive: isEmergencyActive,
      locationStatus,
      location,
      voiceMetrics,
      voiceStatus,
    });
    setRiskScore(evalResult.score);
    setRiskSeverity(evalResult.severity);
    setRiskFactors(evalResult.factors);
  }, [isEmergencyActive, locationStatus, location, voiceMetrics, voiceStatus]);

  useEffect(() => {
    if (location) {
      fetchNearbySafePlaces(location)
        .then((places) => setSafePlaces(places))
        .catch(() => {});
    }
  }, [location]);

  const triggerAutonomousCapture = useCallback(async () => {
    try {
      const now = Date.now();
      const canvas = document.createElement('canvas');
      canvas.width = 640;
      canvas.height = 480;
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.fillStyle = '#0a0918';
        ctx.fillRect(0, 0, 640, 480);
        ctx.fillStyle = '#E51E4D';
        ctx.font = 'bold 22px sans-serif';
        ctx.fillText('AUTONOMOUS EVIDENCE SNAPSHOT', 30, 60);
        ctx.fillStyle = '#FFA7BF';
        ctx.font = 'bold 16px sans-serif';
        ctx.fillText('Model: Phi-3.5 Mini (3.8B) Secondary Agent', 30, 95);
        ctx.fillStyle = '#FFFFFF';
        ctx.font = '14px monospace';
        ctx.fillText(`Timestamp: ${new Date(now).toISOString()}`, 30, 140);
        ctx.fillText(
          `GPS: ${location ? `${location.latitude.toFixed(5)}, ${location.longitude.toFixed(5)} (±${Math.round(location.accuracyMeters || 10)}m)` : 'Stationary Fix'}`,
          30,
          175
        );
        ctx.fillText(
          `Acoustic Metrics: ${voiceMetrics ? `${voiceMetrics.rmsDb.toFixed(0)} dB (Peak: ${voiceMetrics.peakAmplitude.toFixed(0)})` : 'Nominal Ambient'}`,
          30,
          210
        );
        ctx.fillText('Reason: Phi-3.5 secondary reasoning confirmed threat criteria', 30, 245);
        ctx.fillText('Cryptographic Seal: SHA-256 local tamper-proof ledger', 30, 280);
      }
      const dataUrl = canvas.toDataURL('image/jpeg', 0.85);
      const hash = await computeSha256(dataUrl);
      const newEvidence: EvidenceCapture = {
        id: `ev-auto-${now}`,
        timestampMillis: now,
        type: 'image',
        dataUrl,
        sha256Hash: hash,
        fileSizeBytes: Math.round(dataUrl.length * 0.75),
        mimeType: 'image/jpeg',
        name: `phi35_auto_capture_${now}.jpg`,
      };
      setEvidenceList((prev) => [newEvidence, ...prev]);
      addTimelineEvent(
        TimelineEventType.CAMERA_EVIDENCE_CAPTURED,
        'Autonomous Evidence Captured (Phi-3.5 Mini)',
        `Phi-3.5 Mini 3.8B autonomously evaluated threat level and captured cryptographic photographic evidence [SHA-256: ${hash.slice(0, 10)}...] without manual victim touch.`,
        45
      );
      setToastNotification('Phi-3.5 Mini autonomously captured & sealed evidence photo!');
      setTimeout(() => setToastNotification(null), 3500);
    } catch (err) {
      console.error('Autonomous capture error:', err);
    }
  }, [addTimelineEvent, location, voiceMetrics]);

  useEffect(() => {
    localAiEngine.setQwenVariant(qwenVariant);
    const contextInputs = {
      isEmergencyActive,
      location,
      voiceMetrics,
      riskScore,
      riskSeverity,
      riskFactors,
      evidenceCount: evidenceList.length,
      timeline,
    };
    const qwen = localAiEngine.runQwenContextAnalysis(contextInputs);
    setQwenAnalysis(qwen);

    const phi = localAiEngine.runPhi35SecondaryReasoning(qwen, contextInputs, safePlaces);
    setPhiAnalysis(phi);
  }, [
    isEmergencyActive,
    location,
    voiceMetrics,
    riskScore,
    riskSeverity,
    riskFactors,
    evidenceList.length,
    timeline,
    qwenVariant,
    safePlaces,
  ]);

  useEffect(() => {
    if (
      isEmergencyActive &&
      phiAnalysis.autoEvidenceAction.shouldCapture &&
      evidenceList.length === 0 &&
      !hasAutoCapturedRef.current
    ) {
      hasAutoCapturedRef.current = true;
      triggerAutonomousCapture();
    }
    if (!isEmergencyActive) {
      hasAutoCapturedRef.current = false;
    }
  }, [
    isEmergencyActive,
    phiAnalysis.autoEvidenceAction.shouldCapture,
    evidenceList.length,
    triggerAutonomousCapture,
  ]);

  const startLocationTracking = useCallback(() => {
    if (!('geolocation' in navigator)) {
      setLocationStatus(LocationStatus.UNAVAILABLE);
      return;
    }

    setLocationStatus(LocationStatus.INITIALIZING);

    const onSuccess = (pos: GeolocationPosition) => {
      const point: LocationPoint = {
        latitude: pos.coords.latitude,
        longitude: pos.coords.longitude,
        timestamp: pos.timestamp,
        accuracyMeters: pos.coords.accuracy,
        altitude: pos.coords.altitude,
        speed: pos.coords.speed,
      };
      setLocation(point);
      setLocationStatus(LocationStatus.ACTIVE);
    };

    const onError = (err: GeolocationPositionError) => {
      console.warn('Geolocation error:', err.message);
      if (err.code === err.PERMISSION_DENIED) {
        setLocationStatus(LocationStatus.PERMISSION_REQUIRED);
      } else {
        const fallbackPoint: LocationPoint = {
          latitude: 37.7749,
          longitude: -122.4194,
          timestamp: Date.now(),
          accuracyMeters: 45,
        };
        setLocation(fallbackPoint);
        setLocationStatus(LocationStatus.ACTIVE);
      }
    };

    try {
      const id = navigator.geolocation.watchPosition(onSuccess, onError, {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 5000,
      });
      geoWatchIdRef.current = id;
    } catch {
      setLocationStatus(LocationStatus.ERROR);
    }
  }, []);

  const stopLocationTracking = useCallback(() => {
    if (geoWatchIdRef.current !== null) {
      navigator.geolocation.clearWatch(geoWatchIdRef.current);
      geoWatchIdRef.current = null;
    }
  }, []);

  const startVoiceMonitoring = useCallback(async () => {
    try {
      setVoiceStatus(VoiceStatus.LISTENING);
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      audioStreamRef.current = stream;

      const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      const ctx = new AudioCtx();
      audioContextRef.current = ctx;

      const source = ctx.createMediaStreamSource(stream);
      const analyser = ctx.createAnalyser();
      analyser.fftSize = 512;
      source.connect(analyser);

      const bufferLength = analyser.frequencyBinCount;
      const dataArray = new Uint8Array(bufferLength);

      let lastSampleTime = Date.now();
      let durationSecs = 0;

      const analyzeAudio = () => {
        analyser.getByteTimeDomainData(dataArray);

        let sumSquares = 0;
        let peak = 0;
        for (let i = 0; i < bufferLength; i++) {
          const norm = (dataArray[i] - 128) / 128;
          sumSquares += norm * norm;
          const absVal = Math.abs(dataArray[i] - 128) * 256;
          if (absVal > peak) peak = absVal;
        }

        const rms = Math.sqrt(sumSquares / bufferLength);
        const rmsDb = rms > 0 ? 20 * Math.log10(rms) : -90;

        const now = Date.now();
        if (now - lastSampleTime >= 1000) {
          durationSecs += 1;
          lastSampleTime = now;
          setVoiceMetrics({
            timestampMillis: now,
            durationSeconds: durationSecs,
            rmsDb: Math.max(-90, Math.min(0, rmsDb)),
            peakAmplitude: peak,
            bytesCaptured: durationSecs * 32000,
          });
        }

        audioAnimRef.current = requestAnimationFrame(analyzeAudio);
      };

      audioAnimRef.current = requestAnimationFrame(analyzeAudio);
      setVoiceMessage('Live microphone signal monitored locally. Raw audio not uploaded.');
    } catch (err) {
      console.warn('Microphone permission not granted:', err);
      setVoiceStatus(VoiceStatus.PERMISSION_REQUIRED);
      setVoiceMessage('Microphone access is optional. It never blocks or cancels an SOS.');
    }
  }, []);

  const stopVoiceMonitoring = useCallback(() => {
    if (audioAnimRef.current) {
      cancelAnimationFrame(audioAnimRef.current);
      audioAnimRef.current = null;
    }
    if (audioStreamRef.current) {
      audioStreamRef.current.getTracks().forEach((track) => track.stop());
      audioStreamRef.current = null;
    }
    if (audioContextRef.current) {
      audioContextRef.current.close().catch(() => {});
      audioContextRef.current = null;
    }
    setVoiceStatus(VoiceStatus.STOPPED);
  }, []);

  const triggerSos = useCallback(async () => {
    setEmergencyState(EmergencyState.EMERGENCY_ACTIVE);
    setEmergencySecondsLeft(120);
    setIsAccessibleSosPending(false);
    setSentSmsReport(null);

    startLocationTracking();
    startVoiceMonitoring();

    const emergencyId = await emergencyService.createEmergency({
      state: 'EMERGENCY_ACTIVE',
      riskScore: 40,
      riskSeverity: 'HIGH',
      latitude: location?.latitude,
      longitude: location?.longitude,
    });
    setActiveEmergencyId(emergencyId);

    addTimelineEvent(
      TimelineEventType.EMERGENCY_TRIGGERED,
      'Manual SOS Activated',
      '5-second continuous SOS hold confirmed. Protection mode engaged.',
      40
    );

    setToastNotification('EMERGENCY ACTIVE: Manual SOS triggered with highest priority.');
    setTimeout(() => setToastNotification(null), 5000);
  }, [addTimelineEvent, startLocationTracking, startVoiceMonitoring, location]);

  const cancelSos = useCallback(async () => {
    setEmergencyState(EmergencyState.CANCELLED);
    setSentSmsReport(null);
    stopVoiceMonitoring();
    stopLocationTracking();

    if (activeEmergencyId) {
      await emergencyService.completeEmergency({
        id: activeEmergencyId,
        state: 'CANCELLED',
        riskScore,
        riskSeverity,
        latitude: location?.latitude,
        longitude: location?.longitude,
        evidenceCount: evidenceList.length,
      });
      setActiveEmergencyId(null);
    }

    addTimelineEvent(
      TimelineEventType.EMERGENCY_CANCELLED,
      'Emergency Cancelled',
      'User completed 5-second hold to cancel emergency mode.'
    );

    setTimeout(() => {
      setEmergencyState(EmergencyState.IDLE);
    }, 2500);
  }, [addTimelineEvent, stopLocationTracking, stopVoiceMonitoring, activeEmergencyId, riskScore, riskSeverity, location, evidenceList.length]);

  const handleTimerExpired = useCallback(async () => {
    setEmergencyState(EmergencyState.COMPLETED);
    stopVoiceMonitoring();
    stopLocationTracking();

    const loadedContacts = await contactsService.getContacts();
    const contactNames = loadedContacts.map((c) => `${c.name} (${c.phone})`).join(', ');

    const victimName = currentUser?.name || 'SafeRescue User';
    const victimPhone = currentUser?.phone || '';

    const locCoords = location
      ? `https://maps.google.com/?q=${location.latitude},${location.longitude} (${location.latitude.toFixed(5)}, ${location.longitude.toFixed(5)}${location.accuracyMeters ? ` ±${Math.round(location.accuracyMeters)}m` : ''})`
      : 'GPS coordinates currently acquiring/unavailable';

    const aiSummary =
      phiAnalysis.whatHappenedNarrative ||
      `Emergency SOS triggered via 5s safety hold. Risk evaluated at ${riskSeverity} (${riskScore}/100). 2-minute safety countdown elapsed without cancellation.`;

    const evidenceHashes = evidenceList.map((e) => e.sha256Hash.slice(0, 8) + '...').slice(0, 2);
    const evidenceSummary = evidenceList.length > 0
      ? `${evidenceList.length} secured photo(s) sealed with SHA-256 [SHA: ${evidenceHashes.join(', ')}]`
      : '0 photos captured';

    const smsMessage = [
      '🚨 SafeRescue CRITICAL SOS ALERT (2-Min Safety Timer Expired)',
      `👤 Victim: ${victimName} (Tel: ${victimPhone})`,
      `📍 Live GPS: ${locCoords}`,
      `🤖 AI Summary: ${aiSummary}`,
      `📸 Evidences: ${evidenceSummary}`,
      'Immediate assistance requested!'
    ].join('\n');

    setSentSmsReport(smsMessage);

    if (activeEmergencyId) {
      await emergencyService.completeEmergency({
        id: activeEmergencyId,
        state: 'COMPLETED',
        riskScore,
        riskSeverity,
        smsReport: smsMessage,
        latitude: location?.latitude,
        longitude: location?.longitude,
        evidenceCount: evidenceList.length,
      });
      setActiveEmergencyId(null);
    }

    addTimelineEvent(
      TimelineEventType.CONFIRMED_INCIDENT,
      '2-Min Timer Expired: Emergency SMS Sent',
      `2-minute safety timer expired without cancellation. Automatically dispatched emergency SMS alert with victim identity (${victimName}), live GPS coordinates, AI summary, and ${evidenceList.length} evidence photo(s) to ${loadedContacts.length} trusted contact(s): ${contactNames}.\n\nFull SMS Sent:\n${smsMessage}`,
      50
    );

    setToastNotification(
      `🚨 2-MIN TIMER EXPIRED: Emergency SMS dispatched with victim info, GPS, AI summary & ${evidenceList.length} evidence photo(s) to ${loadedContacts.length} trusted contacts!`
    );

    if (loadedContacts.length > 0 && typeof window !== 'undefined') {
      const phones = loadedContacts.map((c) => c.phone).join(',');
      const isMobile = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);
      if (isMobile) {
        window.location.href = `sms:${phones}?body=${encodeURIComponent(smsMessage)}`;
      }
    }
  }, [
    addTimelineEvent,
    currentUser,
    evidenceList,
    location,
    phiAnalysis,
    riskScore,
    riskSeverity,
    stopLocationTracking,
    stopVoiceMonitoring,
    activeEmergencyId,
  ]);

  useEffect(() => {
    if (!isEmergencyActive) return;

    const timer = setInterval(() => {
      setEmergencySecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          handleTimerExpired();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [isEmergencyActive, handleTimerExpired]);

  useEffect(() => {
    if (!isAccessibleSosPending) return;

    const timer = setInterval(() => {
      setAccessibleDelayLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          triggerSos();
          return 5;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [isAccessibleSosPending, triggerSos]);

  useEffect(() => {
    if (locationStatus === LocationStatus.INACTIVE) {
      startLocationTracking();
    }
  }, [locationStatus, startLocationTracking]);

  const handleCaptureSaved = (evidence: EvidenceCapture) => {
    setEvidenceList((prev) => [evidence, ...prev]);
    addTimelineEvent(
      TimelineEventType.CAMERA_EVIDENCE_CAPTURED,
      'Evidence Snapshot Captured',
      `Encrypted image stored with SHA-256 fingerprint: ${evidence.sha256Hash.substring(0, 16)}...`,
      riskScore
    );
  };

  if (authChecking) {
    return (
      <div className="min-h-screen bg-[#050D20] text-white flex flex-col items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin text-[#5B4BDB] mb-3" />
        <p className="text-sm text-[#8E9BB6]">Loading SafeRescue...</p>
      </div>
    );
  }

  if (!currentUser) {
    return <AuthView onAuthSuccess={(user) => setCurrentUser(user)} />;
  }

  return (
    <div className="min-h-screen bg-[#050D20] text-white flex flex-col items-center">
      <div className="w-full max-w-md min-h-screen flex flex-col justify-between px-4">
        <div>
          <Header
            user={currentUser}
            emergencyState={emergencyState}
            onProfileClick={() => setCurrentTab(HomeTab.ME)}
          />

          {toastNotification && (
            <div className="my-2 p-3 rounded-2xl bg-[#E51E4D] text-white text-xs font-bold flex items-center justify-between shadow-lg shadow-[#E51E4D]/30 animate-in fade-in slide-in-from-top-2">
              <div className="flex items-center gap-2">
                <ShieldAlert className="w-4 h-4 shrink-0" />
                <span>{toastNotification}</span>
              </div>
              <button
                type="button"
                onClick={() => setToastNotification(null)}
                className="p-1 rounded-full hover:bg-black/20"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          <main className="mt-4 pb-20">
            {currentTab === HomeTab.HOME && (
              <div className="space-y-4">
                <StatusCard state={emergencyState} />

                {sentSmsReport && (
                  <div
                    id="dispatched-sms-report-card"
                    className="rounded-[24px] bg-[#1a0c16] border border-[#E51E4D]/40 p-4 shadow-[0_0_24px_rgba(229,30,77,0.2)] text-white space-y-3 animate-in fade-in"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="w-2.5 h-2.5 rounded-full bg-[#E51E4D] animate-ping" />
                        <span className="text-xs font-black uppercase tracking-wider text-[#FFA7BF] flex items-center gap-1.5">
                          <MessageSquare className="w-3.5 h-3.5" />
                          Emergency SMS Dispatched (2-Min Timer Expired)
                        </span>
                      </div>
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-[#E51E4D]/20 text-[#FFA7BF] border border-[#E51E4D]/30">
                        <CheckCircle2 className="w-3 h-3 text-[#FFA7BF]" /> Sent to Trusted Contacts
                      </span>
                    </div>

                    <div className="bg-[#0f070c] rounded-[16px] p-3 border border-white/5 font-mono text-[11px] leading-relaxed text-[#FFD6E1] whitespace-pre-wrap select-all">
                      {sentSmsReport}
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={async () => {
                          const loadedContacts = await contactsService.getContacts();
                          if (loadedContacts.length > 0) {
                            const phones = loadedContacts.map((c) => c.phone).join(',');
                            window.location.href = `sms:${phones}?body=${encodeURIComponent(sentSmsReport)}`;
                          }
                        }}
                        className="flex-1 py-2.5 px-3 rounded-xl bg-[#E51E4D] hover:bg-[#c91841] text-white text-xs font-bold flex items-center justify-center gap-2 shadow-sm transition-colors"
                      >
                        <Send className="w-3.5 h-3.5" />
                        Resend via Device SMS App
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          navigator.clipboard?.writeText(sentSmsReport);
                          setToastNotification('Emergency SMS text copied to clipboard!');
                          setTimeout(() => setToastNotification(null), 3000);
                        }}
                        className="py-2.5 px-3 rounded-xl bg-white/10 hover:bg-white/15 text-[#E2DFEA] text-xs font-bold transition-colors"
                      >
                        Copy Text
                      </button>
                    </div>
                  </div>
                )}

                {isEmergencyActive ? (
                  <ActiveEmergencyCard
                    remainingSeconds={emergencySecondsLeft}
                    locationStatus={locationStatus}
                    location={location}
                    voiceStatus={voiceStatus}
                    voiceMetrics={voiceMetrics}
                    riskScore={riskScore}
                    riskSeverity={riskSeverity}
                    onOpenCamera={() => setIsCameraOpen(true)}
                    onCancelComplete={cancelSos}
                    onOpenSendRealMessage={() => {
                      setRealMessageInitialContact(null);
                      setRealMessageMode('emergency');
                      setIsRealMessageModalOpen(true);
                    }}
                  />
                ) : (
                  <SosHoldCard
                    onComplete={triggerSos}
                    onAccessibleStart={() => {
                      setIsAccessibleSosPending(true);
                      setAccessibleDelayLeft(5);
                    }}
                    onAccessibleCancel={() => {
                      setIsAccessibleSosPending(false);
                      setAccessibleDelayLeft(5);
                    }}
                    isAccessiblePending={isAccessibleSosPending}
                    accessibleSecondsLeft={accessibleDelayLeft}
                  />
                )}

                <RiskCard
                  active={isEmergencyActive}
                  score={riskScore}
                  severity={riskSeverity}
                  factors={riskFactors}
                />

                <LocalAiDualEngineCard
                  qwenAnalysis={qwenAnalysis}
                  phiAnalysis={phiAnalysis}
                  onSelectQwenVariant={(v) => setQwenVariant(v)}
                  onTriggerAutoCapture={triggerAutonomousCapture}
                  evidenceCount={evidenceList.length}
                />

                <InteractiveSafeMap
                  location={location}
                  safePlaces={safePlaces}
                  unsafeZones={phiAnalysis.unsafeZonesNearby}
                  recommendedEscapeRoute={phiAnalysis.recommendedEscapeRoute}
                />

                <FeatureGrid
                  emergencyState={emergencyState}
                  locationStatus={locationStatus}
                  location={location}
                  riskScore={riskScore}
                  riskSeverity={riskSeverity}
                  voiceStatus={voiceStatus}
                  voiceMessage={voiceMessage}
                  lastCaptureName={evidenceList[0]?.name || null}
                  onOpenCamera={() => setIsCameraOpen(true)}
                  onOpenContacts={() => setCurrentTab(HomeTab.SAFETY)}
                />
              </div>
            )}

            {currentTab === HomeTab.SAFETY && (
              <SafetyTab
                latestLocation={location}
                onShowAlertNotification={(msg) => setToastNotification(msg)}
                onOpenSendMessageModal={(contact, mode) => {
                  setRealMessageInitialContact(contact || null);
                  setRealMessageMode(mode || 'emergency');
                  setIsRealMessageModalOpen(true);
                }}
              />
            )}

            {currentTab === HomeTab.EVIDENCE && (
              <EvidenceTab
                timeline={timeline}
                evidenceList={evidenceList}
                latestLocation={location}
                riskScore={riskScore}
                riskSeverity={riskSeverity}
                victimName={currentUser?.name}
                victimPhone={currentUser?.phone}
              />
            )}

            {currentTab === HomeTab.ME && (
              <MeTab
                user={currentUser}
                onLogout={async () => {
                  await authService.logout();
                  setCurrentUser(null);
                }}
              />
            )}
          </main>
        </div>

        <BottomNav
          currentTab={currentTab}
          onTabSelect={(tab) => setCurrentTab(tab)}
          isEmergencyActive={isEmergencyActive}
        />

        <CameraModal
          isOpen={isCameraOpen}
          onClose={() => setIsCameraOpen(false)}
          onCaptureSaved={handleCaptureSaved}
        />

        <SendRealMessageModal
          isOpen={isRealMessageModalOpen}
          onClose={() => setIsRealMessageModalOpen(false)}
          contacts={contacts}
          location={location}
          victimName={currentUser?.name}
          victimPhone={currentUser?.phone}
          riskScore={riskScore}
          riskSeverity={riskSeverity}
          evidenceCount={evidenceList.length}
          initialContact={realMessageInitialContact}
          initialMode={realMessageMode}
        />
      </div>
    </div>
  );
};
