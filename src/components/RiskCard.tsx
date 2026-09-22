import React from 'react';
import { ShieldCheck, AlertTriangle } from 'lucide-react';
import { RiskFactor, RiskSeverity } from '../types';

interface RiskCardProps {
  active: boolean;
  score: number;
  severity: RiskSeverity;
  factors: RiskFactor[];
}

export const RiskCard: React.FC<RiskCardProps> = ({ active, score, severity, factors }) => {
  const getSeverityBadge = (sev: RiskSeverity) => {
    switch (sev) {
      case RiskSeverity.CRITICAL:
        return 'bg-red-950 text-red-300 border-red-700';
      case RiskSeverity.HIGH:
        return 'bg-rose-950 text-rose-300 border-rose-800';
      case RiskSeverity.MODERATE:
      case RiskSeverity.ELEVATED:
        return 'bg-amber-950 text-amber-300 border-amber-800';
      case RiskSeverity.LOW:
      default:
        return 'bg-emerald-950 text-emerald-300 border-emerald-800';
    }
  };

  return (
    <div
      id="risk-assessment-card"
      className="w-full rounded-[20px] p-5 bg-[#0D2140] border border-white/5 text-white"
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-full bg-[#12264A] flex items-center justify-center text-[#55D7FF]">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <h4 className="text-[15px] font-bold text-white leading-tight">
              {active ? 'Live Risk Assessment' : 'Deterministic Risk Engine'}
            </h4>
            <p className="text-[12px] text-[#B8C4D9]">
              {active ? `${factors.length} factor(s) evaluated` : 'Standby • Activates on SOS'}
            </p>
          </div>
        </div>

        <div className="flex items-baseline gap-1.5">
          <span className="text-2xl font-black text-[#72D8FF]">{score}</span>
          <span className="text-xs text-[#8E9BB6] font-semibold">/100</span>
        </div>
      </div>

      {/* Severity Badge */}
      <div className="mt-3 flex items-center justify-between">
        <span
          className={`text-[11px] font-bold px-2.5 py-0.5 rounded-full border ${getSeverityBadge(
            severity
          )}`}
        >
          {severity} SEVERITY
        </span>
        <span className="text-[11px] text-[#8E9BB6]">Phase 9 • Local Engine</span>
      </div>

      {/* Factor Breakdown */}
      {active && factors.length > 0 && (
        <div className="mt-3 pt-3 border-t border-white/10 space-y-1.5">
          <span className="text-[11px] font-bold uppercase tracking-wider text-[#8E9BB6]">
            Active Risk Contributing Factors
          </span>
          {factors.map((f) => (
            <div key={f.id} className="flex items-center justify-between text-xs py-1">
              <span className="text-[#D6DCEF] font-medium flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-[#55D7FF]" />
                {f.title}
              </span>
              <span className="text-[#72D8FF] font-mono font-bold">+{f.contribution} pts</span>
            </div>
          ))}
        </div>
      )}

      <p className="text-[11px] text-[#8E9BB6] mt-3 leading-relaxed flex items-start gap-1">
        <AlertTriangle className="w-3.5 h-3.5 text-[#55D7FF] shrink-0 mt-0.5" />
        <span>
          {active
            ? 'Heuristic acoustic & positional signal only — not proof of danger. Manual SOS remains authoritative.'
            : 'Evaluating sensor inputs, location accuracy, and acoustic loudness deterministically without cloud dependency.'}
        </span>
      </p>
    </div>
  );
};
