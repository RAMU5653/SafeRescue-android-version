import React from 'react';
import { User, ShieldAlert } from 'lucide-react';
import { AuthUser, EmergencyState } from '../types';

interface HeaderProps {
  user: AuthUser;
  emergencyState: EmergencyState;
  onProfileClick: () => void;
}

export const Header: React.FC<HeaderProps> = ({ user, emergencyState, onProfileClick }) => {
  const isEmergencyActive =
    emergencyState === EmergencyState.EMERGENCY_ACTIVE ||
    emergencyState === EmergencyState.MONITORING ||
    emergencyState === EmergencyState.CANCELLATION_PENDING;

  return (
    <header className="flex items-center justify-between py-3 border-b border-[#12264A]/60">
      <div className="flex items-center gap-3">
        <img
          src="/saferescue_logo.png"
          alt="SafeRescue — Your Safety Our Priority"
          className="h-10 w-auto object-contain drop-shadow-md"
          onError={(e) => {
            // Fallback if image asset fails
            (e.currentTarget as HTMLElement).style.display = 'none';
          }}
        />
        <div className="flex flex-col">
          <div className="flex items-center gap-2">
            <span className="text-xl font-black tracking-tight text-white">SafeRescue</span>
            {isEmergencyActive && (
              <span className="flex items-center gap-1 text-[11px] font-bold px-2 py-0.5 rounded-full bg-[#E51E4D] text-white animate-pulse">
                <ShieldAlert className="w-3 h-3" /> SOS ACTIVE
              </span>
            )}
          </div>
          <span className="text-[11px] font-medium text-[#8E9BB6]">Local-First Safety Platform</span>
        </div>
      </div>

      <button
        type="button"
        id="btn-header-profile"
        onClick={onProfileClick}
        title="View Profile & Settings"
        className="w-10 h-10 rounded-full bg-[#12264A] flex items-center justify-center text-[#72D8FF] hover:bg-[#1a3461] transition-colors border border-[#72D8FF]/20"
      >
        <User className="w-5 h-5" />
      </button>
    </header>
  );
};
