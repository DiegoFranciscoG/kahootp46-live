const LETTERS = 'ABCDEF';

/** Choice index (0-based) to the letter shown on screen. */
export function choiceLetter(index: number): string {
  return LETTERS.charAt(index) || '?';
}

/** Accent colour per choice position, consistent across viewer and host screens. */
const ACCENTS = [
  'bg-rose-500',
  'bg-sky-500',
  'bg-amber-500',
  'bg-emerald-500',
  'bg-fuchsia-500',
  'bg-cyan-500',
] as const;

export function choiceAccent(index: number): string {
  return ACCENTS[index % ACCENTS.length];
}
