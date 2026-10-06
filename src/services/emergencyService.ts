import { supabase } from '../lib/supabase';

interface DbEmergency {
  id: string;
  user_id: string;
  state: string;
  risk_score: number;
  risk_severity: string;
  sms_report: string | null;
  latitude: number | null;
  longitude: number | null;
  evidence_count: number;
  started_at: string;
  ended_at: string | null;
  created_at: string;
}

interface CreateEmergencyInput {
  state: string;
  riskScore: number;
  riskSeverity: string;
  latitude?: number | null;
  longitude?: number | null;
}

interface CompleteEmergencyInput {
  id: string;
  state: string;
  riskScore: number;
  riskSeverity: string;
  smsReport?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  evidenceCount?: number;
}

class EmergencyService {
  async createEmergency(input: CreateEmergencyInput): Promise<string | null> {
    const { data, error } = await supabase
      .from('emergencies')
      .insert({
        state: input.state,
        risk_score: input.riskScore,
        risk_severity: input.riskSeverity,
        latitude: input.latitude ?? null,
        longitude: input.longitude ?? null,
      })
      .select('id')
      .maybeSingle();

    if (error) {
      console.warn('Failed to create emergency record:', error.message);
      return null;
    }

    return data?.id ?? null;
  }

  async completeEmergency(input: CompleteEmergencyInput): Promise<void> {
    const { error } = await supabase
      .from('emergencies')
      .update({
        state: input.state,
        risk_score: input.riskScore,
        risk_severity: input.riskSeverity,
        sms_report: input.smsReport ?? null,
        latitude: input.latitude ?? null,
        longitude: input.longitude ?? null,
        evidence_count: input.evidenceCount ?? 0,
        ended_at: new Date().toISOString(),
      })
      .eq('id', input.id);

    if (error) {
      console.warn('Failed to update emergency record:', error.message);
    }
  }

  async loadHistory(): Promise<DbEmergency[]> {
    const { data, error } = await supabase
      .from('emergencies')
      .select('*')
      .order('started_at', { ascending: false })
      .limit(20);

    if (error) {
      console.warn('Failed to load emergency history:', error.message);
      return [];
    }

    return (data as DbEmergency[]) || [];
  }
}

export const emergencyService = new EmergencyService();
