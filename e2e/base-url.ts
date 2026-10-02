/** Where the stack under test listens: the nginx of docker compose, or any deployed instance. */
export const BASE_URL = process.env.E2E_BASE_URL ?? 'http://localhost:4200';
