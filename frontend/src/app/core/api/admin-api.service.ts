import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { HostAction, HostGame, Quiz, QuizForm, QuizSummary } from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);

  listQuizzes(): Promise<QuizSummary[]> {
    return firstValueFrom(this.http.get<QuizSummary[]>('/api/quizzes'));
  }

  getQuiz(id: string): Promise<Quiz> {
    return firstValueFrom(this.http.get<Quiz>(`/api/quizzes/${id}`));
  }

  createQuiz(quiz: QuizForm): Promise<Quiz> {
    return firstValueFrom(this.http.post<Quiz>('/api/quizzes', quiz));
  }

  updateQuiz(id: string, quiz: QuizForm): Promise<Quiz> {
    return firstValueFrom(this.http.put<Quiz>(`/api/quizzes/${id}`, quiz));
  }

  deleteQuiz(id: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/quizzes/${id}`));
  }

  importQuiz(file: File, title: string): Promise<Quiz> {
    const form = new FormData();
    form.append('file', file);
    if (title.trim()) {
      form.append('title', title.trim());
    }
    return firstValueFrom(this.http.post<Quiz>('/api/quizzes/import', form));
  }

  createGame(quizId: string): Promise<HostGame> {
    return firstValueFrom(this.http.post<HostGame>('/api/host/games', { quizId }));
  }

  recentGames(): Promise<HostGame[]> {
    return firstValueFrom(this.http.get<HostGame[]>('/api/host/games'));
  }

  getGame(id: string): Promise<HostGame> {
    return firstValueFrom(this.http.get<HostGame>(`/api/host/games/${id}`));
  }

  hostAction(id: string, action: HostAction): Promise<HostGame> {
    return firstValueFrom(this.http.post<HostGame>(`/api/host/games/${id}/${action}`, {}));
  }
}

/** Human message from an RFC 9457 problem detail. */
export function apiErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const problem = error.error as { detail?: string; errors?: string[] } | null;
    if (problem?.errors?.length) {
      return problem.errors.join(' · ');
    }
    if (problem?.detail) {
      return problem.detail;
    }
    if (error.status === 0) {
      return 'No se pudo contactar al servidor.';
    }
  }
  return 'Ocurrió un error inesperado.';
}
