import { EvidenceCapture, LocationPoint } from '../types';

/**
 * Normalizes phone numbers for WhatsApp API (wa.me and api.whatsapp.com).
 * Strips formatting characters (spaces, dashes, parentheses, plus signs)
 * leaving only digits.
 */
export function formatWhatsAppPhone(phone: string): string {
  const digitsOnly = phone.replace(/[^\d]/g, '');
  return digitsOnly;
}

export interface WhatsAppEmergencyParams {
  victimName: string;
  victimPhone: string;
  location: LocationPoint | null;
  aiSummary?: string;
  evidenceList: EvidenceCapture[];
  isTimerExpired?: boolean;
}

/**
 * Builds a structured, rich WhatsApp emergency distress message including
 * victim identity, live Google Maps link, Phi-3.5 Mini AI narrative ("What Happened"),
 * and full cryptographic SHA-256 evidence digests.
 */
export function buildWhatsAppEmergencyMessage({
  victimName,
  victimPhone,
  location,
  aiSummary,
  evidenceList,
  isTimerExpired = false,
}: WhatsAppEmergencyParams): string {
  const locCoords = location
    ? `https://maps.google.com/?q=${location.latitude},${location.longitude}\n• Coords: ${location.latitude.toFixed(5)}, ${location.longitude.toFixed(5)}${location.accuracyMeters ? ` (±${Math.round(location.accuracyMeters)}m)` : ''}`
    : 'GPS coordinates currently acquiring/pending';

  const narrative =
    aiSummary ||
    'Emergency SOS triggered via 5-second continuous hardware safety hold. Protection mode active.';

  const evidenceBlocks =
    evidenceList.length > 0
      ? evidenceList
          .map(
            (ev, i) =>
              `  [${i + 1}] *${ev.name}*\n       • Time: ${new Date(ev.timestampMillis).toLocaleTimeString()}\n       • SHA-256: \`${ev.sha256Hash}\`\n       • Size: ${(ev.fileSizeBytes / 1024).toFixed(1)} KB`
          )
          .join('\n')
      : '  • No photographic snapshots captured yet';

  return [
    '🚨 *SafeRescue CRITICAL SOS ALERT*',
    isTimerExpired
      ? '⚠️ *STATUS: 2-Minute Safety Countdown Expired — Immediate Assistance Required*'
      : '⚠️ *STATUS: Emergency SOS Active*',
    '',
    `👤 *Victim Details:*`,
    `• Name: ${victimName || 'SafeRescue User'}`,
    `• Contact Tel: ${victimPhone || 'Not specified'}`,
    '',
    `📍 *Live GPS Location:*`,
    `• Pin: ${locCoords}`,
    '',
    `🤖 *What Happened (Phi-3.5 Mini 3.8B AI Analysis):*`,
    narrative,
    '',
    `📸 *Cryptographic Evidence Dossier (${evidenceList.length} snapshot${evidenceList.length === 1 ? '' : 's'}):*`,
    evidenceBlocks,
    '',
    '🔒 *Tamper-Proof Chain of Custody:*',
    'All evidence hashes are computed on-device with SHA-256 and sealed in the local audit ledger for law enforcement and legal verification.',
    '',
    '🆘 *Please dispatch emergency responders or reach out immediately!*',
  ].join('\n');
}

/**
 * Builds a single evidence photograph WhatsApp dispatch message.
 */
export function buildWhatsAppSingleEvidenceMessage(
  evidence: EvidenceCapture,
  location: LocationPoint | null,
  victimName: string
): string {
  const locPin = location
    ? `https://maps.google.com/?q=${location.latitude},${location.longitude}`
    : 'Not attached';

  return [
    '📸 *SafeRescue Cryptographic Evidence Dispatch*',
    '',
    `👤 *Victim:* ${victimName || 'SafeRescue User'}`,
    `📁 *Evidence File:* ${evidence.name}`,
    `⏰ *Timestamp:* ${new Date(evidence.timestampMillis).toLocaleString()}`,
    `📦 *File Size:* ${(evidence.fileSizeBytes / 1024).toFixed(1)} KB (${evidence.mimeType})`,
    '',
    `🔒 *Cryptographic SHA-256 Digest:*`,
    `\`${evidence.sha256Hash}\``,
    '',
    `📍 *GPS Coordinates:* ${locPin}`,
    '',
    '🛡️ *Sealed on-device by SafeRescue Dual-Model Local AI (Phi-3.5 Mini & Qwen 2.5).*',
    'This cryptographic signature proves the photo has not been altered or tampered with.',
  ].join('\n');
}

/**
 * Constructs a direct WhatsApp URL to message a specific phone number.
 */
export function getWhatsAppDirectUrl(phone: string, message: string): string {
  const cleanPhone = formatWhatsAppPhone(phone);
  return `https://api.whatsapp.com/send?phone=${cleanPhone}&text=${encodeURIComponent(message)}`;
}

/**
 * Constructs a WhatsApp share URL that prompts the user to select contacts or groups.
 */
export function getWhatsAppShareUrl(message: string): string {
  return `https://api.whatsapp.com/send?text=${encodeURIComponent(message)}`;
}

/**
 * Dispatches a WhatsApp message to a specific phone number.
 */
export function openWhatsAppDirect(phone: string, message: string): void {
  const url = getWhatsAppDirectUrl(phone, message);
  if (typeof window !== 'undefined') {
    window.open(url, '_blank', 'noopener,noreferrer');
  }
}

/**
 * Dispatches a WhatsApp message via universal share dialog (groups or any contact).
 */
export function openWhatsAppShare(message: string): void {
  const url = getWhatsAppShareUrl(message);
  if (typeof window !== 'undefined') {
    window.open(url, '_blank', 'noopener,noreferrer');
  }
}
