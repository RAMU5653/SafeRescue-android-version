import React, { useState } from 'react';
import {
  MessageSquare,
  Send,
  Copy,
  CheckCircle2,
  Share2,
  ExternalLink,
  ShieldCheck,
  Camera,
  AlertTriangle,
  UserCheck,
} from 'lucide-react';
import { EvidenceCapture, TrustedContact } from '../types';
import {
  getWhatsAppDirectUrl,
  openWhatsAppDirect,
  openWhatsAppShare,
} from '../services/whatsappService';

interface DispatchedEmergencyDossierCardProps {
  smsMessage: string;
  whatsAppMessage: string;
  contacts: TrustedContact[];
  evidenceList: EvidenceCapture[];
  onShowToast: (msg: string) => void;
}

export const DispatchedEmergencyDossierCard: React.FC<DispatchedEmergencyDossierCardProps> = ({
  smsMessage,
  whatsAppMessage,
  contacts,
  evidenceList,
  onShowToast,
}) => {
  const [activeChannel, setActiveChannel] = useState<'whatsapp' | 'sms'>('whatsapp');
  const [copied, setCopied] = useState(false);

  const handleCopy = (text: string, label: string) => {
    navigator.clipboard?.writeText(text);
    setCopied(true);
    onShowToast(`${label} copied to clipboard!`);
    setTimeout(() => setCopied(false), 2500);
  };

  const handleOpenWhatsAppAll = () => {
    openWhatsAppShare(whatsAppMessage);
    onShowToast('Opening WhatsApp dispatch...');
  };

  const handleSendContactWhatsApp = (contact: TrustedContact) => {
    openWhatsAppDirect(contact.phone, whatsAppMessage);
    onShowToast(`Dispatching emergency evidence to ${contact.name} on WhatsApp...`);
  };

  const handleResendSms = () => {
    if (contacts.length > 0) {
      const phones = contacts.map((c) => c.phone).join(',');
      window.location.href = `sms:${phones}?body=${encodeURIComponent(smsMessage)}`;
    } else {
      window.location.href = `sms:?body=${encodeURIComponent(smsMessage)}`;
    }
  };

  return (
    <div
      id="dispatched-emergency-dossier-card"
      className="rounded-[24px] bg-[#150a15] border-2 border-[#E51E4D]/60 p-5 shadow-[0_0_30px_rgba(229,30,77,0.25)] text-white space-y-4 animate-in fade-in"
    >
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-1 border-b border-white/10">
        <div className="flex items-center gap-2.5">
          <span className="relative flex h-3 w-3">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#25D366] opacity-75" />
            <span className="relative inline-flex rounded-full h-3 w-3 bg-[#25D366]" />
          </span>
          <div>
            <h4 className="text-sm font-black uppercase tracking-wider text-white flex items-center gap-2">
              🚨 Evidence Dispatched (WhatsApp & SMS)
            </h4>
            <p className="text-[11px] text-[#FFA7BF]">
              2-minute safety window elapsed • Emergency alert & proof transmitted
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1.5 self-start sm:self-auto">
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-bold bg-[#25D366]/20 text-[#25D366] border border-[#25D366]/30">
            <CheckCircle2 className="w-3 h-3" /> WhatsApp Live
          </span>
          <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-[10px] font-bold bg-[#E51E4D]/20 text-[#FFA7BF] border border-[#E51E4D]/30">
            SMS Dispatched
          </span>
        </div>
      </div>

      {/* Channel Switcher Tabs */}
      <div className="flex items-center p-1 bg-black/40 rounded-xl border border-white/10">
        <button
          type="button"
          onClick={() => setActiveChannel('whatsapp')}
          className={`flex-1 py-2 px-3 rounded-lg text-xs font-bold flex items-center justify-center gap-2 transition-all ${
            activeChannel === 'whatsapp'
              ? 'bg-[#25D366] text-[#0A2613] shadow-md'
              : 'text-zinc-400 hover:text-white'
          }`}
        >
          <svg className="w-4 h-4 fill-current shrink-0" viewBox="0 0 24 24">
            <path d="M12.031 6.172c-3.181 0-5.767 2.586-5.768 5.766-.001 1.298.38 2.27 1.019 3.287l-.582 2.128 2.182-.573c.978.58 1.911.928 3.145.929 3.178 0 5.767-2.587 5.768-5.766 0-3.18-2.586-5.771-5.764-5.771zm3.392 8.244c-.144.405-.837.774-1.17.824-.299.045-.677.063-1.092-.069-.252-.08-.575-.187-.988-.365-1.739-.751-2.874-2.502-2.961-2.617-.087-.116-.708-.94-.708-1.793s.448-1.273.607-1.446c.159-.173.346-.217.462-.217l.332.006c.106.005.249-.04.39.299.144.346.491 1.2.534 1.288.043.087.072.188.014.304-.058.116-.087.188-.173.289l-.26.304c-.087.086-.177.18-.076.353.101.173.45 1.743 1.968 2.072.195.042.311.02.427-.037.116-.058.491-.573.621-.769.13-.196.26-.163.433-.101.173.062 1.097.517 1.285.611.188.094.314.139.36.219.045.08.045.464-.099.869z" />
          </svg>
          <span>WhatsApp Dossier</span>
          <span className="text-[10px] px-1.5 py-0.2 rounded-full bg-black/20 font-mono">
            {evidenceList.length} photo{evidenceList.length === 1 ? '' : 's'}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setActiveChannel('sms')}
          className={`flex-1 py-2 px-3 rounded-lg text-xs font-bold flex items-center justify-center gap-2 transition-all ${
            activeChannel === 'sms'
              ? 'bg-[#E51E4D] text-white shadow-md'
              : 'text-zinc-400 hover:text-white'
          }`}
        >
          <MessageSquare className="w-4 h-4 shrink-0" />
          <span>Cellular SMS</span>
        </button>
      </div>

      {/* Attached Evidence Preview Carousel */}
      {evidenceList.length > 0 && (
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-xs text-[#FFA7BF]">
            <span className="font-bold flex items-center gap-1.5">
              <Camera className="w-3.5 h-3.5" />
              Cryptographic Evidence Photos Attached ({evidenceList.length})
            </span>
            <span className="text-[10px] text-zinc-400">Included in WhatsApp dossier</span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
            {evidenceList.slice(0, 3).map((item) => (
              <div
                key={item.id}
                className="rounded-xl overflow-hidden bg-black/60 border border-white/10 flex flex-col group relative"
              >
                <div className="aspect-4/3 relative bg-black">
                  <img
                    src={item.dataUrl}
                    alt={item.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                  />
                  <div className="absolute bottom-1 left-1 px-1.5 py-0.5 rounded bg-black/80 text-[9px] font-mono text-emerald-300 flex items-center gap-1">
                    <ShieldCheck className="w-2.5 h-2.5" /> SHA256
                  </div>
                </div>
                <div className="p-2 text-[10px] text-zinc-300">
                  <span className="font-bold block truncate text-white">{item.name}</span>
                  <span className="font-mono text-[9px] text-[#FFA7BF] truncate block">
                    {item.sha256Hash.slice(0, 14)}...
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Displayed Payload Body */}
      <div className="relative">
        <div className="bg-[#0b0409] rounded-[16px] p-3.5 border border-white/10 font-mono text-[11px] leading-relaxed text-[#FFD6E1] whitespace-pre-wrap select-all max-h-48 overflow-y-auto">
          {activeChannel === 'whatsapp' ? whatsAppMessage : smsMessage}
        </div>
        <button
          type="button"
          onClick={() =>
            handleCopy(
              activeChannel === 'whatsapp' ? whatsAppMessage : smsMessage,
              activeChannel === 'whatsapp' ? 'WhatsApp message' : 'SMS text'
            )
          }
          className="absolute top-2.5 right-2.5 px-2 py-1 rounded bg-white/15 hover:bg-white/25 text-white text-[10px] font-bold flex items-center gap-1 backdrop-blur-sm transition-colors"
        >
          <Copy className="w-3 h-3" /> {copied ? 'Copied' : 'Copy'}
        </button>
      </div>

      {/* WhatsApp Dispatch Actions */}
      {activeChannel === 'whatsapp' ? (
        <div className="space-y-2.5">
          <div className="flex flex-col sm:flex-row gap-2">
            <button
              type="button"
              id="btn-whatsapp-share-all"
              onClick={handleOpenWhatsAppAll}
              className="flex-1 py-3 px-4 rounded-xl bg-[#25D366] hover:bg-[#20ba59] text-[#0A2613] text-xs font-extrabold flex items-center justify-center gap-2 shadow-lg shadow-[#25D366]/20 transition-transform active:scale-[0.98]"
            >
              <svg className="w-4 h-4 fill-current shrink-0" viewBox="0 0 24 24">
                <path d="M12.031 6.172c-3.181 0-5.767 2.586-5.768 5.766-.001 1.298.38 2.27 1.019 3.287l-.582 2.128 2.182-.573c.978.58 1.911.928 3.145.929 3.178 0 5.767-2.587 5.768-5.766 0-3.18-2.586-5.771-5.764-5.771zm3.392 8.244c-.144.405-.837.774-1.17.824-.299.045-.677.063-1.092-.069-.252-.08-.575-.187-.988-.365-1.739-.751-2.874-2.502-2.961-2.617-.087-.116-.708-.94-.708-1.793s.448-1.273.607-1.446c.159-.173.346-.217.462-.217l.332.006c.106.005.249-.04.39.299.144.346.491 1.2.534 1.288.043.087.072.188.014.304-.058.116-.087.188-.173.289l-.26.304c-.087.086-.177.18-.076.353.101.173.45 1.743 1.968 2.072.195.042.311.02.427-.037.116-.058.491-.573.621-.769.13-.196.26-.163.433-.101.173.062 1.097.517 1.285.611.188.094.314.139.36.219.045.08.045.464-.099.869z" />
              </svg>
              <span>Forward Evidence Dossier to WhatsApp Contacts / Groups</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* Direct 1-Tap WhatsApp Buttons for configured contacts */}
          {contacts.length > 0 && (
            <div className="pt-2 border-t border-white/10 space-y-1.5">
              <span className="text-[11px] text-zinc-400 font-medium block">
                Direct WhatsApp Contact Send:
              </span>
              <div className="flex flex-wrap gap-2">
                {contacts.map((c) => (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => handleSendContactWhatsApp(c)}
                    className="py-1.5 px-3 rounded-lg bg-emerald-950/80 hover:bg-emerald-900 border border-[#25D366]/40 text-emerald-200 text-xs font-bold flex items-center gap-1.5 transition-colors"
                  >
                    <UserCheck className="w-3.5 h-3.5 text-[#25D366]" />
                    <span>Send to {c.name}</span>
                    <ExternalLink className="w-3 h-3 text-[#25D366]" />
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      ) : (
        /* SMS Dispatch Actions */
        <div className="flex flex-col sm:flex-row gap-2">
          <button
            type="button"
            id="btn-resend-sms-dossier"
            onClick={handleResendSms}
            className="flex-1 py-3 px-4 rounded-xl bg-[#E51E4D] hover:bg-[#c91841] text-white text-xs font-bold flex items-center justify-center gap-2 shadow-sm transition-colors"
          >
            <Send className="w-3.5 h-3.5" />
            <span>Resend via Device SMS App</span>
          </button>
        </div>
      )}
    </div>
  );
};
