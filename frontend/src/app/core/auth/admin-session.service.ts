import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { LoginResponse } from '../models/admin.models';

const KEY = 'kp46:admin';

interface StoredSession {
  token: string;
  expiresAt: string;
}

/**
 * Short-lived admin JWT (1 h). Kept in sessionStorage so it disappears with the tab; never in a cookie
 * or in localStorage.
 */
@Injectable({ providedIn: 'root' })
export class AdminSessionService {
  private readonly http = inject(HttpClient);
  private readonly session = signal<StoredSession | null>(load());

  readonly token = computed(() => {
    const current = this.session();
    return current && Date.parse(current.expiresAt) > Date.now() ? current.token : null;
  });

  async login(email: string, password: string): Promise<void> {
    const response = await firstValueFrom(
      this.http.post<LoginResponse>('/api/auth/login', { email, password }),
    );
    this.store({ token: response.accessToken, expiresAt: response.expiresAt });
  }

  logout(): void {
    this.store(null);
  }

  isAuthenticated(): boolean {
    return this.token() !== null;
  }

  private store(session: StoredSession | null): void {
    this.session.set(session);
    try {
      if (session) {
        sessionStorage.setItem(KEY, JSON.stringify(session));
      } else {
        sessionStorage.removeItem(KEY);
      }
    } catch {
      // Storage blocked: the session lives only in memory.
    }
  }
}

function load(): StoredSession | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as StoredSession) : null;
  } catch {
    return null;
  }
}
