import React, { useState, useRef, useEffect } from 'react';
import { Camera, X, RefreshCw, CheckCircle, ShieldAlert } from 'lucide-react';
import { EvidenceCapture } from '../types';
import { computeSha256 } from '../services/hashService';

interface CameraModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCaptureSaved: (evidence: EvidenceCapture) => void;
}

export const CameraModal: React.FC<CameraModalProps> = ({ isOpen, onClose, onCaptureSaved }) => {
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [facingMode, setFacingMode] = useState<'user' | 'environment'>('environment');
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [capturing, setCapturing] = useState(false);
  const [lastSnap, setLastSnap] = useState<EvidenceCapture | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);

  useEffect(() => {
    if (!isOpen) {
      if (stream) {
        stream.getTracks().forEach((track) => track.stop());
        setStream(null);
      }
      setLastSnap(null);
      return;
    }

    let activeStream: MediaStream | null = null;

    const startCamera = async () => {
      try {
        setPermissionDenied(false);
        const s = await navigator.mediaDevices.getUserMedia({
          video: { facingMode },
          audio: false,
        });
        activeStream = s;
        setStream(s);
        if (videoRef.current) {
          videoRef.current.srcObject = s;
        }
      } catch (err) {
        console.warn('Camera access denied or unavailable:', err);
        setPermissionDenied(true);
      }
    };

    startCamera();

    return () => {
      if (activeStream) {
        activeStream.getTracks().forEach((track) => track.stop());
      }
    };
  }, [isOpen, facingMode]);

  const toggleLens = () => {
    setFacingMode((prev) => (prev === 'environment' ? 'user' : 'environment'));
  };

  const capturePhoto = async () => {
    if (!videoRef.current || capturing) return;
    setCapturing(true);

    const video = videoRef.current;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    const dataUrl = canvas.toDataURL('image/jpeg', 0.85);

    // Compute SHA-256 fingerprint for tamper-evident verification
    const hash = await computeSha256(dataUrl);
    const now = Date.now();
    const evidence: EvidenceCapture = {
      id: `ev-img-${now}`,
      timestampMillis: now,
      type: 'image',
      dataUrl,
      sha256Hash: hash,
      fileSizeBytes: Math.round(dataUrl.length * 0.75),
      mimeType: 'image/jpeg',
      name: `evidence_snapshot_${now}.jpg`,
    };

    setLastSnap(evidence);
    onCaptureSaved(evidence);
    setCapturing(false);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/90 flex flex-col items-center justify-center p-4 backdrop-blur-md">
      <div className="w-full max-w-md bg-[#07152C] rounded-[28px] overflow-hidden border border-[#12264A] shadow-2xl flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-white/10 bg-[#050D20]">
          <div className="flex items-center gap-2 text-white">
            <Camera className="w-5 h-5 text-[#55D7FF]" />
            <span className="font-bold text-sm">Camera Evidence Capture</span>
          </div>
          <button
            type="button"
            id="btn-close-camera"
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center text-white hover:bg-white/20 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Viewport / Fallback */}
        <div className="relative aspect-3/4 bg-black flex items-center justify-center overflow-hidden">
          {permissionDenied ? (
            <div className="p-6 text-center text-white space-y-3">
              <ShieldAlert className="w-12 h-12 text-[#FFA7BF] mx-auto opacity-80" />
              <h4 className="font-bold text-base">Camera Access Restricted</h4>
              <p className="text-xs text-[#B8C4D9] leading-relaxed">
                Camera permission was denied or is not supported in this frame. Camera access is
                optional and never blocks or cancels an active SOS emergency.
              </p>
              <button
                type="button"
                onClick={onClose}
                className="mt-2 px-4 py-2 rounded-xl bg-[#5B4BDB] text-white text-xs font-bold"
              >
                Continue Without Camera
              </button>
            </div>
          ) : (
            <>
              <video
                ref={videoRef}
                autoPlay
                playsInline
                muted
                className="w-full h-full object-cover"
              />

              {/* Viewfinder crosshairs */}
              <div className="absolute inset-8 border border-white/30 rounded-2xl pointer-events-none flex items-center justify-center">
                <div className="w-3 h-3 border-t-2 border-l-2 border-[#55D7FF] absolute top-2 left-2" />
                <div className="w-3 h-3 border-t-2 border-r-2 border-[#55D7FF] absolute top-2 right-2" />
                <div className="w-3 h-3 border-b-2 border-l-2 border-[#55D7FF] absolute bottom-2 left-2" />
                <div className="w-3 h-3 border-b-2 border-r-2 border-[#55D7FF] absolute bottom-2 right-2" />
              </div>

              {lastSnap && (
                <div className="absolute bottom-3 left-3 right-3 p-2 rounded-xl bg-black/75 border border-emerald-500/40 text-xs text-white flex items-center gap-2">
                  <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />
                  <div className="truncate">
                    <span className="font-bold text-emerald-300">Saved: </span>
                    <span className="font-mono text-[10px] text-zinc-300">
                      SHA256: {lastSnap.sha256Hash.substring(0, 14)}...
                    </span>
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Controls */}
        {!permissionDenied && (
          <div className="p-4 bg-[#050D20] border-t border-white/10 flex items-center justify-around">
            <button
              type="button"
              id="btn-toggle-lens"
              onClick={toggleLens}
              className="p-3 rounded-full bg-[#12264A] text-white hover:bg-[#1a3461] transition-colors"
              title="Switch Front/Back Lens"
            >
              <RefreshCw className="w-5 h-5" />
            </button>

            <button
              type="button"
              id="btn-snap-photo"
              onClick={capturePhoto}
              disabled={capturing}
              className="w-16 h-16 rounded-full border-4 border-[#55D7FF] bg-white flex items-center justify-center shadow-lg active:scale-90 transition-transform"
              title="Capture Evidence Photo"
            >
              <div className="w-12 h-12 rounded-full bg-[#E51E4D]" />
            </button>

            <button
              type="button"
              id="btn-done-camera"
              onClick={onClose}
              className="px-4 py-2 rounded-xl bg-[#12264A] text-white text-xs font-bold hover:bg-[#1a3461] transition-colors"
            >
              Done
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
