import React from 'react';
import {
  User,
  ShieldCheck,
  Radio,
  Lock,
  LogOut,
  Sliders,
  Cpu,
  Smartphone,
  CheckCircle2,
} from 'lucide-react';
import { AuthUser } from '../types';

interface MeTabProps {
  user: AuthUser;
  onLogout: () => void;
}

export const MeTab: React.FC<MeTabProps> = ({ user, onLogout }) => {
  return (
    <div className="space-y-4 pb-20">
      <div>
        <h2 className="text-2xl font-extrabold text-white">Device & Profile</h2>
        <p className="text-xs text-[#8E9BB6] mt-0.5">
          Phase 2, 11 & 17 • Local cryptographic identity, gateway diagnostics, and device security.
        </p>
      </div>

      {/* User Profile Card */}
      <div className="rounded-[20px] p-5 bg-white text-[#17172A] shadow-md space-y-3">
        <div className="flex items-center gap-3.5">
          <div className="w-13 h-13 rounded-full bg-gradient-to-tr from-[#5B4BDB] to-[#72D8FF] text-white flex items-center justify-center font-black text-xl shadow-md">
            {user.name ? user.name[0]?.toUpperCase() : 'U'}
          </div>
          <div>
            <h3 className="font-extrabold text-base text-[#17172A]">{user.name}</h3>
            <p className="text-xs text-[#5B4BDB] font-semibold">@{user.username}</p>
            <span className="inline-block mt-0.5 text-[10px] font-bold uppercase tracking-wider bg-zinc-100 text-zinc-600 px-2 py-0.5 rounded-full">
              Role: {user.role}
            </span>
          </div>
        </div>

        <div className="pt-2 border-t border-zinc-100 grid grid-cols-2 gap-2 text-xs">
          <div>
            <span className="text-[10px] text-zinc-400 font-semibold block">PHONE</span>
            <span className="font-medium text-zinc-700">{user.phone || '+1 555-0199'}</span>
          </div>
          <div>
            <span className="text-[10px] text-zinc-400 font-semibold block">EMAIL</span>
            <span className="font-medium text-zinc-700 truncate block">
              {user.email || 'admin@saferescue.local'}
            </span>
          </div>
        </div>
      </div>

      {/* Cryptographic KeyStore & Security Status */}
      <div className="rounded-[20px] p-4 bg-white text-[#17172A] shadow-md space-y-2.5">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-[#F1EFFF] flex items-center justify-center text-[#5B4BDB]">
            <Lock className="w-5 h-5" />
          </div>
          <div>
            <h4 className="font-bold text-sm text-[#222236]">Local Keystore & Vault</h4>
            <p className="text-xs text-[#777788]">Phase 11 • AES-256-GCM hardware protection</p>
          </div>
        </div>

        <div className="space-y-1.5 pt-1 text-xs">
          <div className="flex items-center justify-between p-2 rounded-lg bg-zinc-50 border border-zinc-100">
            <span className="text-zinc-600 font-medium">Session Protection:</span>
            <span className="font-bold text-emerald-700 flex items-center gap-1">
              <CheckCircle2 className="w-3.5 h-3.5" /> Active & Encrypted
            </span>
          </div>
          <div className="flex items-center justify-between p-2 rounded-lg bg-zinc-50 border border-zinc-100">
            <span className="text-zinc-600 font-medium">Offline Resilience:</span>
            <span className="font-bold text-emerald-700 flex items-center gap-1">
              <CheckCircle2 className="w-3.5 h-3.5" /> 100% Local-First
            </span>
          </div>
          <div className="flex items-center justify-between p-2 rounded-lg bg-zinc-50 border border-zinc-100">
            <span className="text-zinc-600 font-medium">Tamper Digest:</span>
            <span className="font-mono text-[10px] text-zinc-600">SHA-256 Chain</span>
          </div>
        </div>
      </div>

      {/* LoRa & BLE Gateway Bridge (Phase 17) */}
      <div className="rounded-[20px] p-4 bg-white text-[#17172A] shadow-md space-y-2.5">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-[#F1EFFF] flex items-center justify-center text-[#5B4BDB]">
            <Radio className="w-5 h-5" />
          </div>
          <div>
            <h4 className="font-bold text-sm text-[#222236]">Off-Grid Hardware Bridge</h4>
            <p className="text-xs text-[#777788]">Phase 17 • LoRa RF / BLE Mesh Protocol</p>
          </div>
        </div>

        <div className="space-y-1.5 pt-1 text-xs">
          <div className="flex items-center justify-between p-2 rounded-lg bg-zinc-50 border border-zinc-100">
            <span className="text-zinc-600 font-medium">LoRa Gateway Bridge:</span>
            <span className="font-bold text-zinc-700">Protocol Spec Defined</span>
          </div>
          <div className="flex items-center justify-between p-2 rounded-lg bg-zinc-50 border border-zinc-100">
            <span className="text-zinc-600 font-medium">RF Frequencies:</span>
            <span className="font-mono text-[11px] text-zinc-600">915 MHz (US) / 868 MHz (EU)</span>
          </div>
        </div>
      </div>

      {/* Logout Action */}
      <div className="pt-2">
        <button
          type="button"
          id="btn-logout"
          onClick={onLogout}
          className="w-full py-3 rounded-[16px] bg-[#1a233a] hover:bg-[#222f4f] text-rose-300 border border-rose-500/20 font-bold text-xs flex items-center justify-center gap-2 transition-colors"
        >
          <LogOut className="w-4 h-4" /> Sign Out from SafeRescue
        </button>
      </div>
    </div>
  );
};
