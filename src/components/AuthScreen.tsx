import React, { useState } from 'react';
import { Shield, Lock, User, Phone, Mail, ArrowLeft, CheckCircle } from 'lucide-react';
import { AuthScreen, AuthUser, RegistrationInput } from '../types';
import { authService, OtpChallenge } from '../services/authService';

interface AuthScreenProps {
  onAuthSuccess: (user: AuthUser) => void;
}

export const AuthView: React.FC<AuthScreenProps> = ({ onAuthSuccess }) => {
  const [screen, setScreen] = useState<AuthScreen>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  // Register state
  const [regName, setRegName] = useState('');
  const [regUsername, setRegUsername] = useState('');
  const [regPhone, setRegPhone] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regConfirmPassword, setRegConfirmPassword] = useState('');

  // OTP state
  const [activeChallengeId, setActiveChallengeId] = useState<string | null>(null);
  const [otpCode, setOtpCode] = useState('');
  const [debugOtpNotice, setDebugOtpNotice] = useState<string | null>(null);

  // Reset state
  const [resetUsername, setResetUsername] = useState('');
  const [newPassword, setNewPassword] = useState('');

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    const res = authService.login(username, password);
    if (res.success && res.user) {
      onAuthSuccess(res.user);
    } else {
      setError(res.error || 'Authentication failed');
    }
  };

  const handleRegister = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (regPassword !== regConfirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    const input: RegistrationInput = {
      name: regName,
      username: regUsername,
      phone: regPhone,
      email: regEmail,
      password: regPassword,
    };
    const { challengeId, debugOtp } = authService.startRegistration(input);
    setActiveChallengeId(challengeId);
    setDebugOtpNotice(debugOtp);
    setOtpCode(debugOtp); // auto-fill for frictionless preview
    setScreen('register_otp');
  };

  const handleVerifyRegisterOtp = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeChallengeId) return;
    setError(null);
    const res = authService.verifyRegistrationOtp(activeChallengeId, otpCode);
    if (res.success && res.user) {
      onAuthSuccess(res.user);
    } else {
      setError(res.error || 'OTP verification failed');
    }
  };

  const handleStartReset = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!resetUsername) return;
    const { challengeId, debugOtp } = authService.startPasswordReset(resetUsername);
    setActiveChallengeId(challengeId);
    setDebugOtpNotice(debugOtp);
    setOtpCode(debugOtp);
    setScreen('reset_otp');
  };

  const handleCompleteReset = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeChallengeId) return;
    setError(null);
    const res = authService.completePasswordReset(activeChallengeId, otpCode, newPassword);
    if (res.success) {
      // Auto login as demo
      const loginRes = authService.login(resetUsername, newPassword);
      if (loginRes.success && loginRes.user) {
        onAuthSuccess(loginRes.user);
      } else {
        setScreen('login');
        setError('Password reset successfully. Please sign in.');
      }
    } else {
      setError(res.error || 'Reset failed');
    }
  };

  const fillDemoLogin = () => {
    setUsername('admin');
    setPassword('admin');
  };

  return (
    <div className="min-h-screen bg-[#050D20] flex flex-col justify-center items-center p-4 relative overflow-hidden">
      {/* Background ambient gradient */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-96 h-96 bg-[#5B4BDB]/15 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-sm z-10 space-y-6">
        {/* Branding */}
        <div className="flex flex-col items-center text-center">
          <img
            src="/saferescue_logo.png"
            alt="SafeRescue Logo"
            className="h-20 w-auto object-contain drop-shadow-xl mb-3"
            onError={(e) => {
              (e.currentTarget as HTMLElement).style.display = 'none';
            }}
          />
          <h1 className="text-2xl font-black text-white tracking-tight">SafeRescue</h1>
          <p className="text-xs text-[#8E9BB6] mt-1">
            Local-First Emergency SOS & Offline Safety Pipeline
          </p>
        </div>

        {/* Form Container */}
        <div className="bg-[#07152C] rounded-[24px] p-6 border border-[#12264A] shadow-2xl">
          {error && (
            <div className="mb-4 p-3 rounded-xl bg-rose-950/60 border border-rose-800 text-rose-200 text-xs leading-relaxed">
              {error}
            </div>
          )}

          {/* SCREEN: LOGIN */}
          {screen === 'login' && (
            <form onSubmit={handleLogin} className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-[#8E9BB6] block mb-1">
                  Username or ID
                </label>
                <div className="relative">
                  <User className="w-4 h-4 text-zinc-500 absolute left-3 top-3" />
                  <input
                    type="text"
                    required
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="e.g. admin"
                    className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-[#0D2140] border border-white/10 text-white text-sm focus:outline-none focus:border-[#72D8FF]"
                  />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs font-semibold text-[#8E9BB6]">Password</label>
                  <button
                    type="button"
                    onClick={() => {
                      setError(null);
                      setScreen('reset');
                    }}
                    className="text-[11px] text-[#72D8FF] hover:underline"
                  >
                    Forgot?
                  </button>
                </div>
                <div className="relative">
                  <Lock className="w-4 h-4 text-zinc-500 absolute left-3 top-3" />
                  <input
                    type="password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-[#0D2140] border border-white/10 text-white text-sm focus:outline-none focus:border-[#72D8FF]"
                  />
                </div>
              </div>

              <button
                type="submit"
                id="btn-submit-login"
                className="w-full py-3 rounded-xl bg-[#5B4BDB] hover:bg-[#4838c4] text-white font-bold text-sm tracking-wide transition-colors shadow-lg shadow-[#5B4BDB]/25"
              >
                Sign In
              </button>

              <div className="pt-2 flex items-center justify-between text-xs">
                <button
                  type="button"
                  onClick={fillDemoLogin}
                  className="text-[11px] text-[#FFA7BF] hover:underline font-semibold"
                >
                  Fill Demo: admin / admin
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setError(null);
                    setScreen('register');
                  }}
                  className="text-[11px] text-[#72D8FF] hover:underline font-semibold"
                >
                  Create Account
                </button>
              </div>
            </form>
          )}

          {/* SCREEN: REGISTER */}
          {screen === 'register' && (
            <form onSubmit={handleRegister} className="space-y-3">
              <div className="flex items-center gap-2 mb-2">
                <button
                  type="button"
                  onClick={() => setScreen('login')}
                  className="p-1 text-zinc-400 hover:text-white"
                >
                  <ArrowLeft className="w-4 h-4" />
                </button>
                <h3 className="font-bold text-sm text-white">Create SafeRescue Identity</h3>
              </div>

              <div>
                <label className="text-[11px] text-zinc-400 block mb-0.5">Full Name</label>
                <input
                  type="text"
                  required
                  value={regName}
                  onChange={(e) => setRegName(e.target.value)}
                  placeholder="Venkata Ram"
                  className="w-full px-3 py-2 rounded-lg bg-[#0D2140] border border-white/10 text-white text-xs"
                />
              </div>

              <div>
                <label className="text-[11px] text-zinc-400 block mb-0.5">Username</label>
                <input
                  type="text"
                  required
                  value={regUsername}
                  onChange={(e) => setRegUsername(e.target.value)}
                  placeholder="ramu5653"
                  className="w-full px-3 py-2 rounded-lg bg-[#0D2140] border border-white/10 text-white text-xs"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-[11px] text-zinc-400 block mb-0.5">Phone</label>
                  <input
                    type="tel"
                    required
                    value={regPhone}
                    onChange={(e) => setRegPhone(e.target.value)}
                    placeholder="+1 555-0199"
                    className="w-full px-3 py-2 rounded-lg bg-[#0D2140] border border-white/10 text-white text-xs"
                  />
                </div>
                <div>
                  <label className="text-[11px] text-zinc-400 block mb-0.5">Email</label>
                  <input
                    type="email"
                    required
                    value={regEmail}
                    onChange={(e) => setRegEmail(e.target.value)}
                    placeholder="user@safe.io"
                    className="w-full px-3 py-2 rounded-lg bg-[#0D2140] border border-white/10 text-white text-xs"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-[11px] text-zinc-400 block mb-0.5">Password</label>
                  <input
                    type="password"
                    required
                    value={regPassword}
                    onChange={(e) => setRegPassword(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg bg-[#0D2140] border border-white/10 text-white text-xs"
                  />
                </div>
                <div>
                  <label className="text-[11px] text-zinc-400 block mb-0.5">Confirm</label>
                  <input
                    type="password"
                    required
                    value={regConfirmPassword}
                    onChange={(e) => setRegConfirmPassword(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg bg-[#0D2140] border border-white/10 text-white text-xs"
                  />
                </div>
              </div>

              <button
                type="submit"
                className="w-full py-2.5 mt-2 rounded-xl bg-[#5B4BDB] hover:bg-[#4838c4] text-white font-bold text-xs"
              >
                Continue to OTP Verification
              </button>
            </form>
          )}

          {/* SCREEN: REGISTER OTP */}
          {screen === 'register_otp' && (
            <form onSubmit={handleVerifyRegisterOtp} className="space-y-4">
              <div className="flex items-center gap-2 mb-2">
                <button
                  type="button"
                  onClick={() => setScreen('register')}
                  className="p-1 text-zinc-400 hover:text-white"
                >
                  <ArrowLeft className="w-4 h-4" />
                </button>
                <h3 className="font-bold text-sm text-white">Enter Registration OTP</h3>
              </div>

              {debugOtpNotice && (
                <div className="p-2.5 rounded-xl bg-indigo-950/60 border border-indigo-700 text-[11px] text-indigo-200">
                  <span className="font-bold">Test OTP Code:</span> {debugOtpNotice}
                </div>
              )}

              <div>
                <label className="text-xs text-zinc-400 block mb-1">6-Digit Verification Code</label>
                <input
                  type="text"
                  maxLength={6}
                  required
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value)}
                  placeholder="123456"
                  className="w-full py-2 text-center font-mono text-xl tracking-widest rounded-xl bg-[#0D2140] border border-white/10 text-white"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 rounded-xl bg-[#5B4BDB] text-white font-bold text-xs"
              >
                Verify & Activate Account
              </button>
            </form>
          )}

          {/* SCREEN: RESET PASSWORD */}
          {screen === 'reset' && (
            <form onSubmit={handleStartReset} className="space-y-4">
              <div className="flex items-center gap-2 mb-2">
                <button
                  type="button"
                  onClick={() => setScreen('login')}
                  className="p-1 text-zinc-400 hover:text-white"
                >
                  <ArrowLeft className="w-4 h-4" />
                </button>
                <h3 className="font-bold text-sm text-white">Reset Account Password</h3>
              </div>

              <div>
                <label className="text-xs text-zinc-400 block mb-1">Username or Phone</label>
                <input
                  type="text"
                  required
                  value={resetUsername}
                  onChange={(e) => setResetUsername(e.target.value)}
                  placeholder="admin"
                  className="w-full px-3 py-2 rounded-xl bg-[#0D2140] border border-white/10 text-white text-sm"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 rounded-xl bg-[#5B4BDB] text-white font-bold text-xs"
              >
                Send Reset Code
              </button>
            </form>
          )}

          {/* SCREEN: RESET OTP & NEW PASS */}
          {screen === 'reset_otp' && (
            <form onSubmit={handleCompleteReset} className="space-y-3">
              <div className="flex items-center gap-2 mb-2">
                <button
                  type="button"
                  onClick={() => setScreen('reset')}
                  className="p-1 text-zinc-400 hover:text-white"
                >
                  <ArrowLeft className="w-4 h-4" />
                </button>
                <h3 className="font-bold text-sm text-white">Enter Reset Code & New Password</h3>
              </div>

              {debugOtpNotice && (
                <div className="p-2.5 rounded-xl bg-indigo-950/60 border border-indigo-700 text-[11px] text-indigo-200">
                  <span className="font-bold">Test OTP Code:</span> {debugOtpNotice}
                </div>
              )}

              <div>
                <label className="text-xs text-zinc-400 block mb-0.5">6-Digit Code</label>
                <input
                  type="text"
                  maxLength={6}
                  required
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value)}
                  className="w-full py-1.5 text-center font-mono text-lg rounded-xl bg-[#0D2140] border border-white/10 text-white"
                />
              </div>

              <div>
                <label className="text-xs text-zinc-400 block mb-0.5">New Password</label>
                <input
                  type="password"
                  required
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-[#0D2140] border border-white/10 text-white text-sm"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 rounded-xl bg-[#5B4BDB] text-white font-bold text-xs"
              >
                Save New Password & Sign In
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};
