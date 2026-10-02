import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { ViewerStatus } from '../features/live/live-game.reducer';

const STYLES: Record<ViewerStatus, { label: string; classes: string; pulse: boolean }> = {
  CONNECTED: { label: 'CONNECTED', classes: 'bg-sky-500/15 text-sky-300 ring-sky-400/40', pulse: false },
  WAITING: { label: 'WAITING', classes: 'bg-slate-500/15 text-slate-300 ring-slate-400/40', pulse: false },
  QUESTION: { label: 'QUESTION', classes: 'bg-rose-500/15 text-rose-300 ring-rose-400/40', pulse: true },
  REVEAL: { label: 'REVEAL', classes: 'bg-emerald-500/15 text-emerald-300 ring-emerald-400/40', pulse: false },
  FINISHED: { label: 'FINISHED', classes: 'bg-violet-500/15 text-violet-300 ring-violet-400/40', pulse: false },
  ERROR: { label: 'ERROR', classes: 'bg-red-500/15 text-red-300 ring-red-400/40', pulse: false },
  RECONNECTING: { label: 'RECONNECTING', classes: 'bg-amber-500/15 text-amber-300 ring-amber-400/40', pulse: true },
};

@Component({
  selector: 'app-status-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-bold tracking-widest ring-1"
      [class]="style().classes"
      role="status"
      [attr.aria-label]="'Estado: ' + style().label"
    >
      <span class="relative flex size-2">
        @if (style().pulse) {
          <span class="absolute inline-flex size-full animate-ping rounded-full bg-current opacity-60"></span>
        }
        <span class="relative inline-flex size-2 rounded-full bg-current"></span>
      </span>
      {{ style().label }}
    </span>
  `,
})
export class StatusBadgeComponent {
  readonly status = input.required<ViewerStatus>();
  protected readonly style = computed(() => STYLES[this.status()]);
}
