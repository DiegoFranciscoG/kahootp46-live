import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AdminApiService, apiErrorMessage } from '../../core/api/admin-api.service';
import { HostGame, QuizSummary } from '../../core/models/admin.models';

@Component({
  selector: 'app-quiz-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DatePipe],
  template: `
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-2xl font-black">Mis quizzes</h1>
      <a routerLink="/admin/quizzes/new" class="rounded-xl bg-violet-600 px-4 py-2.5 text-sm font-black tracking-wide hover:bg-violet-500">
        + Crear quiz
      </a>
    </div>

    @if (error(); as message) {
      <p class="mt-4 rounded-xl bg-rose-500/10 px-4 py-3 text-sm font-semibold text-rose-200 ring-1 ring-rose-400/30" role="alert">{{ message }}</p>
    }

    <details class="mt-4 rounded-2xl bg-slate-900 ring-1 ring-white/10">
      <summary class="cursor-pointer px-5 py-4 text-sm font-bold text-slate-300">Importar JSON o CSV</summary>
      <div class="space-y-3 border-t border-white/5 px-5 py-4 text-sm">
        <p class="text-slate-400">
          JSON: <code class="text-slate-300">{{ '{' }}"title", "questions": [{{ '{' }}"question", "choices": [{{ '{' }}"text", "correct"{{ '}' }}]{{ '}' }}]{{ '}' }}</code>.
          CSV: <code class="text-slate-300">question,choice_a,choice_b,choice_c,choice_d,correct,time_limit</code> (correct = letra).
        </p>
        <input type="text" placeholder="Título (obligatorio para CSV)" (input)="importTitle.set(value($event))"
               class="w-full rounded-xl bg-slate-950 px-3 py-2 ring-1 ring-white/10 outline-none focus:ring-2 focus:ring-violet-500" />
        <input type="file" accept=".json,.csv,application/json,text/csv" (change)="importFile($event)"
               class="block w-full text-slate-300 file:mr-3 file:rounded-lg file:border-0 file:bg-white/10 file:px-3 file:py-2 file:font-semibold file:text-white" />
      </div>
    </details>

    <ul class="mt-4 grid gap-3 sm:grid-cols-2">
      @for (quiz of quizzes(); track quiz.id) {
        <li class="flex flex-col rounded-2xl bg-slate-900 p-5 ring-1 ring-white/10">
          <h2 class="text-lg font-bold text-white">{{ quiz.title }}</h2>
          @if (quiz.description) {
            <p class="mt-1 line-clamp-2 text-sm text-slate-400">{{ quiz.description }}</p>
          }
          <p class="mt-2 text-xs text-slate-500">{{ quiz.questionCount }} preguntas · {{ quiz.updatedAt | date: 'short' }}</p>
          <div class="mt-4 flex flex-wrap gap-2 text-sm font-bold">
            <button type="button" (click)="startGame(quiz)" [disabled]="busy()"
                    class="rounded-lg bg-emerald-600 px-3 py-2 hover:bg-emerald-500 disabled:opacity-40">Iniciar partida</button>
            <a [routerLink]="['/admin/quizzes', quiz.id]" class="rounded-lg bg-white/10 px-3 py-2 hover:bg-white/15">Editar</a>
            <button type="button" (click)="remove(quiz)" [disabled]="busy()"
                    class="rounded-lg px-3 py-2 text-rose-300 hover:bg-rose-500/10 disabled:opacity-40">Eliminar</button>
          </div>
        </li>
      } @empty {
        <li class="rounded-2xl bg-slate-900 p-6 text-slate-400 ring-1 ring-white/10 sm:col-span-2">Todavía no tienes quizzes.</li>
      }
    </ul>

    @if (games().length) {
      <h2 class="mt-8 text-lg font-black">Partidas recientes</h2>
      <ul class="mt-3 divide-y divide-white/5 rounded-2xl bg-slate-900 ring-1 ring-white/10">
        @for (game of games(); track game.id) {
          <li>
            <a [routerLink]="['/admin/games', game.id]" class="flex items-center justify-between gap-3 px-5 py-3 hover:bg-white/5">
              <span class="min-w-0 truncate font-semibold">{{ game.quizTitle }}</span>
              <span class="flex shrink-0 items-center gap-3 text-sm">
                <span class="font-mono font-bold tracking-widest">{{ game.pin }}</span>
                <span class="text-xs text-slate-400">{{ game.status }}</span>
              </span>
            </a>
          </li>
        }
      </ul>
    }
  `,
})
export class QuizListComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly router = inject(Router);

  protected readonly quizzes = signal<QuizSummary[]>([]);
  protected readonly games = signal<HostGame[]>([]);
  protected readonly importTitle = signal('');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    void this.reload();
  }

  protected value(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  protected async startGame(quiz: QuizSummary): Promise<void> {
    await this.run(async () => {
      const game = await this.api.createGame(quiz.id);
      await this.router.navigate(['/admin/games', game.id]);
    });
  }

  protected async remove(quiz: QuizSummary): Promise<void> {
    if (!confirm(`¿Eliminar "${quiz.title}"? Esta acción no se puede deshacer.`)) {
      return;
    }
    await this.run(async () => {
      await this.api.deleteQuiz(quiz.id);
      await this.reload();
    });
  }

  protected async importFile(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    await this.run(async () => {
      await this.api.importQuiz(file, this.importTitle());
      await this.reload();
    });
    input.value = '';
  }

  private async reload(): Promise<void> {
    await this.run(async () => {
      const [quizzes, games] = await Promise.all([this.api.listQuizzes(), this.api.recentGames()]);
      this.quizzes.set(quizzes);
      this.games.set(games);
    });
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
    } catch (error) {
      this.error.set(apiErrorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }
}
