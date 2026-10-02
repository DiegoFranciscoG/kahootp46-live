import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-brand-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="font-black tracking-tight" [class]="large() ? 'text-4xl sm:text-5xl' : 'text-lg'">
      <span class="text-white">KAHOOT</span><span class="text-violet-400">P46</span>
    </span>
  `,
})
export class BrandLogoComponent {
  readonly large = input(false);
}
