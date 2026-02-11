import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { filter, map, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

/**
 * RoleGuard factory - Creates a guard that checks for specific roles
 * Waits for auth state to be fully resolved before checking
 * Usage: canActivate: [roleGuard('seller')] or roleGuard('client')
 */
export function roleGuard(requiredRole: 'seller' | 'client'): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const checkAccess = () => {
      // First check if logged in
      if (!authService.isLoggedIn()) {
        router.navigate(['/login']);
        return false;
      }

      // Then check role
      const user = authService.getCurrentUser();
      if (user?.role === requiredRole) {
        return true;
      }

      // Redirect to home if wrong role
      router.navigate(['/']);
      return false;
    };

    // If auth is already ready, check immediately
    if (authService.isAuthReady()) {
      return checkAccess();
    }

    // Wait for auth to be ready before checking
    return authService.authReady$.pipe(
      filter((ready) => ready),
      take(1),
      map(() => checkAccess())
    );
  };
}

/**
 * SellerGuard - Convenience guard for seller-only routes
 */
export const sellerGuard: CanActivateFn = roleGuard('seller');

/**
 * ClientGuard - Convenience guard for client-only routes
 */
export const clientGuard: CanActivateFn = roleGuard('client');
