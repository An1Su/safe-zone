import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, of, timeout } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environments';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/ecommerce.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  private router = inject(Router);

  // Tracks whether initial auth check is complete (for guards to wait on)
  private readonly authReadySubject = new BehaviorSubject<boolean>(false);
  public authReady$ = this.authReadySubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadUserFromStorage();
  }

  /**
   * Returns true when auth state is fully resolved
   */
  isAuthReady(): boolean {
    return this.authReadySubject.value;
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, credentials, {
        withCredentials: true, // Include cookies
      })
      .pipe(
        tap((response) => {
          this.setCurrentUser(response.user);
          localStorage.setItem('token', response.token);
        })
      );
  }

  register(userData: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/register`, userData, {
        withCredentials: true, // Include cookies
      })
      .pipe(
        tap((response) => {
          this.setCurrentUser(response.user);
          localStorage.setItem('token', response.token);
        })
      );
  }

  logout(): Observable<string> {
    // Clear localStorage immediately - logout should always work client-side
    this.clearCurrentUser();

    // Attempt to blacklist token on backend
    const logoutRequest = this.http
      .post<string>(
        `${this.apiUrl}/logout`,
        {},
        {
          withCredentials: true,
          responseType: 'text' as 'json',
        }
      )
      .pipe(
        catchError((error) => {
          // Backend call failed, but we've already cleared client state
          console.warn('Backend logout failed, but local state cleared', error);
          return of('Logged out locally');
        }),
        tap(() => {
          // Navigate to home page to clear any cached state
          this.router.navigate(['/']);
        })
      );

    return logoutRequest;
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  isLoggedIn(): boolean {
    return this.currentUserSubject.value !== null;
  }

  isSeller(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'seller';
  }

  isClient(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'client';
  }

  getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      Authorization: token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json',
    });
  }

  /**
   * Update the current user's data (e.g., after avatar upload)
   */
  updateCurrentUser(updates: Partial<User>): void {
    const currentUser = this.currentUserSubject.value;
    if (currentUser) {
      const updatedUser = { ...currentUser, ...updates };
      this.currentUserSubject.next(updatedUser);
      localStorage.setItem('currentUser', JSON.stringify(updatedUser));
    }
  }

  private setCurrentUser(user: User): void {
    this.currentUserSubject.next(user);
    localStorage.setItem('currentUser', JSON.stringify(user));
  }

  private clearCurrentUser(): void {
    this.currentUserSubject.next(null);
    localStorage.removeItem('currentUser');
    localStorage.removeItem('token');
  }

  private loadUserFromStorage(): void {
    const userStr = localStorage.getItem('currentUser');
    const token = localStorage.getItem('token');

    if (userStr && token) {
      try {
        const user = JSON.parse(userStr);
        // Temporarily set user for immediate UI rendering
        this.currentUserSubject.next(user);
        // Validate token with backend asynchronously
        this.validateToken(user);
      } catch (error) {
        this.clearCurrentUser();
        this.authReadySubject.next(true);
      }
    } else {
      // No stored credentials - auth is ready (user is not logged in)
      this.authReadySubject.next(true);
    }
  }

  /**
   * Validates the stored token with the backend.
   * If token is expired or blacklisted, clears the session.
   * On network errors, keeps the session (optimistic approach).
   */
  private validateToken(user: User): void {
    const token = localStorage.getItem('token');
    if (!token || !user.id) {
      this.authReadySubject.next(true);
      return;
    }

    // Call backend to verify token is still valid
    // Using the user endpoint as a validation check
    // Timeout after 5 seconds to prevent blocking page load
    this.http
      .get<User>(`${environment.apiUrl}/users/${user.id}`, {
        headers: new HttpHeaders({
          Authorization: `Bearer ${token}`,
        }),
        withCredentials: true,
      })
      .pipe(
        timeout(5000), // Don't block page load for more than 5 seconds
        catchError((error) => {
          // Only clear session on 401 (token invalid/expired/blacklisted)
          // Keep session on network errors (offline, timeout, etc.)
          if (error.status === 401 || error.status === 403) {
            console.warn('Token validation failed - clearing session', error.status);
            this.clearCurrentUser();
            alert('Your session has expired. Please log in again.');
            this.router.navigate(['/login']);
          } else {
            // Network error, timeout, or server error - keep the session
            // User can still try to use the app, API calls will fail if token is truly invalid
            console.warn('Token validation request failed (network/timeout/server error), keeping session', error.status || error.message);
          }

          return of(null);
        })
      )
      .subscribe((validatedUser) => {
        if (validatedUser) {
          // Update user data in case it changed on backend
          this.setCurrentUser(validatedUser);
        }
        // Mark auth as ready after validation completes
        this.authReadySubject.next(true);
      });
  }
}
