import React from 'react';
import { Shield, AlertTriangle } from 'lucide-react';
import { EmergencyState } from '../types';

interface StatusCardProps {
  state: EmergencyState;
}

export const StatusCard: React.FC<StatusCardProps> = ({ state }) => {
  const active =
    state === EmergencyState.EMERGENCY_ACTIVE ||
    state === EmergencyState.MONITORING ||
    state === EmergencyState.CANCELLATION_PENDING;

  const isCompleted = state === EmergencyState.COMPLETED;

  return (
    <div
      id="emergency-status-card"
      className={`w-full rounded-[24px] p-5 transition-all duration-300 border ${
        active
          ? 'bg-[#4B1724] border-[#E51E4D]/40 text-white shadow-[0_0_24px_rgba(229,30,77,0.25)]'
          : isCompleted
          ? 'bg-[#2A1520] border-[#E51E4D]/50 text-white shadow-[0_0_24px_rgba(229,30,77,0.2)]'
          : 'bg-[#17172A] border-white/5 text-white'
      }`}
    >
      <div className="flex items-center gap-4">
        <div
          className={`w-13 h-13 rounded-full flex items-center justify-center shrink-0 ${
            active || isCompleted ? 'bg-[#E51E4D]/20 text-[#FFA7BF]' : 'bg-[#2C2A4C] text-[#C8BFFF]'
          }`}
        >
          {active ? (
            <AlertTriangle className="w-7 h-7 animate-bounce" />
          ) : isCompleted ? (
            <AlertTriangle className="w-7 h-7 text-[#FFA7BF]" />
          ) : (
            <Shield className="w-7 h-7" />
          )}
        </div>
        <div className="flex flex-col">
          <div className="flex items-center gap-2">
            <span className="text-[17px] font-extrabold tracking-wide uppercase">
              {active ? 'EMERGENCY ACTIVE' : isCompleted ? 'EMERGENCY CONFIRMED' : state.replace('_', ' ')}
            </span>
          </div>
          <span className="text-[13px] text-[#E2DFEA]">
            {active
              ? 'Manual SOS has priority • Live protection engaged'
              : isCompleted
              ? '2-Min timer expired: Emergency SMS sent with GPS & evidence'
              : 'No emergency is active'}
          </span>
          <span className="text-[11px] font-medium text-[#AAA6BA] mt-0.5">
            {isCompleted ? 'Alerts successfully delivered to trusted contacts' : 'Phase 5 • Emergency Control Subsystem'}
          </span>
        </div>
      </div>
    </div>
  );
};
