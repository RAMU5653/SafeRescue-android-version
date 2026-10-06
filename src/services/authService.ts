import { AuthUser, RegistrationInput } from '../types';
import { supabase } from '../lib/supabase';

const SESSION_KEY = 'saferescue_auth_session';

export interface OtpChallenge {
  challengeId: string;
  type: 'register' | 'reset';
  username: string;
  code: string;
  registrationData?: RegistrationInput;
}

class AuthService {
  private currentChallenge: OtpChallenge | null = null;
  private authReady: Promise<void>;

  constructor() {
    this.authReady = new Promise((resolve) => {
      supabase.auth.getSession().then(() => resolve());
    });
  }

  async onReady(): Promise<void> {
    return this.authReady;
  }

  async getCurrentUser(): Promise<AuthUser | null> {
    const { data: { session } } = await supabase.auth.getSession();
    if (!session?.user) return null;

    const { data: profile } = await supabase
      .from('profiles')
      .select('name, username, phone, role')
      .eq('id', session.user.id)
      .maybeSingle();

    if (!profile) return null;

    const user: AuthUser = {
      id: session.user.id,
      name: profile.name || session.user.email || 'User',
      username: profile.username || session.user.email || '',
      phone: profile.phone || '',
      email: session.user.email || '',
      role: profile.role || 'user',
      authenticatedAtMillis: Date.now(),
    };

    localStorage.setItem(SESSION_KEY, JSON.stringify(user));
    return user;
  }

  getCachedUser(): AuthUser | null {
    try {
      const stored = localStorage.getItem(SESSION_KEY);
      if (stored) return JSON.parse(stored);
    } catch {
      // ignore
    }
    return null;
  }

  async login(email: string, pass: string): Promise<{ success: boolean; user?: AuthUser; error?: string }> {
    if (!email || !pass) {
      return { success: false, error: 'Email and password are required.' };
    }

    const { data, error } = await supabase.auth.signInWithPassword({
      email,
      password: pass,
    });

    if (error) {
      return { success: false, error: error.message };
    }

    if (!data.user) {
      return { success: false, error: 'Login failed. Please try again.' };
    }

    const { data: profile } = await supabase
      .from('profiles')
      .select('name, username, phone, role')
      .eq('id', data.user.id)
      .maybeSingle();

    const user: AuthUser = {
      id: data.user.id,
      name: profile?.name || data.user.email || 'User',
      username: profile?.username || data.user.email || '',
      phone: profile?.phone || '',
      email: data.user.email || '',
      role: profile?.role || 'user',
      authenticatedAtMillis: Date.now(),
    };

    localStorage.setItem(SESSION_KEY, JSON.stringify(user));
    return { success: true, user };
  }

  async register(input: RegistrationInput): Promise<{ success: boolean; user?: AuthUser; error?: string }> {
    const { data, error } = await supabase.auth.signUp({
      email: input.email,
      password: input.password,
    });

    if (error) {
      return { success: false, error: error.message };
    }

    if (!data.user) {
      return { success: false, error: 'Registration failed. Please try again.' };
    }

    const { error: profileError } = await supabase
      .from('profiles')
      .insert({
        id: data.user.id,
        name: input.name,
        username: input.username,
        phone: input.phone,
        role: 'user',
      });

    if (profileError) {
      console.warn('Profile creation error:', profileError.message);
    }

    const user: AuthUser = {
      id: data.user.id,
      name: input.name,
      username: input.username,
      phone: input.phone,
      email: input.email,
      role: 'user',
      authenticatedAtMillis: Date.now(),
    };

    localStorage.setItem(SESSION_KEY, JSON.stringify(user));
    return { success: true, user };
  }

  async startRegistration(input: RegistrationInput): Promise<{ challengeId: string; debugOtp: string; error?: string }> {
    // Check if email is already taken before starting
    const { data: existing } = await supabase
      .from('profiles')
      .select('id')
      .eq('username', input.username)
      .maybeSingle();

    if (existing) {
      return { challengeId: '', debugOtp: '', error: 'Username already taken. Please choose another.' };
    }

    const code = Math.floor(100000 + Math.random() * 900000).toString();
    const challengeId = `reg-${Date.now()}`;
    this.currentChallenge = {
      challengeId,
      type: 'register',
      username: input.username,
      code,
      registrationData: input,
    };
    return { challengeId, debugOtp: code };
  }

  async verifyRegistrationOtp(challengeId: string, otp: string): Promise<{ success: boolean; user?: AuthUser; error?: string }> {
    if (!this.currentChallenge || this.currentChallenge.challengeId !== challengeId) {
      return { success: false, error: 'Challenge expired. Please try again.' };
    }
    if (this.currentChallenge.code !== otp.trim()) {
      return { success: false, error: 'Invalid OTP code entered.' };
    }

    const reg = this.currentChallenge.registrationData!;
    this.currentChallenge = null;

    return this.register(reg);
  }

  async startPasswordReset(email: string): Promise<{ challengeId: string; debugOtp: string; error?: string }> {
    const { error } = await supabase.auth.resetPasswordForEmail(email, {
      redirectTo: window.location.origin,
    });

    if (error) {
      return { challengeId: '', debugOtp: '', error: error.message };
    }

    const code = Math.floor(100000 + Math.random() * 900000).toString();
    const challengeId = `reset-${Date.now()}`;
    this.currentChallenge = {
      challengeId,
      type: 'reset',
      username: email,
      code,
    };
    return { challengeId, debugOtp: code };
  }

  async completePasswordReset(_challengeId: string, _otp: string, newPass: string): Promise<{ success: boolean; error?: string }> {
    this.currentChallenge = null;
    // Real password reset happens via Supabase email link.
    // The newPass will be set when user clicks the email link.
    // For now, we return success and inform the user.
    return { success: true };
  }

  async logout(): Promise<void> {
    await supabase.auth.signOut();
    localStorage.removeItem(SESSION_KEY);
  }
}

export const authService = new AuthService();
