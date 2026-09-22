export interface AuthUser {
  id: string;
  name: string;
  username: string;
  phone?: string;
  email?: string;
  role: string;
  authenticatedAtMillis: number;
}

export interface RegistrationInput {
  name: string;
  username: string;
  phone: string;
  email: string;
  password: string;
}

export type AuthScreen = 'login' | 'register' | 'register_otp' | 'reset' | 'reset_otp';

export enum HomeTab {
  HOME = 'HOME',
  SAFETY = 'SAFETY',
  EVIDENCE = 'EVIDENCE',
  ME = 'ME',
}

export enum EmergencyState {
  IDLE = 'IDLE',
  SOS_HOLDING = 'SOS_HOLDING',
  EMERGENCY_ACTIVE = 'EMERGENCY_ACTIVE',
  MONITORING = 'MONITORING',
  CANCELLATION_PENDING = 'CANCELLATION_PENDING',
  CANCELLED = 'CANCELLED',
  CONFIRMED = 'CONFIRMED',
  EVIDENCE_PRESERVATION = 'EVIDENCE_PRESERVATION',
  UPLOADING = 'UPLOADING',
  QUEUED_OFFLINE = 'QUEUED_OFFLINE',
  SYNCING = 'SYNCING',
  COMPLETED = 'COMPLETED',
  FAILED_RECOVERABLE = 'FAILED_RECOVERABLE',
  FAILED_FATAL = 'FAILED_FATAL',
}

export enum LocationStatus {
  INACTIVE = 'INACTIVE',
  INITIALIZING = 'INITIALIZING',
  ACTIVE = 'ACTIVE',
  PERMISSION_REQUIRED = 'PERMISSION_REQUIRED',
  UNAVAILABLE = 'UNAVAILABLE',
  ERROR = 'ERROR',
}

export interface LocationPoint {
  latitude: number;
  longitude: number;
  timestamp: number;
  accuracyMeters: number;
  altitude?: number | null;
  speed?: number | null;
}

export enum VoiceStatus {
  INACTIVE = 'INACTIVE',
  LISTENING = 'LISTENING',
  PERMISSION_REQUIRED = 'PERMISSION_REQUIRED',
  UNAVAILABLE = 'UNAVAILABLE',
  ERROR = 'ERROR',
  STOPPED = 'STOPPED',
}

export interface VoiceSignalMetrics {
  timestampMillis: number;
  durationSeconds: number;
  rmsDb: number;
  peakAmplitude: number;
  bytesCaptured: number;
}

export enum RiskSeverity {
  LOW = 'LOW',
  MODERATE = 'MODERATE',
  ELEVATED = 'ELEVATED',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

export interface RiskFactor {
  id: string;
  title: string;
  contribution: number;
  description: string;
}

export enum TimelineEventType {
  SYSTEM = 'SYSTEM',
  EMERGENCY_TRIGGERED = 'EMERGENCY_TRIGGERED',
  LOCATION_ACQUIRED = 'LOCATION_ACQUIRED',
  VOICE_RECORDING_STARTED = 'VOICE_RECORDING_STARTED',
  VOICE_METRIC_UPDATED = 'VOICE_METRIC_UPDATED',
  CAMERA_EVIDENCE_CAPTURED = 'CAMERA_EVIDENCE_CAPTURED',
  RISK_EVALUATED = 'RISK_EVALUATED',
  CANCELLATION_ATTEMPTED = 'CANCELLATION_ATTEMPTED',
  EMERGENCY_CANCELLED = 'EMERGENCY_CANCELLED',
  REPORT_GENERATED = 'REPORT_GENERATED',
  CONFIRMED_INCIDENT = 'CONFIRMED_INCIDENT',
  SMS_ALERT_SENT = 'SMS_ALERT_SENT',
}

export interface TimelineEvent {
  id: string;
  timestampMillis: number;
  type: TimelineEventType;
  title: string;
  detail: string;
  riskScore?: number | null;
}

export interface TrustedContact {
  id: string;
  name: string;
  phone: string;
  verified: boolean;
  relationship?: string;
  createdAtMillis: number;
}

export interface SafetyPlace {
  id: string;
  name: string;
  category: 'police' | 'hospital' | 'fire_station' | 'public_safe';
  distanceMeters?: number;
  address?: string;
  latitude: number;
  longitude: number;
}

export interface WeatherCondition {
  temperatureC: number;
  weatherCode: number;
  weatherDescription: string;
  windSpeedKmh: number;
  humidityPercent: number;
  isDay: boolean;
}

export interface EvidenceCapture {
  id: string;
  timestampMillis: number;
  type: 'image' | 'audio';
  dataUrl: string;
  sha256Hash: string;
  fileSizeBytes: number;
  mimeType: string;
  name: string;
}

export interface IncidentReport {
  incidentId: string;
  generatedAtMillis: number;
  finalRiskScore: number;
  finalRiskSeverity: RiskSeverity;
  timeline: TimelineEvent[];
  evidenceCount: number;
  locationAvailable: boolean;
  latestLocation?: LocationPoint | null;
  voiceMonitoringUsed: boolean;
}

export interface QwenContextAnalysis {
  modelName: 'Qwen 2.5 1.5B' | 'Qwen 2.5 3B';
  timestampMillis: number;
  situationalSummary: string;
  threatLevel: RiskSeverity;
  threatProbability: number; // 0.0 to 1.0
  identifiedSignals: string[];
  hypothesis: string;
  escalateToPhi: boolean;
  inferenceLatencyMs: number;
}

export interface UnsafeZone {
  id: string;
  name: string;
  category: 'unlit_alley' | 'isolated_area' | 'high_incident' | 'blind_underpass';
  latitude: number;
  longitude: number;
  radiusMeters: number;
  riskSeverity: 'CRITICAL' | 'HIGH' | 'MODERATE';
  reason: string;
}

export interface Phi35IncidentEvaluation {
  modelName: 'Phi-3.5 Mini 3.8B';
  timestampMillis: number;
  incidentConfirmed: boolean;
  confidenceScore: number; // 0-100
  whatHappenedNarrative: string;
  autoEvidenceAction: {
    shouldCapture: boolean;
    reason: string;
    capturedCount: number;
  };
  unsafeZonesNearby: UnsafeZone[];
  safePlacesNearby: SafetyPlace[];
  recommendedEscapeRoute?: {
    destinationName: string;
    distanceMeters: number;
    estimatedWalkMinutes: number;
    guidanceStep: string;
  } | null;
  inferenceLatencyMs: number;
}
