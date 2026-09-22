import React, { useState } from 'react';
import { X, MessageSquare, Send, Share2, Copy, Check, ExternalLink, AlertTriangle, ShieldCheck } from 'lucide-react';
import { LocationPoint, TrustedContact } from '../types';
import {
  formatEmergencyText,
  sendSms,
  sendWhatsApp,
  shareNative,
  copyMessage,
} from '../services/emergencyMessenger';

interface SendRealMessageModalProps {
  isOpen: boolean;
  onClose: () => void;
  contacts: TrustedContact[];
  location: LocationPoint | null;
  victimName?: string;
  victimPhone?: string;
  riskScore?: number;
  riskSeverity?: string;
  evidenceCount?: number;
  initialContact?: TrustedContact | null;
  initialMode?: 'emergency' | 'test';
}

export const SendRealMessageModal: React.FC<SendRealMessageModalProps> = ({
  isOpen,
  onClose,
  contacts,
  location,
  victimName = 'SafeRescue User',
  victimPhone,
  riskScore,
  riskSeverity,
  evidenceCount = 0,
  initialContact,
  initialMode = 'emergency',
}) => {
  const [selectedContactId, setSelectedContactId] = useState<string>(
    initialContact?.id || contacts[0]?.id || 'custom'
  );
  const [customPhone, setCustomPhone] = useState<string>('');
  const [isTestMode, setIsTestMode] = useState<boolean>(initialMode === 'test');
  const [customNote, setCustomNote] = useState<string>('');
  const [copied, setCopied] = useState<boolean>(false);
  const [statusNotice, setStatusNotice] = useState<string | null>(null);

  if (!isOpen) return null;

  const currentContact = contacts.find((c) => c.id === selectedContactId);
  const targetPhone = selectedContactId === 'custom' ? customPhone : (currentContact?.phone || '');

  const generatedMessage = formatEmergencyText({
    victimName,
    victimPhone,
    location,
    riskScore,
    riskSeverity,
    evidenceCount,
    customNote: customNote.trim() || undefined,
    isTest: isTestMode,
  });

  const handleSendSms = () => {
    if (!targetPhone.trim()) {
      setStatusNotice('Please select a contact or enter a phone number.');
      return;
    }
    const success = sendSms(targetPhone, generatedMessage);
    if (success) {
      setStatusNotice(`Opened SMS messaging app with ${targetPhone}`);
    }
  };

  const handleSendWhatsApp = () => {
    if (!targetPhone.trim()) {
      setStatusNotice('Please select a contact or enter a phone number with country code.');
      return;
    }
    const success = sendWhatsApp(targetPhone, generatedMessage);
    if (success) {
      setStatusNotice(`Opened WhatsApp to send message to ${targetPhone}`);
    }
  };

  const handleShare = async () => {
    const success = await shareNative(
      generatedMessage,
      isTestMode ? 'SafeRescue Test Check' : '🚨 SafeRescue Emergency SOS'
    );
    if (success) {
      setStatusNotice('Shared via system share menu!');
    }
  };

  const handleCopy = async () => {
    const success = await copyMessage(generatedMessage);
    if (success) {
      setCopied(true);
      setStatusNotice('Emergency message copied to clipboard!');
      setTimeout(() => setCopied(false), 3000);
    }
  };

  return (
    <div
      id="send-real-message-modal"
      className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in"
    >
      <div className="w-full max-w-md bg-[#0F172A] border border-white/10 rounded-[24px] shadow-2xl p-5 text-white space-y-4 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/10 pb-3">
          <div className="flex items-center gap-2.5">
            <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${isTestMode ? 'bg-[#5B4BDB]/20 text-[#8B7CFF]' : 'bg-[#E51E4D]/20 text-[#FFA7BF]'}`}>
              {isTestMode ? <ShieldCheck className="w-5 h-5" /> : <AlertTriangle className="w-5 h-5" />}
            </div>
            <div>
              <h3 className="font-extrabold text-base leading-tight">
                {isTestMode ? 'Send Test Safety Check' : 'Send Real Emergency Message'}
              </h3>
              <p className="text-[11px] text-[#94A3B8]">
                Dispatches directly via your device's SMS or WhatsApp
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-full text-zinc-400 hover:text-white hover:bg-white/10 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Mode Selector Toggle */}
        <div className="grid grid-cols-2 gap-1.5 p-1 bg-[#1E293B] rounded-xl text-xs font-bold">
          <button
            type="button"
            onClick={() => setIsTestMode(false)}
            className={`py-2 px-3 rounded-lg flex items-center justify-center gap-1.5 transition-all ${
              !isTestMode
                ? 'bg-[#E51E4D] text-white shadow-md'
                : 'text-zinc-400 hover:text-white'
            }`}
          >
            <AlertTriangle className="w-3.5 h-3.5" /> Real Emergency SOS
          </button>
          <button
            type="button"
            onClick={() => setIsTestMode(true)}
            className={`py-2 px-3 rounded-lg flex items-center justify-center gap-1.5 transition-all ${
              isTestMode
                ? 'bg-[#5B4BDB] text-white shadow-md'
                : 'text-zinc-400 hover:text-white'
            }`}
          >
            <ShieldCheck className="w-3.5 h-3.5" /> Test Check Ping
          </button>
        </div>

        {/* Recipient Selection */}
        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-[#CBD5E1] block">
            Select Recipient:
          </label>
          <div className="grid grid-cols-1 gap-2">
            {contacts.map((c) => (
              <label
                key={c.id}
                className={`flex items-center justify-between p-2.5 rounded-xl border text-xs cursor-pointer transition-colors ${
                  selectedContactId === c.id
                    ? 'bg-[#1E293B] border-[#5B4BDB] text-white'
                    : 'bg-[#141E33] border-white/5 text-zinc-300 hover:border-white/20'
                }`}
              >
                <div className="flex items-center gap-2">
                  <input
                    type="radio"
                    name="recipient"
                    checked={selectedContactId === c.id}
                    onChange={() => setSelectedContactId(c.id)}
                    className="accent-[#5B4BDB]"
                  />
                  <div>
                    <div className="font-bold">{c.name}</div>
                    <div className="text-[11px] text-zinc-400 font-mono">{c.phone}</div>
                  </div>
                </div>
                <span className="text-[10px] uppercase font-bold text-zinc-500">
                  {c.relationship || 'Trusted'}
                </span>
              </label>
            ))}

            <label
              className={`flex items-center justify-between p-2.5 rounded-xl border text-xs cursor-pointer transition-colors ${
                selectedContactId === 'custom'
                  ? 'bg-[#1E293B] border-[#5B4BDB] text-white'
                  : 'bg-[#141E33] border-white/5 text-zinc-300 hover:border-white/20'
              }`}
            >
              <div className="flex items-center gap-2">
                <input
                  type="radio"
                  name="recipient"
                  checked={selectedContactId === 'custom'}
                  onChange={() => setSelectedContactId('custom')}
                  className="accent-[#5B4BDB]"
                />
                <span className="font-bold">Other Phone Number (Any mobile)</span>
              </div>
            </label>
          </div>

          {selectedContactId === 'custom' && (
            <input
              type="tel"
              placeholder="e.g. +1 555-0199 or +91 9876543210"
              value={customPhone}
              onChange={(e) => setCustomPhone(e.target.value)}
              className="w-full mt-2 px-3 py-2 bg-[#090D1A] border border-white/20 rounded-xl text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-[#5B4BDB]"
            />
          )}
        </div>

        {/* Custom optional note */}
        <div className="space-y-1">
          <label className="text-xs font-semibold text-[#CBD5E1] block">
            Add Note / Immediate Situation (Optional):
          </label>
          <input
            type="text"
            placeholder="e.g., Suspicious person following me, or minor medical emergency"
            value={customNote}
            onChange={(e) => setCustomNote(e.target.value)}
            className="w-full px-3 py-2 bg-[#090D1A] border border-white/20 rounded-xl text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-[#5B4BDB]"
          />
        </div>

        {/* Message Preview Box */}
        <div className="space-y-1">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-zinc-400">Message Preview:</span>
            <button
              type="button"
              onClick={handleCopy}
              className="text-[11px] text-[#8B7CFF] hover:underline flex items-center gap-1 font-medium"
            >
              {copied ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
              {copied ? 'Copied' : 'Copy Text'}
            </button>
          </div>
          <div className="bg-[#090D1A] p-3 rounded-xl border border-white/10 text-[11px] font-mono leading-relaxed text-[#E2E8F0] whitespace-pre-wrap max-h-32 overflow-y-auto select-all">
            {generatedMessage}
          </div>
        </div>

        {/* Status notice */}
        {statusNotice && (
          <div className="p-2.5 rounded-xl bg-emerald-950/80 border border-emerald-500/40 text-emerald-200 text-xs flex items-center justify-between">
            <span>{statusNotice}</span>
            <button type="button" onClick={() => setStatusNotice(null)} className="text-zinc-400 hover:text-white">
              <X className="w-3 h-3" />
            </button>
          </div>
        )}

        {/* Action Buttons */}
        <div className="space-y-2 pt-1">
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              id="btn-modal-send-sms"
              onClick={handleSendSms}
              className="py-3 px-3 rounded-xl bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-xs font-bold flex items-center justify-center gap-2 shadow-lg shadow-blue-900/30 transition-all active:scale-[0.98]"
            >
              <Send className="w-4 h-4" />
              Send Real SMS
            </button>
            <button
              type="button"
              id="btn-modal-send-whatsapp"
              onClick={handleSendWhatsApp}
              className="py-3 px-3 rounded-xl bg-[#16A34A] hover:bg-[#15803D] text-white text-xs font-bold flex items-center justify-center gap-2 shadow-lg shadow-emerald-900/30 transition-all active:scale-[0.98]"
            >
              <MessageSquare className="w-4 h-4" />
              Send WhatsApp
            </button>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              id="btn-modal-share-native"
              onClick={handleShare}
              className="py-2.5 px-3 rounded-xl bg-white/10 hover:bg-white/15 text-white text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors"
            >
              <Share2 className="w-3.5 h-3.5 text-zinc-300" />
              Share via Any App
            </button>
            <button
              type="button"
              id="btn-modal-copy-msg"
              onClick={handleCopy}
              className="py-2.5 px-3 rounded-xl bg-white/10 hover:bg-white/15 text-white text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5 text-zinc-300" />}
              {copied ? 'Copied!' : 'Copy to Clipboard'}
            </button>
          </div>
        </div>

        <p className="text-[10px] text-zinc-400 text-center leading-normal">
          💡 Clicking <strong>Send Real SMS</strong> or <strong>WhatsApp</strong> triggers your phone or laptop's native messaging application with the message and live coordinates pre-filled.
        </p>
      </div>
    </div>
  );
};
