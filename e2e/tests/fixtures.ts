import { APIRequestContext, test as base } from '@playwright/test';

import { BASE_URL } from '../base-url';

export interface QuizQuestion {
  text: string;
  timeLimit: number;
  /** Texts of the choices; the one at `correct` (0-based) is the right answer. */
  choices: string[];
  correct: number;
}

export interface Game {
  id: string;
  pin: string;
  quizId: string;
}

export type HostAction = 'start' | 'reveal' | 'next' | 'finish';

/** Capital, ocean and mountain: correct answers B, C and D, so a wrong index shows up immediately. */
export const GEOGRAPHY: QuizQuestion[] = [
  {
    text: '¿Cuál es la capital del Ecuador?',
    timeLimit: 30,
    choices: ['Guayaquil', 'Quito', 'Cuenca', 'Loja'],
    correct: 1,
  },
  {
    text: '¿En qué océano se encuentran las Islas Galápagos?',
    timeLimit: 30,
    choices: ['Atlántico', 'Índico', 'Pacífico', 'Ártico'],
    correct: 2,
  },
  {
    text: '¿Cuál es la montaña más alta del Ecuador?',
    timeLimit: 30,
    choices: ['Cotopaxi', 'Cayambe', 'Tungurahua', 'Chimborazo'],
    correct: 3,
  },
];

/**
 * Admin REST API as the host uses it. Logs in once per worker with ADMIN_EMAIL / ADMIN_PASSWORD
 * (the same variables docker compose reads from .env) and removes everything it created.
 */
export class HostApi {
  private readonly games: string[] = [];
  private readonly quizzes: string[] = [];

  private constructor(
    private readonly api: APIRequestContext,
    readonly token: string,
    readonly expiresAt: string,
  ) {}

  static async login(api: APIRequestContext): Promise<HostApi> {
    const email = process.env.ADMIN_EMAIL;
    const password = process.env.ADMIN_PASSWORD;
    if (!email || !password) {
      throw new Error('Define ADMIN_EMAIL y ADMIN_PASSWORD (las mismas del .env de docker compose).');
    }
    const response = await api.post('/api/auth/login', { data: { email, password } });
    if (!response.ok()) {
      throw new Error(`Login del admin falló: HTTP ${response.status()}`);
    }
    const body = (await response.json()) as { accessToken: string; expiresAt: string };
    return new HostApi(api, body.accessToken, body.expiresAt);
  }

  async createGame(title: string, questions: QuizQuestion[]): Promise<Game> {
    const quiz = await this.post('/api/quizzes', {
      title: `${title} ${Date.now()}`,
      questions: questions.map((q) => ({
        text: q.text,
        timeLimit: q.timeLimit,
        choices: q.choices.map((text, index) => ({ text, correct: index === q.correct })),
      })),
    });
    this.quizzes.push(quiz.id);
    const game = await this.post('/api/host/games', { quizId: quiz.id });
    this.games.push(game.id);
    return { id: game.id, pin: game.pin, quizId: quiz.id };
  }

  /** Game for a quiz that already exists (for example the demo quizzes); the quiz is not removed afterwards. */
  async gameForQuizTitled(title: string): Promise<Game> {
    const response = await this.api.get('/api/quizzes', { headers: this.auth() });
    const quizzes = (await response.json()) as { id: string; title: string }[];
    const quiz = quizzes.find((q) => q.title === title);
    if (!quiz) {
      throw new Error(`No existe el quiz "${title}". Arranca el stack con SPRING_PROFILES_ACTIVE=demo.`);
    }
    const game = await this.post('/api/host/games', { quizId: quiz.id });
    this.games.push(game.id);
    return { id: game.id, pin: game.pin, quizId: quiz.id };
  }

  async act(game: Game, action: HostAction): Promise<void> {
    await this.post(`/api/host/games/${game.id}/${action}`, {});
  }

  /** Removes quizzes created through the UI, which are not tracked by createGame(). */
  async removeQuizzesTitled(prefix: string): Promise<void> {
    const response = await this.api.get('/api/quizzes', { headers: this.auth() });
    const quizzes = (await response.json()) as { id: string; title: string }[];
    for (const quiz of quizzes.filter((q) => q.title.startsWith(prefix))) {
      await this.api.delete(`/api/quizzes/${quiz.id}`, { headers: this.auth() });
    }
  }

  async cleanup(): Promise<void> {
    for (const id of this.games) {
      await this.api.post(`/api/host/games/${id}/finish`, { headers: this.auth() });
    }
    for (const id of this.quizzes) {
      await this.api.delete(`/api/quizzes/${id}`, { headers: this.auth() });
    }
  }

  private async post(path: string, data: unknown): Promise<{ id: string; pin: string }> {
    const response = await this.api.post(path, { data, headers: this.auth() });
    if (!response.ok()) {
      throw new Error(`POST ${path} → HTTP ${response.status()}: ${await response.text()}`);
    }
    return response.json();
  }

  private auth(): Record<string, string> {
    return { Authorization: `Bearer ${this.token}` };
  }
}

export const test = base.extend<object, { host: HostApi }>({
  host: [
    async ({ playwright }, use) => {
      const api = await playwright.request.newContext({ baseURL: BASE_URL });
      const host = await HostApi.login(api);
      await use(host);
      await host.cleanup();
      await api.dispose();
    },
    { scope: 'worker' },
  ],
});

export { expect } from '@playwright/test';
