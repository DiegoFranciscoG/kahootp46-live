import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AdminSessionService } from '../../core/auth/admin-session.service';
import { BrandLogoComponent } from '../../shared/brand-logo.component';

@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BrandLogoComponent, RouterLink],
  template: `
    <main class="flex min-h-dvh items-center justify-center bg-slate-950 px-4 text-slate-100">
      <form class="w-full max-w-sm space-y-4 rounded-2xl bg-slate-900 p-6 ring-1 ring-white/10" (submit)="submit($event)">
        <div class="text-center">
          <app-brand-logo />
          <h1 class="mt-2 text-sm font-semibold uppercase tracking-widest text-slate-400">Panel de administración</h1>
        </div>
        <label class="block text-sm font-semibold text-slate-300">
          Correo
          <input name="email" type="email" autocomplete="username" required
                 class="mt-1 w-full rounded-xl bg-slate-950 px-3 py-2.5 ring-1 ring-white/10 outline-none focus:ring-2 focus:ring-violet-500"
                 (input)="email.set(value($event))" />
        </label>
        <label class="block text-sm font-semibold text-slate-300">
          Contraseña
          <input name="password" type="password" autocomplete="current-password" required
                 class="mt-1 w-full rounded-xl bg-slate-950 px-3 py-2.5 ring-1 ring-white/10 outline-none focus:ring-2 focus:ring-violet-500"
                 (input)="password.set(value($event))" />
        </label>
        @if (error(); as message) {
          <p class="text-sm font-semibold text-rose-300" role="alert">{{ message }}</p>
        }
        <button type="submit" [disabled]="loading() || !email() || !password()"
                class="w-full rounded-xl bg-violet-600 py-3 font-black tracking-widest hover:bg-violet-500 disabled:opacity-40">
          {{ loading() ? 'ENTRANDO…' : 'ENTRAR' }}
        </button>
        <a routerLink="/" class="block text-center text-xs text-slate-500 hover:text-slate-300">Volver al visor</a>
      </form>
    </main>
  `,
})
export class LoginComponent {
  private readonly session = inject(AdminSessionService);
  private readonly router = inject(Router);

  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected value(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  protected async submit(event: Event): Promise<void> {
    event.preventDefault();
    this.loading.set(true);
    this.error.set(null);
    try {
      await this.session.login(this.email(), this.password());
      await this.router.navigate(['/admin']);
    } catch (error) {
      this.error.set(
        error instanceof HttpErrorResponse && error.status === 429
          ? 'Demasiados intentos. Espera un minuto.'
          : 'Correo o contraseña incorrectos.',
      );
    } finally {
      this.loading.set(false);
    }
  }
}
