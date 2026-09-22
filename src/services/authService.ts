import { AuthUser, RegistrationInput } from '../types';

const SESSION_KEY = 'saferescue_auth_session';
const USERS_KEY = 'saferescue_registered_users';

const DEFAULT_ADMIN: AuthUser = {
  id: 'usr-admin-01',
  name: 'Venkata Ram',
  username: 'admin',
  phone: '+1 555-0199',
  email: 'admin@saferescue.local',
  role: 'admin',
  authenticatedAtMillis: Date.now(),
};

export interface OtpChallenge {
  challengeId: string;
  type: 'register' | 'reset';
  username: string;
  code: string;
  registrationData?: RegistrationInput;
}

class AuthService {
  private currentChallenge: OtpChallenge | null = null;

  getCurrentUser(): AuthUser | null {
    try {
      const stored = localStorage.getItem(SESSION_KEY);
      if (stored) return JSON.parse(stored);
    } catch {
      // ignore
    }
    // Default session pre-authenticated for seamless preview experience
    this.saveSession(DEFAULT_ADMIN);
    return DEFAULT_ADMIN;
  }

  saveSession(user: AuthUser | null) {
    if (user) {
      localStorage.setItem(SESSION_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(SESSION_KEY);
    }
  }

  login(username: string, pass: string): { success: boolean; user?: AuthUser; error?: string } {
    if (!username || !pass) {
      return { success: false, error: 'Username and password are required.' };
    }

    if (username.toLowerCase() === 'admin' && pass === 'admin') {
      const user = { ...DEFAULT_ADMIN, authenticatedAtMillis: Date.now() };
      this.saveSession(user);
      return { success: true, user };
    }

    // Check registered users in storage
    const users: Array<AuthUser & { passwordHash: string }> = JSON.parse(localStorage.getItem(USERS_KEY) || '[]');
    const found = users.find((u) => u.username.toLowerCase() === username.toLowerCase());
    if (found && found.passwordHash === pass) {
      const { passwordHash, ...safeUser } = found;
      this.saveSession(safeUser);
      return { success: true, user: safeUser };
    }

    return { success: false, error: 'Invalid credentials. Demo test account is admin / admin.' };
  }

  startRegistration(input: RegistrationInput): { challengeId: string; debugOtp: string } {
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

  verifyRegistrationOtp(challengeId: string, otp: string): { success: boolean; user?: AuthUser; error?: string } {
    if (!this.currentChallenge || this.currentChallenge.challengeId !== challengeId) {
      return { success: false, error: 'Challenge expired. Please try again.' };
    }
    if (this.currentChallenge.code !== otp.trim()) {
      return { success: false, error: 'Invalid OTP code entered.' };
    }

    const reg = this.currentChallenge.registrationData!;
    const newUser: AuthUser = {
      id: `usr-${Date.now()}`,
      name: reg.name,
      username: reg.username,
      phone: reg.phone,
      email: reg.email,
      role: 'user',
      authenticatedAtMillis: Date.now(),
    };

    const users = JSON.parse(localStorage.getItem(USERS_KEY) || '[]');
    users.push({ ...newUser, passwordHash: reg.password });
    localStorage.setItem(USERS_KEY, JSON.stringify(users));

    this.currentChallenge = null;
    this.saveSession(newUser);
    return { success: true, user: newUser };
  }

  startPasswordReset(username: string): { challengeId: string; debugOtp: string } {
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    const challengeId = `reset-${Date.now()}`;
    this.currentChallenge = {
      challengeId,
      type: 'reset',
      username,
      code,
    };
    return { challengeId, debugOtp: code };
  }

  completePasswordReset(challengeId: string, otp: string, newPass: string): { success: boolean; error?: string } {
    if (!this.currentChallenge || this.currentChallenge.challengeId !== challengeId) {
      return { success: false, error: 'Reset session expired.' };
    }
    if (this.currentChallenge.code !== otp.trim()) {
      return { success: false, error: 'Invalid OTP verification code.' };
    }

    // In a real app, update stored user password
    this.currentChallenge = null;
    return { success: true };
  }

  logout() {
    this.saveSession(null);
  }
}

export const authService = new AuthService();
