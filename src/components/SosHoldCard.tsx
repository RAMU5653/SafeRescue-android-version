import React, { useState, useRef, useEffect, useCallback } from 'react';
import { AlertCircle, Clock, XCircle } from 'lucide-react';

interface SosHoldCardProps {
  onComplete: () => void;
  onAccessibleStart: () => void;
  onAccessibleCancel: () => void;
  isAccessiblePending?: boolean;
  accessibleSecondsLeft?: number;
}

export const SosHoldCard: React.FC<SosHoldCardProps> = ({
  onComplete,
  onAccessibleStart,
  onAccessibleCancel,
  isAccessiblePending = false,
  accessibleSecondsLeft = 5,
}) => {
  const [holding, setHolding] = useState(false);
  const [progress, setProgress] = useState(0);
  const startTimeRef = useRef<number | null>(null);
  const animFrameRef = useRef<number | null>(null);

  const triggerHaptic = useCallback(() => {
    if ('vibrate' in navigator) {
      try {
        navigator.vibrate([100, 50, 200]);
      } catch {
        // ignore
      }
    }
  }, []);

  const handleStartHold = (e: React.SyntheticEvent) => {
    e.preventDefault();
    if (isAccessiblePending) return;
    setHolding(true);
    startTimeRef.current = Date.now();
  };

  const handleRelease = useCallback(() => {
    if (holding && progress < 1) {
      setHolding(false);
      setProgress(0);
      startTimeRef.current = null;
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    }
  }, [holding, progress]);

  useEffect(() => {
    if (!holding) {
      setProgress(0);
      startTimeRef.current = null;
      return;
    }

    const interval = 5000; // 5 seconds
    const tick = () => {
      if (!startTimeRef.current) return;
      const elapsed = Date.now() - startTimeRef.current;
      const p = Math.min(1, elapsed / interval);
      setProgress(p);

      if (p >= 1) {
        setHolding(false);
        triggerHaptic();
        onComplete();
      } else {
        animFrameRef.current = requestAnimationFrame(tick);
      }
    };

    animFrameRef.current = requestAnimationFrame(tick);

    return () => {
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    };
  }, [holding, onComplete, triggerHaptic]);

  // Global mouse/touch release listener
  useEffect(() => {
    const onUp = () => {
      if (holding) handleRelease();
    };
    window.addEventListener('mouseup', onUp);
    window.addEventListener('touchend', onUp);
    return () => {
      window.removeEventListener('mouseup', onUp);
      window.removeEventListener('touchend', onUp);
    };
  }, [holding, handleRelease]);

  const currentSeconds = Math.min(5, Math.floor(progress * 5) + 1);

  return (
    <div
      id="sos-hold-container"
      className="w-full rounded-[24px] p-5 bg-[#321126] border border-[#7C315E] shadow-[0_8px_32px_rgba(0,0,0,0.3)] select-none text-white"
    >
      <div className="flex flex-col items-center text-center">
        <h3 className="text-[17px] font-bold tracking-tight">Emergency Access</h3>
        <p className="text-[12px] text-[#D5B9C8] mt-0.5">
          Press and continuously hold for 5 seconds to activate SOS.
        </p>

        {isAccessiblePending ? (
          <div className="w-full mt-4 p-4 rounded-[16px] bg-[#4B1724] border border-[#E51E4D]/50 flex flex-col items-center">
            <div className="flex items-center gap-2 text-[#FFA7BF] font-bold text-sm">
              <Clock className="w-4 h-4 animate-spin" />
              Safety Delay Active: Activating in {accessibleSecondsLeft}s
            </div>
            <button
              type="button"
              id="btn-cancel-accessible-sos"
              onClick={onAccessibleCancel}
              className="mt-3 px-4 py-2 rounded-xl bg-[#8E1740] hover:bg-[#a61c4c] text-white text-xs font-bold flex items-center gap-1.5 transition-colors"
            >
              <XCircle className="w-4 h-4" /> Cancel Pending Activation
            </button>
          </div>
        ) : (
          <div className="w-full mt-4">
            <button
              type="button"
              id="btn-hold-sos"
              onMouseDown={handleStartHold}
              onTouchStart={handleStartHold}
              className={`w-full h-16 rounded-[16px] flex items-center justify-center gap-2 text-white font-black tracking-wider text-base transition-all duration-150 active:scale-[0.98] ${
                holding
                  ? 'bg-[#b8143c] shadow-[inset_0_4px_12px_rgba(0,0,0,0.4)] scale-[0.99]'
                  : 'bg-[#E51E4D] hover:bg-[#ff2458] shadow-[0_6px_20px_rgba(229,30,77,0.4)]'
              }`}
            >
              <AlertCircle className="w-6 h-6 shrink-0" />
              <span>
                {holding ? `HOLDING SOS • ${currentSeconds}S / 5S` : 'HOLD SOS • 5 SEC'}
              </span>
            </button>

            {/* Hold progress bar */}
            <div className="w-full h-2 rounded-full bg-[#512431] mt-3 overflow-hidden">
              <div
                className="h-full bg-gradient-to-r from-[#FF72C8] to-[#FFA7BF] transition-[width] duration-75"
                style={{ width: `${progress * 100}%` }}
              />
            </div>

            {/* Accessible fallback options */}
            <div className="mt-3 flex items-center justify-between text-xs text-[#D5B9C8]">
              <span className="text-[11px] opacity-80">Non-touch / single-click option:</span>
              <button
                type="button"
                id="btn-accessible-start-sos"
                onClick={onAccessibleStart}
                className="text-[#FFA7BF] hover:text-white underline font-semibold transition-colors"
              >
                Start with 5s delay
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
