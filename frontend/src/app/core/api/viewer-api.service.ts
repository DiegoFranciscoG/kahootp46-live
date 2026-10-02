import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ConnectResponse } from '../models/live.models';

export interface ViewerSession {
  pin: string;
  token: string;
  expiresAt: string;
}

const SESSION_PREFIX = 'kp46:viewer:';
/** Renew the token a little before it expires. */
const RENEW_MARGIN_MS = 60_000;

@Injectable({ providedIn: 'root' })
export class ViewerApiService {
  private readonly http = inject(HttpClient);

  async connect(pin: string): Promise<ConnectResponse> {
    const response = await firstValueFrom(
      this.http.post<ConnectResponse>('/api/games/connect', { pin }),
    );
    this.saveSession({ pin, token: response.viewerToken, expiresAt: response.expiresAt });
    return response;
  }

  /** Current token for the PIN, renewing it through /connect when missing or about to expire. */
  async token(pin: string): Promise<string> {
    const session = this.session(pin);
    if (session && Date.parse(session.expiresAt) - Date.now() > RENEW_MARGIN_MS) {
      return session.token;
    }
    return (await this.connect(pin)).viewerToken;
  }

  session(pin: string): ViewerSession | null {
    try {
      const raw = sessionStorage.getItem(SESSION_PREFIX + pin);
      return raw ? (JSON.parse(raw) as ViewerSession) : null;
    } catch {
      return null;
    }
  }

  private saveSession(session: ViewerSession): void {
    try {
      sessionStorage.setItem(SESSION_PREFIX + session.pin, JSON.stringify(session));
    } catch {
      // Storage unavailable (private mode): the token simply lives in memory for this page.
    }
  }
}

export function viewerErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    switch (error.status) {
      case 0:
        return 'No se pudo contactar al servidor. Revisa tu conexión.';
      case 400:
        return 'El PIN debe tener exactamente 6 dígitos.';
      case 404:
        return 'No existe una partida con ese PIN.';
      case 429:
        return 'Demasiados intentos. Espera un minuto y vuelve a intentar.';
    }
  }
  return 'No se pudo conectar a la partida.';
}
