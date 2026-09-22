import React from 'react';
import { QwenContextAnalysis, Phi35IncidentEvaluation, SafetyPlace, UnsafeZone, LocationPoint } from '../types';
import { Cpu, Brain, Camera, AlertOctagon, ShieldCheck, Zap, Sparkles, CheckCircle2 } from 'lucide-react';

interface LocalAiDualEngineCardProps {
  qwenAnalysis: QwenContextAnalysis;
  phiAnalysis: Phi35IncidentEvaluation;
  onSelectQwenVariant: (variant: 'Qwen 2.5 1.5B' | 'Qwen 2.5 3B') => void;
  onTriggerAutoCapture: () => void;
  evidenceCount: number;
}

export const LocalAiDualEngineCard: React.FC<LocalAiDualEngineCardProps> = ({
  qwenAnalysis,
  phiAnalysis,
  onSelectQwenVariant,
  onTriggerAutoCapture,
  evidenceCount,
}) => {
  return (
    <div id="local-ai-dual-engine-card" className="rounded-[24px] bg-[#100D22] border border-white/10 p-5 space-y-4 shadow-xl text-white">
      {/* Top Main Heading */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-[#5B4BDB] to-[#7B68EE] flex items-center justify-center text-white shadow-md shadow-[#5B4BDB]/20">
            <Brain className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-extrabold text-sm tracking-wide uppercase text-white">
                Dual Local AI Reasoning Pipeline
              </h3>
              <span className="px-2 py-0.5 rounded-full text-[9px] font-bold bg-[#10B981]/20 text-[#34D399] border border-[#10B981]/30">
                100% Offline
              </span>
            </div>
            <p className="text-[11px] text-[#A6A2BC]">
              Qwen 2.5 (Context Analysis) + Phi-3.5 Mini (Secondary Reasoning & Autonomous Action)
            </p>
          </div>
        </div>
      </div>

      {/* MODEL 1: QWEN 2.5 (1.5B / 3B) */}
      <div className="rounded-[20px] bg-[#16132D] border border-[#5B4BDB]/30 p-4 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-2.5 h-2.5 rounded-full bg-[#7B68EE] animate-pulse" />
            <span className="text-xs font-black uppercase tracking-wider text-[#C8BFFF] flex items-center gap-1.5">
              <Cpu className="w-3.5 h-3.5 text-[#A594FD]" />
              {qwenAnalysis.modelName} • Main Local Reasoning
            </span>
          </div>

          {/* Model Switcher */}
          <div className="flex items-center gap-1 p-0.5 rounded-xl bg-black/40 border border-white/5 text-[10px] font-bold">
            <button
              type="button"
              onClick={() => onSelectQwenVariant('Qwen 2.5 3B')}
              className={`px-2 py-1 rounded-lg transition-colors ${
                qwenAnalysis.modelName === 'Qwen 2.5 3B'
                  ? 'bg-[#5B4BDB] text-white'
                  : 'text-[#8E8A9F] hover:text-white'
              }`}
            >
              3B (Precision)
            </button>
            <button
              type="button"
              onClick={() => onSelectQwenVariant('Qwen 2.5 1.5B')}
              className={`px-2 py-1 rounded-lg transition-colors ${
                qwenAnalysis.modelName === 'Qwen 2.5 1.5B'
                  ? 'bg-[#5B4BDB] text-white'
                  : 'text-[#8E8A9F] hover:text-white'
              }`}
            >
              1.5B (Ultra-Fast)
            </button>
          </div>
        </div>

        {/* Situational Context & Hypothesis */}
        <div className="space-y-1.5">
          <div className="text-[11px] font-bold uppercase tracking-wider text-[#A6A2BC] flex items-center justify-between">
            <span>Incident Context Analysis</span>
            <span className="text-[10px] text-[#C8BFFF] font-mono">{qwenAnalysis.inferenceLatencyMs}ms latency</span>
          </div>
          <div className="p-3 rounded-xl bg-black/30 border border-white/5 text-xs text-[#E2DFEA] leading-relaxed">
            {qwenAnalysis.hypothesis}
          </div>
        </div>

        {/* Identified Sensor Signals Pill Cloud */}
        <div className="space-y-1">
          <span className="text-[10px] font-bold uppercase tracking-wider text-[#8E8A9F] block">
            Synthesized Signals
          </span>
          <div className="flex flex-wrap gap-1.5">
            {qwenAnalysis.identifiedSignals.map((signal, i) => (
              <span
                key={i}
                className="px-2 py-1 rounded-lg bg-white/5 border border-white/10 text-[11px] text-[#DDD8ED] font-medium"
              >
                {signal}
              </span>
            ))}
          </div>
        </div>

        {/* Escalation Tag */}
        <div className="flex items-center justify-between pt-1 border-t border-white/5 text-[11px]">
          <span className="text-[#A6A2BC]">Escalated to Phi-3.5 Mini:</span>
          <span
            className={`font-bold px-2 py-0.5 rounded-full text-[10px] ${
              qwenAnalysis.escalateToPhi
                ? 'bg-[#E51E4D]/20 text-[#FFA7BF] border border-[#E51E4D]/30'
                : 'bg-white/5 text-[#A6A2BC]'
            }`}
          >
            {qwenAnalysis.escalateToPhi ? 'YES • High Confidence Threat' : 'Standby'}
          </span>
        </div>
      </div>

      {/* MODEL 2: PHI-3.5 MINI 3.8B */}
      <div className="rounded-[20px] bg-[#161225] border border-[#E51E4D]/30 p-4 space-y-3.5">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-2.5 h-2.5 rounded-full bg-[#E51E4D] animate-ping" />
            <span className="text-xs font-black uppercase tracking-wider text-[#FFA7BF] flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-[#FF7597]" />
              Phi-3.5 Mini (3.8B) • Secondary Reasoning Agent
            </span>
          </div>

          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-[#E51E4D]/20 text-[#FFA7BF] border border-[#E51E4D]/40">
            Confidence: {phiAnalysis.confidenceScore}%
          </span>
        </div>

        {/* Feature 1: "Tell What Happened" Natural Narrative */}
        <div className="space-y-1.5">
          <div className="flex items-center gap-1.5">
            <span className="text-[11px] font-black uppercase tracking-wider text-[#FFA7BF]">
              AI Explanation: "What Happened"
            </span>
          </div>
          <div className="p-3.5 rounded-2xl bg-[#0e0a17] border border-white/5 text-xs text-[#F2ECF9] leading-relaxed select-text">
            {phiAnalysis.whatHappenedNarrative}
          </div>
        </div>

        {/* Feature 2: Autonomous Camera Evidence Trigger */}
        <div className="p-3 rounded-2xl bg-[#1d101a] border border-[#E51E4D]/20 space-y-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="w-7 h-7 rounded-xl bg-[#E51E4D]/20 text-[#FFA7BF] flex items-center justify-center">
                <Camera className="w-4 h-4" />
              </div>
              <div>
                <span className="font-bold text-xs text-white block">
                  Autonomous Camera Evidence Trigger
                </span>
                <span className="text-[10px] text-[#E0AFC0] block">
                  {phiAnalysis.autoEvidenceAction.reason}
                </span>
              </div>
            </div>

            <button
              type="button"
              onClick={onTriggerAutoCapture}
              className="px-3 py-1.5 rounded-xl bg-[#E51E4D] hover:bg-[#cf1340] text-white text-[11px] font-bold flex items-center gap-1.5 shadow transition-colors shrink-0"
            >
              <Camera className="w-3 h-3" />
              Auto-Snap Photo ({evidenceCount})
            </button>
          </div>
        </div>

        {/* Feature 3: Safe & Unsafe Surroundings Insight */}
        <div className="grid grid-cols-2 gap-2 text-[11px]">
          <div className="p-2.5 rounded-xl bg-[#0d1e16] border border-[#10B981]/20 space-y-1">
            <span className="text-[10px] font-extrabold uppercase text-[#34D399] flex items-center gap-1">
              <ShieldCheck className="w-3.5 h-3.5" /> Safe Havens
            </span>
            <p className="text-[#C1F3DC] text-[11px]">
              {phiAnalysis.safePlacesNearby.length} verified safe locations mapped (nearest: {phiAnalysis.recommendedEscapeRoute?.destinationName || 'Local Precinct'}).
            </p>
          </div>

          <div className="p-2.5 rounded-xl bg-[#241113] border border-[#EF4444]/20 space-y-1">
            <span className="text-[10px] font-extrabold uppercase text-[#F87171] flex items-center gap-1">
              <AlertOctagon className="w-3.5 h-3.5" /> Danger Zones
            </span>
            <p className="text-[#FDC5C8] text-[11px]">
              {phiAnalysis.unsafeZonesNearby.length} high-risk zones flagged (poor lighting & blind egress corridors).
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
