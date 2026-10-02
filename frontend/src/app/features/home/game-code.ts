export type GameCode = { kind: 'pin'; pin: string } | { kind: 'error'; message: string };

const QUIZLIVE_PIN = /^\d{6}$/;
const KAHOOT_HOST = /(^|\.)kahoot\.(it|com)$/i;

const NOT_QUIZLIVE =
  'KahootP46 Live solo muestra partidas creadas en QuizLive; no se conecta a partidas de Kahoot!.';

/**
 * Accepts a QuizLive PIN ("994254", spaces allowed) or a QuizLive share link
 * (".../live/994254" or "...?pin=994254"). Third-party links are rejected with an explanation.
 */
export function parseGameCode(raw: string): GameCode {
  const value = raw.trim();
  if (!value) {
    return error('Ingresa el PIN o pega el enlace de la partida.');
  }

  const compact = value.replace(/\s+/g, '');
  if (/^\d+$/.test(compact)) {
    return QUIZLIVE_PIN.test(compact)
      ? { kind: 'pin', pin: compact }
      : error(`Los PIN de QuizLive tienen 6 dígitos (este tiene ${compact.length}). ${NOT_QUIZLIVE}`);
  }

  let url: URL;
  try {
    url = new URL(/^[a-z]+:\/\//i.test(value) ? value : `https://${value}`);
  } catch {
    return error('No es un PIN de 6 dígitos ni un enlace válido.');
  }
  if (KAHOOT_HOST.test(url.hostname)) {
    return error(`Ese enlace es de Kahoot!. ${NOT_QUIZLIVE}`);
  }

  const fromPath = url.pathname.match(/\/live\/(\d{6})\/?$/)?.[1];
  const fromQuery = url.searchParams.get('pin') ?? '';
  const pin = fromPath ?? (QUIZLIVE_PIN.test(fromQuery) ? fromQuery : null);
  return pin ? { kind: 'pin', pin } : error('El enlace no contiene un PIN de QuizLive.');
}

function error(message: string): GameCode {
  return { kind: 'error', message };
}
