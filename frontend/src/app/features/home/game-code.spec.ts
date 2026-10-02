import { parseGameCode } from './game-code';

describe('parseGameCode', () => {
  it('accepts a 6-digit QuizLive PIN, with or without spaces', () => {
    expect(parseGameCode('994254')).toEqual({ kind: 'pin', pin: '994254' });
    expect(parseGameCode(' 994 254 ')).toEqual({ kind: 'pin', pin: '994254' });
  });

  it('accepts QuizLive share links', () => {
    expect(parseGameCode('http://localhost:4200/live/994254')).toEqual({ kind: 'pin', pin: '994254' });
    expect(parseGameCode('https://kahootp46.example/live/994254/')).toEqual({ kind: 'pin', pin: '994254' });
    expect(parseGameCode('kahootp46.example/?pin=994254')).toEqual({ kind: 'pin', pin: '994254' });
  });

  it('explains that PINs of another length are not QuizLive games', () => {
    const result = parseGameCode('09399511');

    expect(result.kind).toBe('error');
    expect(result.kind === 'error' && result.message).toContain('6 dígitos (este tiene 8)');
  });

  it('rejects Kahoot! links without contacting them', () => {
    const result = parseGameCode('https://kahoot.it/challenge/12345678?challenge-id=abc');

    expect(result.kind).toBe('error');
    expect(result.kind === 'error' && result.message).toContain('Kahoot!');
    expect(parseGameCode('https://create.kahoot.com/details/x').kind).toBe('error');
  });

  it('rejects empty input, garbage and links without a PIN', () => {
    expect(parseGameCode('   ').kind).toBe('error');
    expect(parseGameCode('hola mundo').kind).toBe('error');
    expect(parseGameCode('https://kahootp46.example/admin').kind).toBe('error');
    expect(parseGameCode('https://kahootp46.example/?pin=12').kind).toBe('error');
  });
});
