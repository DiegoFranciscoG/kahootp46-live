import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';

import { AdminSessionService } from '../../core/auth/admin-session.service';
import { BrandLogoComponent } from '../../shared/brand-logo.component';

@Component({
  selector: 'app-admin-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, BrandLogoComponent],
  template: `
    <div class="min-h-dvh bg-slate-950 text-slate-100">
      <header class="border-b border-white/5 bg-slate-950/85">
        <div class="mx-auto flex max-w-5xl items-center justify-between gap-3 px-4 py-3">
          <a routerLink="/admin" class="flex items-baseline gap-2">
            <app-brand-logo />
            <span class="text-sm font-semibold text-slate-400">Admin</span>
          </a>
          <nav class="flex items-center gap-4 text-sm font-semibold">
            <a routerLink="/" class="text-slate-400 hover:text-white">Visor</a>
            <button type="button" (click)="logout()" class="text-slate-400 hover:text-white">Salir</button>
          </nav>
        </div>
      </header>
      <main class="mx-auto max-w-5xl px-4 py-6">
        <router-outlet />
      </main>
    </div>
  `,
})
export class AdminShellComponent {
  private readonly session = inject(AdminSessionService);
  private readonly router = inject(Router);

  protected logout(): void {
    this.session.logout();
    void this.router.navigate(['/admin/login']);
  }
}
