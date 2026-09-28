export function apiErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === 'object' && error !== null && 'error' in error) {
    const body: unknown = error.error;
    if (typeof body === 'object' && body !== null && 'message' in body && typeof body.message === 'string') {
      return body.message;
    }
  }
  return fallback;
}
