import { LocationPoint } from '../types';

export interface EmergencyMessageParams {
  victimName?: string;
  victimPhone?: string;
  location?: LocationPoint | null;
  riskScore?: number;
  riskSeverity?: string;
  evidenceCount?: number;
  customNote?: string;
  isTest?: boolean;
}

export function formatEmergencyText(params: EmergencyMessageParams): string {
  const {
    victimName = 'SafeRescue User',
    victimPhone,
    location,
    riskScore,
    riskSeverity,
    evidenceCount = 0,
    customNote,
    isTest = false,
  } = params;

  const header = isTest
    ? '⚠️ [SafeRescue TEST ALERT] - Emergency System Check'
    : '🚨 SafeRescue CRITICAL EMERGENCY ALERT - NEED ASSISTANCE';

  const timeStr = new Date().toLocaleString();

  const gpsLine = location
    ? `📍 Live Location: https://maps.google.com/?q=${location.latitude.toFixed(6)},${location.longitude.toFixed(6)}\n(GPS Accuracy: ±${Math.round(location.accuracyMeters || 10)}m)`
    : '📍 Live Location: GPS coordinates pending / acquiring';

  const lines = [
    header,
    '',
    `👤 Sender: ${victimName}${victimPhone ? ` (${victimPhone})` : ''}`,
    `⏱️ Time: ${timeStr}`,
    gpsLine,
  ];

  if (riskScore !== undefined && riskSeverity) {
    lines.push(`⚡ Risk Level: ${riskSeverity.toUpperCase()} (${riskScore}/100)`);
  }

  if (evidenceCount > 0) {
    lines.push(`📸 Secured Evidences: ${evidenceCount} photo snapshot(s) sealed locally`);
  }

  if (customNote) {
    lines.push(`📝 Details: ${customNote}`);
  }

  if (!isTest) {
    lines.push('');
    lines.push('⚠️ This is an automated high-priority emergency SOS. Please call emergency services or check on me immediately.');
  } else {
    lines.push('');
    lines.push('✅ This was a test verification sent from SafeRescue. No action required.');
  }

  return lines.join('\n');
}

export function cleanPhoneNumberForSms(phone: string): string {
  // Retain + and digits
  return phone.replace(/[^\d+]/g, '');
}

export function cleanPhoneNumberForWhatsApp(phone: string): string {
  // WhatsApp wa.me requires country code and digits only (no +, no dashes, no spaces)
  return phone.replace(/\D/g, '');
}

export function sendSms(phone: string, text: string): boolean {
  try {
    const cleanPhone = cleanPhoneNumberForSms(phone);
    const smsUrl = `sms:${cleanPhone}?body=${encodeURIComponent(text)}`;
    window.location.href = smsUrl;
    return true;
  } catch (err) {
    console.error('Failed to trigger SMS handler:', err);
    return false;
  }
}

export function sendWhatsApp(phone: string, text: string): boolean {
  try {
    const cleanPhone = cleanPhoneNumberForWhatsApp(phone);
    const waUrl = cleanPhone
      ? `https://wa.me/${cleanPhone}?text=${encodeURIComponent(text)}`
      : `https://wa.me/?text=${encodeURIComponent(text)}`;
    window.open(waUrl, '_blank', 'noopener,noreferrer');
    return true;
  } catch (err) {
    console.error('Failed to open WhatsApp:', err);
    return false;
  }
}

export async function shareNative(text: string, title = 'SafeRescue Emergency SOS'): Promise<boolean> {
  if (typeof navigator !== 'undefined' && navigator.share) {
    try {
      await navigator.share({
        title,
        text,
      });
      return true;
    } catch (err) {
      if ((err as Error)?.name === 'AbortError') {
        return false;
      }
      console.warn('Native share failed:', err);
    }
  }
  return false;
}

export async function copyMessage(text: string): Promise<boolean> {
  if (typeof navigator !== 'undefined' && navigator.clipboard) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch (err) {
      console.warn('Clipboard write failed:', err);
    }
  }
  return false;
}
