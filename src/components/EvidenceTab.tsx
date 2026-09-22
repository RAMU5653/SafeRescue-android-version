import React, { useState } from 'react';
import {
  FileText,
  Download,
  ShieldCheck,
  Camera,
  Clock,
  ExternalLink,
  ChevronRight,
  AlertCircle,
  Hash,
  Share2,
  Send,
  UserCheck,
} from 'lucide-react';
import { EvidenceCapture, IncidentReport, LocationPoint, RiskSeverity, TimelineEvent } from '../types';
import { generateIncidentPdf } from '../services/reportService';
import {
  buildWhatsAppEmergencyMessage,
  buildWhatsAppSingleEvidenceMessage,
  openWhatsAppDirect,
  openWhatsAppShare,
} from '../services/whatsappService';
import { contactsService } from '../services/contactsService';

interface EvidenceTabProps {
  timeline: TimelineEvent[];
  evidenceList: EvidenceCapture[];
  latestLocation: LocationPoint | null;
  riskScore: number;
  riskSeverity: RiskSeverity;
  victimName?: string;
  victimPhone?: string;
  onClearIncident?: () => void;
}

export const EvidenceTab: React.FC<EvidenceTabProps> = ({
  timeline,
  evidenceList,
  latestLocation,
  riskScore,
  riskSeverity,
  victimName = 'Venkata Ram',
  victimPhone = '+1 555-0199',
  onClearIncident,
}) => {
  const [selectedImage, setSelectedImage] = useState<EvidenceCapture | null>(null);
  const [generatingPdf, setGeneratingPdf] = useState(false);
  const [downloadSuccess, setDownloadSuccess] = useState<string | null>(null);
  const [whatsAppSuccess, setWhatsAppSuccess] = useState<string | null>(null);

  const contacts = contactsService.getContacts();

  const handleShareAllWhatsApp = () => {
    const msg = buildWhatsAppEmergencyMessage({
      victimName,
      victimPhone,
      location: latestLocation,
      evidenceList,
      aiSummary: 'Evidence dossier dispatched directly from SafeRescue Evidence Locker.',
      isTimerExpired: false,
    });
    openWhatsAppShare(msg);
    setWhatsAppSuccess('WhatsApp opened with complete evidence dossier!');
    setTimeout(() => setWhatsAppSuccess(null), 3500);
  };

  const handleShareSingleWhatsApp = (item: EvidenceCapture, contactPhone?: string) => {
    const msg = buildWhatsAppSingleEvidenceMessage(item, latestLocation, victimName);
    if (contactPhone) {
      openWhatsAppDirect(contactPhone, msg);
    } else {
      openWhatsAppShare(msg);
    }
    setWhatsAppSuccess(`Evidence photo "${item.name}" shared to WhatsApp!`);
    setTimeout(() => setWhatsAppSuccess(null), 3500);
  };

  const handleDownloadPdf = () => {
    setGeneratingPdf(true);
    try {
      const report: IncidentReport = {
        incidentId: `INC-${Date.now().toString().slice(-6)}`,
        generatedAtMillis: Date.now(),
        finalRiskScore: riskScore,
        finalRiskSeverity: riskSeverity,
        timeline,
        evidenceCount: evidenceList.length,
        locationAvailable: Boolean(latestLocation),
        latestLocation,
        voiceMonitoringUsed: true,
      };

      const { blob, filename } = generateIncidentPdf(report);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      setDownloadSuccess(`Generated ${filename}`);
      setTimeout(() => setDownloadSuccess(null), 4000);
    } catch (err) {
      console.error('PDF export error:', err);
    } finally {
      setGeneratingPdf(false);
    }
  };

  return (
    <div className="space-y-4 pb-20">
      <div>
        <h2 className="text-2xl font-extrabold text-white">Evidence & Reports</h2>
        <p className="text-xs text-[#8E9BB6] mt-0.5">
          Phase 10 & 15 • Local tamper-evident incident log and downloadable PDF documentation.
        </p>
      </div>

      {/* PDF Generation Card */}
      <div className="rounded-[20px] p-5 bg-white text-[#17172A] shadow-md space-y-3">
        <div className="flex items-center justify-between gap-2 flex-wrap">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-[#F1EFFF] flex items-center justify-center text-[#5B4BDB]">
              <FileText className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-sm text-[#222236]">Incident Dossier & Evidence</h4>
              <p className="text-xs text-[#777788]">Printable PDF & direct WhatsApp dispatch</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              id="btn-whatsapp-export"
              onClick={handleShareAllWhatsApp}
              disabled={evidenceList.length === 0}
              className="px-3.5 py-2 rounded-xl bg-[#25D366] hover:bg-[#20ba59] text-[#0A2613] text-xs font-bold flex items-center gap-1.5 transition-all shadow-sm disabled:opacity-40"
            >
              <svg className="w-3.5 h-3.5 fill-current shrink-0" viewBox="0 0 24 24">
                <path d="M12.031 6.172c-3.181 0-5.767 2.586-5.768 5.766-.001 1.298.38 2.27 1.019 3.287l-.582 2.128 2.182-.573c.978.58 1.911.928 3.145.929 3.178 0 5.767-2.587 5.768-5.766 0-3.18-2.586-5.771-5.764-5.771zm3.392 8.244c-.144.405-.837.774-1.17.824-.299.045-.677.063-1.092-.069-.252-.08-.575-.187-.988-.365-1.739-.751-2.874-2.502-2.961-2.617-.087-.116-.708-.94-.708-1.793s.448-1.273.607-1.446c.159-.173.346-.217.462-.217l.332.006c.106.005.249-.04.39.299.144.346.491 1.2.534 1.288.043.087.072.188.014.304-.058.116-.087.188-.173.289l-.26.304c-.087.086-.177.18-.076.353.101.173.45 1.743 1.968 2.072.195.042.311.02.427-.037.116-.058.491-.573.621-.769.13-.196.26-.163.433-.101.173.062 1.097.517 1.285.611.188.094.314.139.36.219.045.08.045.464-.099.869z" />
              </svg>
              <span>Send to WhatsApp</span>
            </button>

            <button
              type="button"
              id="btn-generate-pdf"
              onClick={handleDownloadPdf}
              disabled={generatingPdf}
              className="px-3.5 py-2 rounded-xl bg-[#5B4BDB] hover:bg-[#4838c4] text-white text-xs font-bold flex items-center gap-1.5 transition-colors shadow-sm disabled:opacity-50"
            >
              <Download className="w-3.5 h-3.5" />
              {generatingPdf ? '...' : 'PDF'}
            </button>
          </div>
        </div>

        <p className="text-[11px] text-[#777788] leading-relaxed">
          Compiles all timestamped events, acoustic signals, GPS fix coordinates, and SHA-256
          evidence fingerprints into a structured, tamper-evident document or instant WhatsApp distress payload.
        </p>

        {whatsAppSuccess && (
          <div className="p-2.5 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-center gap-2 font-medium">
            <svg className="w-4 h-4 fill-emerald-600 shrink-0" viewBox="0 0 24 24">
              <path d="M12.031 6.172c-3.181 0-5.767 2.586-5.768 5.766-.001 1.298.38 2.27 1.019 3.287l-.582 2.128 2.182-.573c.978.58 1.911.928 3.145.929 3.178 0 5.767-2.587 5.768-5.766 0-3.18-2.586-5.771-5.764-5.771zm3.392 8.244c-.144.405-.837.774-1.17.824-.299.045-.677.063-1.092-.069-.252-.08-.575-.187-.988-.365-1.739-.751-2.874-2.502-2.961-2.617-.087-.116-.708-.94-.708-1.793s.448-1.273.607-1.446c.159-.173.346-.217.462-.217l.332.006c.106.005.249-.04.39.299.144.346.491 1.2.534 1.288.043.087.072.188.014.304-.058.116-.087.188-.173.289l-.26.304c-.087.086-.177.18-.076.353.101.173.45 1.743 1.968 2.072.195.042.311.02.427-.037.116-.058.491-.573.621-.769.13-.196.26-.163.433-.101.173.062 1.097.517 1.285.611.188.094.314.139.36.219.045.08.045.464-.099.869z" />
            </svg>
            <span>{whatsAppSuccess}</span>
          </div>
        )}

        {downloadSuccess && (
          <div className="p-2.5 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-center gap-2 font-medium">
            <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{downloadSuccess} downloaded successfully.</span>
          </div>
        )}
      </div>

      {/* Captured Evidence Gallery */}
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <Camera className="w-4 h-4 text-[#72D8FF]" /> Captured Evidence ({evidenceList.length})
          </h3>
          <span className="text-[11px] text-[#8E9BB6]">SHA-256 Verified</span>
        </div>

        {evidenceList.length === 0 ? (
          <div className="rounded-[18px] p-6 bg-[#0D2140] text-center text-white border border-white/5 space-y-2">
            <Camera className="w-8 h-8 text-[#55D7FF] mx-auto opacity-70" />
            <h5 className="font-bold text-sm">No Evidence Snapshots Yet</h5>
            <p className="text-xs text-[#B8C4D9]">
              Activate SOS and launch the Camera Evidence lens to record cryptographic proof.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-2.5">
            {evidenceList.map((item) => (
              <div
                key={item.id}
                onClick={() => setSelectedImage(item)}
                className="rounded-[16px] overflow-hidden bg-[#0D2140] border border-white/10 hover:border-[#72D8FF]/50 transition-all cursor-pointer group"
              >
                <div className="aspect-4/3 bg-black relative">
                  <img
                    src={item.dataUrl}
                    alt={item.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                  <div className="absolute top-1.5 right-1.5 px-2 py-0.5 rounded bg-black/70 text-[9px] font-mono text-emerald-300 flex items-center gap-1">
                    <ShieldCheck className="w-2.5 h-2.5" /> SHA256
                  </div>
                </div>
                <div className="p-2.5">
                  <span className="text-xs font-bold text-white block truncate">{item.name}</span>
                  <span className="text-[10px] text-[#8E9BB6] block mt-0.5 font-mono truncate">
                    Hash: {item.sha256Hash.slice(0, 16)}...
                  </span>
                  <div className="flex items-center justify-between mt-1.5 pt-1.5 border-t border-white/5">
                    <span className="text-[10px] text-[#72D8FF]">
                      {new Date(item.timestampMillis).toLocaleTimeString()}
                    </span>
                    <button
                      type="button"
                      title="Send this evidence photo to WhatsApp"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleShareSingleWhatsApp(item);
                      }}
                      className="px-2 py-0.5 rounded bg-[#25D366]/20 hover:bg-[#25D366]/30 text-[#25D366] text-[10px] font-bold flex items-center gap-1 border border-[#25D366]/30 transition-colors"
                    >
                      <svg className="w-3 h-3 fill-current shrink-0" viewBox="0 0 24 24">
                        <path d="M12.031 6.172c-3.181 0-5.767 2.586-5.768 5.766-.001 1.298.38 2.27 1.019 3.287l-.582 2.128 2.182-.573c.978.58 1.911.928 3.145.929 3.178 0 5.767-2.587 5.768-5.766 0-3.18-2.586-5.771-5.764-5.771zm3.392 8.244c-.144.405-.837.774-1.17.824-.299.045-.677.063-1.092-.069-.252-.08-.575-.187-.988-.365-1.739-.751-2.874-2.502-2.961-2.617-.087-.116-.708-.94-.708-1.793s.448-1.273.607-1.446c.159-.173.346-.217.462-.217l.332.006c.106.005.249-.04.39.299.144.346.491 1.2.534 1.288.043.087.072.188.014.304-.058.116-.087.188-.173.289l-.26.304c-.087.086-.177.18-.076.353.101.173.45 1.743 1.968 2.072.195.042.311.02.427-.037.116-.058.491-.573.621-.769.13-.196.26-.163.433-.101.173.062 1.097.517 1.285.611.188.094.314.139.36.219.045.08.045.464-.099.869z" />
                      </svg>
                      <span>WhatsApp</span>
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Chronological Incident Timeline */}
      <div className="space-y-2 pt-2">
        <h3 className="text-base font-bold text-white flex items-center gap-2">
          <Clock className="w-4 h-4 text-[#FFA7BF]" /> Incident Timeline ({timeline.length})
        </h3>

        <div className="rounded-[20px] p-4 bg-white text-[#17172A] shadow-sm space-y-3">
          {timeline.length === 0 ? (
            <p className="text-xs text-[#777788] italic py-2 text-center">
              No incident events logged in current session. Trigger an SOS hold to initiate logging.
            </p>
          ) : (
            <div className="relative pl-5 space-y-4 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-zinc-200">
              {timeline.map((event) => (
                <div key={event.id} className="relative text-xs">
                  {/* Dot */}
                  <div className="absolute -left-5 top-1 w-2.5 h-2.5 rounded-full bg-[#5B4BDB] ring-4 ring-white" />
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-zinc-900">{event.title}</span>
                    <span className="text-[10px] text-zinc-400 font-mono">
                      {new Date(event.timestampMillis).toLocaleTimeString()}
                    </span>
                  </div>
                  <p className="text-[11px] text-zinc-600 mt-0.5 leading-relaxed">{event.detail}</p>
                  {event.riskScore != null && (
                    <span className="inline-block mt-1 text-[10px] font-bold text-rose-700 bg-rose-50 px-1.5 py-0.5 rounded border border-rose-200">
                      Risk: {event.riskScore}/100
                    </span>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Image Preview Modal */}
      {selectedImage && (
        <div className="fixed inset-0 z-50 bg-black/90 flex flex-col items-center justify-center p-4 backdrop-blur-md">
          <div className="w-full max-w-lg bg-[#07152C] rounded-[24px] overflow-hidden border border-white/10 shadow-2xl flex flex-col">
            <div className="p-3 bg-[#050D20] flex items-center justify-between border-b border-white/10">
              <span className="text-xs font-bold text-white truncate">{selectedImage.name}</span>
              <button
                type="button"
                onClick={() => setSelectedImage(null)}
                className="px-3 py-1 rounded-lg bg-white/10 text-white text-xs font-bold hover:bg-white/20"
              >
                Close
              </button>
            </div>
            <div className="max-h-[60vh] overflow-hidden bg-black flex items-center justify-center">
              <img
                src={selectedImage.dataUrl}
                alt={selectedImage.name}
                className="max-h-full max-w-full object-contain"
              />
            </div>
            <div className="p-4 bg-[#050D20] text-xs space-y-1.5">
              <div className="flex items-center gap-1.5 text-[#55D7FF]">
                <Hash className="w-3.5 h-3.5" />
                <span className="font-bold">Cryptographic SHA-256 Digest:</span>
              </div>
              <p className="font-mono text-[10px] text-zinc-300 break-all bg-black/50 p-2 rounded-lg border border-white/5">
                {selectedImage.sha256Hash}
              </p>
              <div className="flex items-center justify-between text-[11px] text-[#8E9BB6] pt-1">
                <span>
                  Captured: {new Date(selectedImage.timestampMillis).toLocaleString()}
                </span>
                <span>Size: {(selectedImage.fileSizeBytes / 1024).toFixed(1)} KB</span>
              </div>

              {/* WhatsApp Share in Modal */}
              <div className="pt-2 border-t border-white/10 space-y-2">
                <button
                  type="button"
                  id="btn-modal-whatsapp-share"
                  onClick={() => handleShareSingleWhatsApp(selectedImage)}
                  className="w-full py-2.5 px-3 rounded-xl bg-[#25D366] hover:bg-[#20ba59] text-[#0A2613] text-xs font-bold flex items-center justify-center gap-2 transition-all shadow-md"
                >
                  <svg className="w-4 h-4 fill-current shrink-0" viewBox="0 0 24 24">
                    <path d="M12.031 6.172c-3.181 0-5.767 2.586-5.768 5.766-.001 1.298.38 2.27 1.019 3.287l-.582 2.128 2.182-.573c.978.58 1.911.928 3.145.929 3.178 0 5.767-2.587 5.768-5.766 0-3.18-2.586-5.771-5.764-5.771zm3.392 8.244c-.144.405-.837.774-1.17.824-.299.045-.677.063-1.092-.069-.252-.08-.575-.187-.988-.365-1.739-.751-2.874-2.502-2.961-2.617-.087-.116-.708-.94-.708-1.793s.448-1.273.607-1.446c.159-.173.346-.217.462-.217l.332.006c.106.005.249-.04.39.299.144.346.491 1.2.534 1.288.043.087.072.188.014.304-.058.116-.087.188-.173.289l-.26.304c-.087.086-.177.18-.076.353.101.173.45 1.743 1.968 2.072.195.042.311.02.427-.037.116-.058.491-.573.621-.769.13-.196.26-.163.433-.101.173.062 1.097.517 1.285.611.188.094.314.139.36.219.045.08.045.464-.099.869z" />
                  </svg>
                  <span>Dispatch Evidence & SHA-256 to WhatsApp</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </button>

                {contacts.length > 0 && (
                  <div className="flex flex-wrap items-center gap-1.5 pt-1">
                    <span className="text-[10px] text-zinc-400">Direct to Contact:</span>
                    {contacts.map((c) => (
                      <button
                        key={c.id}
                        type="button"
                        onClick={() => handleShareSingleWhatsApp(selectedImage, c.phone)}
                        className="px-2 py-0.5 rounded bg-white/10 hover:bg-white/20 text-[10px] text-white font-medium flex items-center gap-1 transition-colors"
                      >
                        <UserCheck className="w-2.5 h-2.5 text-[#25D366]" />
                        <span>{c.name}</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
