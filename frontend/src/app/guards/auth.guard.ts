import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { filter, map, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

/**
 * AuthGuard - Protects routes that require authentication
 * Waits for auth state to be fully resolved before checking
 * Redirects to /login if user is not logged in
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // If auth is already ready, check immediately
  if (authService.isAuthReady()) {
    if (authService.isLoggedIn()) {
      return true;
    }
    router.navigate(['/login']);
    return false;
  }

  // Wait for auth to be ready before checking
  return authService.authReady$.pipe(
    filter((ready) => ready),
    take(1),
    map(() => {
      if (authService.isLoggedIn()) {
        return true;
      }
      router.navigate(['/login']);
      return false;
    })
  );
};
