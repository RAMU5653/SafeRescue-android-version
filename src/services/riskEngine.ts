import { LocationPoint, LocationStatus, RiskFactor, RiskSeverity, VoiceSignalMetrics } from '../types';

export interface RiskInput {
  emergencyActive: boolean;
  locationStatus: LocationStatus;
  location: LocationPoint | null;
  voiceMetrics: VoiceSignalMetrics | null;
  voiceStatus: string;
}

export interface RiskEvaluation {
  score: number;
  severity: RiskSeverity;
  factors: RiskFactor[];
  updatedAtMillis: number;
}

export class RiskEngine {
  evaluate(input: RiskInput, nowMillis: number = Date.now()): RiskEvaluation {
    if (!input.emergencyActive) {
      return {
        score: 0,
        severity: RiskSeverity.LOW,
        factors: [],
        updatedAtMillis: nowMillis,
      };
    }

    const factors: RiskFactor[] = [];

    // Factor 1: Manual SOS
    factors.push({
      id: 'manual_sos',
      title: 'Manual SOS Active',
      contribution: 40,
      description: 'A user-triggered emergency state is currently active.',
    });

    // Factor 2: Location availability / accuracy
    if (input.locationStatus === LocationStatus.UNAVAILABLE || input.locationStatus === LocationStatus.ERROR) {
      factors.push({
        id: 'location_unavailable',
        title: 'Location Unavailable',
        contribution: 8,
        description: 'GPS location fix is currently unavailable on this device.',
      });
    } else if (input.locationStatus === LocationStatus.PERMISSION_REQUIRED) {
      factors.push({
        id: 'location_permission',
        title: 'Location Permission Needed',
        contribution: 5,
        description: 'Location access was not granted by the browser/system.',
      });
    } else if (input.locationStatus === LocationStatus.ACTIVE && input.location) {
      if (input.location.accuracyMeters > 100) {
        factors.push({
          id: 'location_accuracy',
          title: 'Low Location Precision',
          contribution: 4,
          description: `Fix accuracy is ±${Math.round(input.location.accuracyMeters)}m.`,
        });
      }
    }

    // Factor 3: Voice / Acoustic signals
    if (input.voiceMetrics) {
      const { rmsDb, peakAmplitude } = input.voiceMetrics;
      let loudnessPoints = 0;
      if (rmsDb >= -12) {
        loudnessPoints = 15;
      } else if (rmsDb >= -20) {
        loudnessPoints = 10;
      } else if (rmsDb >= -35) {
        loudnessPoints = 5;
      }

      if (loudnessPoints > 0) {
        factors.push({
          id: 'voice_signal',
          title: 'Elevated Audio Volume',
          contribution: loudnessPoints,
          description: `Microphone signal is elevated (${rmsDb.toFixed(1)} dB).`,
        });
      }

      let peakPoints = 0;
      if (peakAmplitude >= 30000) {
        peakPoints = 5;
      } else if (peakAmplitude >= 22000) {
        peakPoints = 3;
      } else if (peakAmplitude >= 12000) {
        peakPoints = 2;
      }

      if (peakPoints > 0) {
        factors.push({
          id: 'voice_peak',
          title: 'Acoustic Peak Spike',
          contribution: peakPoints,
          description: 'Recent microphone audio waveform detected sudden peak.',
        });
      }
    }

    // Factor 4: Night-time situational risk heuristic (22:00 to 05:00)
    const currentHour = new Date(nowMillis).getHours();
    if (currentHour >= 22 || currentHour < 5) {
      factors.push({
        id: 'night_time',
        title: 'Night-time Window',
        contribution: 10,
        description: 'Incident initiated during low visibility night hours.',
      });
    }

    const score = Math.min(100, Math.max(0, factors.reduce((sum, f) => sum + f.contribution, 0)));

    return {
      score,
      severity: this.severityFor(score),
      factors,
      updatedAtMillis: nowMillis,
    };
  }

  severityFor(score: number): RiskSeverity {
    if (score < 25) return RiskSeverity.LOW;
    if (score < 50) return RiskSeverity.MODERATE;
    if (score < 75) return RiskSeverity.HIGH;
    return RiskSeverity.CRITICAL;
  }
}

export const riskEngine = new RiskEngine();
