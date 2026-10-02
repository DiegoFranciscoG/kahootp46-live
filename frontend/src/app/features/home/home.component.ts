import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { ViewerApiService, viewerErrorMessage } from '../../core/api/viewer-api.service';
import { BrandLogoComponent } from '../../shared/brand-logo.component';

@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BrandLogoComponent, RouterLink],
  template: `
    <main class="flex min-h-dvh flex-col items-center justify-center bg-slate-950 px-4 py-10 text-slate-100">
      <div class="w-full max-w-sm text-center">
        <app-brand-logo [large]="true" />
        <p class="mt-3 text-sm font-semibold uppercase tracking-[0.3em] text-slate-400">Live Quiz Answer Viewer</p>

        <form class="mt-10 space-y-4" (submit)="connect($event)" novalidate>
          <label for="pin" class="block text-left text-sm font-semibold text-slate-300">Ingresa el código de partida</label>
          <input
            id="pin"
            name="pin"
            inputmode="numeric"
            autocomplete="off"
            maxlength="6"
            placeholder="123456"
            [value]="pin()"
            (input)="onInput($event)"
            class="w-full rounded-2xl bg-slate-900 px-4 py-4 text-center font-mono text-3xl font-black tracking-[0.4em] text-white placeholder:text-slate-700 ring-1 ring-white/10 outline-none focus:ring-2 focus:ring-violet-500"
            [attr.aria-invalid]="error() !== null"
            aria-describedby="pin-help"
          />
          @if (error(); as message) {
            <p class="text-sm font-semibold text-rose-300" role="alert">{{ message }}</p>
          }
          <button
            type="submit"
            [disabled]="pin().length !== 6 || loading()"
            class="w-full rounded-2xl bg-violet-600 py-4 text-lg font-black tracking-widest text-white shadow-lg shadow-violet-600/30 transition hover:bg-violet-500 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {{ loading() ? 'CONECTANDO…' : 'CONECTAR' }}
          </button>
        </form>

        <p id="pin-help" class="mt-6 flex items-center justify-center gap-2 text-sm text-slate-400">
          <span class="inline-flex size-2 rounded-full bg-emerald-400"></span>
          Conexión en tiempo real
        </p>
      </div>

      <a routerLink="/admin" class="mt-16 text-xs font-semibold text-slate-600 hover:text-slate-400">Panel de administración</a>
    </main>
  `,
})
export class HomeComponent {
  private readonly api = inject(ViewerApiService);
  private readonly router = inject(Router);

  protected readonly pin = signal('');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected onInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 6);
    input.value = digits;
    this.pin.set(digits);
    this.error.set(null);
  }

  protected async connect(event: Event): Promise<void> {
    event.preventDefault();
    if (!/^\d{6}$/.test(this.pin())) {
      this.error.set('El PIN debe tener exactamente 6 dígitos.');
      return;
    }
    this.loading.set(true);
    try {
      const response = await this.api.connect(this.pin());
      await this.router.navigate(['/live', this.pin()], { state: { snapshot: response.snapshot } });
    } catch (error) {
      this.error.set(viewerErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }
}
